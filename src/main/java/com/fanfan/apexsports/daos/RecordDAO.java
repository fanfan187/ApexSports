package com.fanfan.apexsports.daos;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fanfan.apexsports.cache.RecordCache;
import com.fanfan.apexsports.entities.Record;
import com.fanfan.apexsports.mappers.RecordMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 校记录数据调度层
 * <p>
 * 协调 {@link RecordMapper} 与 {@link RecordCache}，负责校记录（recordType=1）的
 * 查询、写入与作废，仅供成绩模块内部调用（对外 Controller/Logic 在模块11 实现）。
 * 作为普通 Spring Bean，不继承 ServiceImpl，内部自动填充审计字段并做写后驱逐缓存。
 * </p>
 * <p>
 * <b>关键技术点：</b>
 * <ul>
 *   <li>{@link #getBestRecord(Long, Integer, Integer)} 采用 cache-aside：先查
 *       {@link RecordCache#get(Long, Integer)}，未命中则查库取最优后回写缓存。
 *       「最优」方向由调用方（成绩模块 Logic）通过 {@code direction} 参数传入，
 *       径赛取 score 升序首条（越小越优），田赛取 score 降序首条（越大越优），
 *       避免本 DAO 依赖 {@code EventDAO} 反向耦合 Event.category。</li>
 *   <li>写操作（{@link #insertRecord} / {@link #invalidateRecord}）统一驱逐
 *       该 eventId + recordType 维度的缓存，保证下次查询回源取最新最优。</li>
 * </ul>
 * </p>
 * <p>
 * <b>红线遵守：</b>DAO 层不直接操作缓存（通过 {@link RecordCache}）、不做业务校验
 * （如成绩合法性校验在 Logic）、不做 DO→VO 转换、不出现底层存储相关字样。
 * </p>
 *
 * @author fanfan187
 * @version v1.0.0
 * @since v1.0.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RecordDAO {

    // ==================== 方向常量（由调用方传入，避免反向依赖 EventDAO） ====================

    /** 径赛方向：score 升序，越小越优（如短跑用时） */
    public static final int DIRECTION_TRACK = 1;

    /** 田赛方向：score 降序，越大越优（如跳远距离） */
    public static final int DIRECTION_FIELD = 2;

    /** 默认状态：有效 */
    private static final int STATUS_ACTIVE = 1;

    /** 作废状态 */
    private static final int STATUS_INVALID = 0;

    // ==================== 依赖注入 ====================

    private final RecordMapper recordMapper;
    private final RecordCache recordCache;

    // ==================== 查询 ====================

    /**
     * 查询指定 eventId + recordType 维度的最优记录（cache-aside）。
     * <p>
     * 先查缓存，未命中则查库：按 status=1 有效、recordType 过滤，按 direction 决定
     * score 排序方向取首条最优记录，回写缓存后返回。
     * </p>
     *
     * @param eventId    项目ID
     * @param recordType 记录类型（1-校记录，2-市记录，3-省记录）
     * @param direction  排序方向：{@link #DIRECTION_TRACK}（径赛，升序）或 {@link #DIRECTION_FIELD}（田赛，降序），
     *                   为 null 时默认按径赛升序
     * @return 最优记录实体，无有效记录返回 {@code null}
     */
    public Record getBestRecord(Long eventId, Integer recordType, Integer direction) {
        // 1. 先查缓存
        Record cached = recordCache.get(eventId, recordType);
        if (cached != null) {
            return cached;
        }
        // 2. 缓存未命中，查库取最优
        LambdaQueryWrapper<Record> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Record::getEventId, eventId)
                .eq(Record::getRecordType, recordType)
                .eq(Record::getStatus, STATUS_ACTIVE);
        if (direction != null && direction == DIRECTION_FIELD) {
            wrapper.orderByDesc(Record::getScore);
        } else {
            wrapper.orderByAsc(Record::getScore);
        }
        wrapper.last("LIMIT 1");
        List<Record> records = recordMapper.selectList(wrapper);
        if (records.isEmpty()) {
            // 无有效记录不写缓存，避免缓存穿透占位（由 Logic 层判空处理）
            return null;
        }
        Record best = records.get(0);
        // 3. 回写缓存
        recordCache.set(eventId, recordType, best);
        return best;
    }

    // ==================== 列表查询（支持可选筛选） ====================

    /**
     * 查询记录列表（eventId / recordType / status 三个可选筛选）。
     * <p>
     * 三个参数均为可选（null 表示不过滤该维度），结果按 {@code recordDate DESC} 排序。
     * 不做业务校验、不转 VO，直接返回 DO 列表，由 Logic 层负责 DO→VO 转换。
     * </p>
     *
     * @param eventId    项目ID筛选（可选）
     * @param recordType 记录类型筛选（可选）：1-校记录 / 2-市记录 / 3-省记录
     * @param status     状态筛选（可选）：0-已作废 / 1-有效
     * @return 记录实体列表（按创造记录日期倒序）
     */
    public List<Record> findAll(Long eventId, Integer recordType, Integer status) {
        LambdaQueryWrapper<Record> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(eventId != null, Record::getEventId, eventId)
                .eq(recordType != null, Record::getRecordType, recordType)
                .eq(status != null, Record::getStatus, status)
                .orderByDesc(Record::getRecordDate);
        return recordMapper.selectList(wrapper);
    }

    /**
     * 根据 ID 查询记录。
     *
     * @param id 记录ID
     * @return 记录实体，不存在返回 {@code null}（由 Logic 层判空并抛 RECORD_NOT_FOUND）
     */
    public Record getById(Long id) {
        return recordMapper.selectById(id);
    }

    // ==================== 写入 ====================

    /**
     * 插入记录（自动填充状态、审计字段、逻辑删除标记）。
     * <p>
     * 主键 id 由库自增（{@code @TableId(IdType.AUTO)}），不手动 set；
     * status 为 null 时默认置为有效（1）；写入后驱逐该维度缓存并重新查库返回完整字段。
     * </p>
     *
     * @param record 记录实体（不含主键和审计字段）
     * @return 插入后的记录实体（含自动生成的主键，重新查库保证字段完整）
     */
    public Record insertRecord(Record record) {
        if (record.getStatus() == null) {
            record.setStatus(STATUS_ACTIVE);
        }
        record.setDeleted(0);
        record.setCreatedAt(LocalDateTime.now());
        record.setUpdatedAt(LocalDateTime.now());
        recordMapper.insert(record);
        Record inserted = recordMapper.selectById(record.getId());
        // 写操作：驱逐该 eventId + recordType 维度的最优缓存
        recordCache.evict(record.getEventId(), record.getRecordType());
        return inserted;
    }

    /**
     * 作废记录（置 status=0，逻辑作废而非物理删除）。
     * <p>
     * 先查出该记录以拿到 eventId + recordType，再置 status=0、更新时间，
     * 写后驱逐对应维度缓存。
     * </p>
     *
     * @param recordId 记录ID
     */
    public void invalidateRecord(Long recordId) {
        Record existing = recordMapper.selectById(recordId);
        if (existing == null) {
            log.warn("invalidateRecord: record {} not found, skip", recordId);
            return;
        }
        Record patch = new Record();
        patch.setId(recordId);
        patch.setStatus(STATUS_INVALID);
        patch.setUpdatedAt(LocalDateTime.now());
        recordMapper.updateById(patch);
        // 写操作：驱逐该 eventId + recordType 维度的最优缓存
        recordCache.evict(existing.getEventId(), existing.getRecordType());
    }

    /**
     * 按非空字段部分更新记录，并驱逐相关维度缓存（G2 缓存一致性修复）。
     * <p>
     * 三步执行：
     * <ol>
     *   <li>查出 existing 记录；若不存在则静默返回（Logic 层负责抛 RECORD_NOT_FOUND）。</li>
     *   <li>覆盖 id 与 updatedAt 后调用 {@code updateById}（MBP 默认仅 SET 非空字段，
     *       正好实现部分更新语义）。</li>
     *   <li>驱逐缓存：old 维度（existing.eventId + existing.recordType）必驱；
     *       若 patch.recordType 非空且与 existing 不同，<b>额外</b>驱逐 new 维度
     *       （existing.eventId + patch.recordType），因为 recordType 变更意味着
     *       旧类型最优缓存已失效，新类型的槽位也可能被这次更新影响。</li>
     * </ol>
     * </p>
     *
     * @param id    待更新记录ID
     * @param patch 携带部分更新字段的实体（非空字段才会被写入）
     */
    public void updateSelective(Long id, Record patch) {
        // Step 1: 查出现有记录，缺失则静默返回（Logic 层负责抛 RECORD_NOT_FOUND）
        Record existing = recordMapper.selectById(id);
        if (existing == null) {
            return;
        }
        // Step 2: 部分更新（MBP updateById 仅 SET 非空字段）
        patch.setId(id);
        patch.setUpdatedAt(LocalDateTime.now());
        recordMapper.updateById(patch);
        // Step 3: 驱逐缓存 —— old 维度必驱
        recordCache.evict(existing.getEventId(), existing.getRecordType());
        // 若 recordType 变更，新维度的最优缓存也可能受影响，一并驱逐
        if (patch.getRecordType() != null
                && !patch.getRecordType().equals(existing.getRecordType())) {
            recordCache.evict(existing.getEventId(), patch.getRecordType());
        }
    }
}
