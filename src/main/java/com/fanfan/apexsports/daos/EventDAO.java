package com.fanfan.apexsports.daos;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fanfan.apexsports.constants.EventConstants;
import com.fanfan.apexsports.entities.Event;
import com.fanfan.apexsports.entities.Record;
import com.fanfan.apexsports.mappers.EventMapper;
import com.fanfan.apexsports.mappers.RecordMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 比赛项目数据调度层
 * <p>
 * 协调 {@link EventMapper} 与 {@link RecordMapper}，负责比赛项目数据的存储、查询、调度。
 * 作为普通 Spring Bean，不继承 ServiceImpl，内部自动填充状态、创建时间、更新时间等字段。
 * </p>
 * <p>
 * <b>关键技术点：</b><br>
 * 由于 {@code Event.code} 的唯一性校验需要覆盖所有记录（包括已被逻辑删除的记录），
 * 而 MyBatis-Plus 的 {@code @TableLogic} 会自动在 LambdaQueryWrapper / selectCount
 * 生成的 SQL 中追加 {@code AND deleted = 0} 条件，会导致已软删的记录被漏掉，
 * 因此 {@link #countByCode(String)} 与 {@link #countByCodeExcludeId(String, Long)}
 * 必须改用 {@link JdbcTemplate} 执行原生 SQL（不带 deleted 条件）。
 * </p>
 * <p>
 * 三张引用表（as_participation / as_team_participation / as_competition）当前尚无对应 Entity/Mapper，
 * 引用完整性检查同样使用 JdbcTemplate 原生 SQL（带 deleted = 0 过滤）。
 * </p>
 *
 * @author fanfan187
 * @version v1.0.0
 * @since v1.0.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EventDAO {

    private final EventMapper eventMapper;
    private final RecordMapper recordMapper;
    private final JdbcTemplate jdbcTemplate;

    // ==================== 查询操作 ====================

    /**
     * 查询所有未删除的比赛项目，支持按类型/类别/性别/状态可选过滤（按 ID 升序）。
     * <p>
     * MyBatis-Plus 的 {@code @TableLogic} 会自动追加 {@code deleted = 0} 条件，
     * 因此软删记录不会被返回。
     * </p>
     *
     * @param type     项目类型（1-个人，2-集体），可为 null 表示不限制
     * @param category 项目类别（1-径赛，2-田赛），可为 null 表示不限制
     * @param gender   性别限制（0-女子，1-男子，2-混合），可为 null 表示不限制
     * @param status   状态（0-停用，1-启用），可为 null 表示不限制
     * @return 符合条件的比赛项目列表
     */
    public List<Event> findAll(Integer type, Integer category, Integer gender, Integer status) {
        LambdaQueryWrapper<Event> wrapper = new LambdaQueryWrapper<>();
        if (type != null) {
            wrapper.eq(Event::getType, type);
        }
        if (category != null) {
            wrapper.eq(Event::getCategory, category);
        }
        if (gender != null) {
            wrapper.eq(Event::getGender, gender);
        }
        if (status != null) {
            wrapper.eq(Event::getStatus, status);
        }
        wrapper.orderByAsc(Event::getId);
        return eventMapper.selectList(wrapper);
    }

    /**
     * 根据 ID 查询比赛项目
     *
     * @param id 项目ID
     * @return 项目实体，不存在返回 null
     */
    public Event getById(Long id) {
        return eventMapper.selectById(id);
    }

    /**
     * 查询指定项目下的有效历史记录（按创造日期倒序）。
     * <p>
     * 仅返回 status=1（有效）且未逻辑删除的记录（{@code @TableLogic} 自动追加 deleted=0）。
     * </p>
     *
     * @param eventId    项目ID
     * @param recordType 记录类型（1-校记录，2-市记录，3-省记录），可为 null 表示不限制
     * @return 有效记录列表（按记录日期倒序排列）
     */
    public List<Record> getRecordsByEventId(Long eventId, Integer recordType) {
        LambdaQueryWrapper<Record> wrapper = new LambdaQueryWrapper<Record>()
                .eq(Record::getEventId, eventId)
                .eq(Record::getStatus, 1);
        if (recordType != null) {
            wrapper.eq(Record::getRecordType, recordType);
        }
        wrapper.orderByDesc(Record::getRecordDate);
        return recordMapper.selectList(wrapper);
    }

    // ==================== 代码唯一性校验 ====================
    // ⚠️ 必须使用 JdbcTemplate 原生 SQL，且不能带 deleted 条件
    //    —— 因为 API 文档要求 code 唯一性覆盖所有记录（含已逻辑删除的记录）
    //    若用 LambdaQueryWrapper，MyBatis-Plus @TableLogic 会自动追加 deleted = 0，
    //    导致已被软删的同 code 记录被漏算，从而无法阻止重复 code 创建。

    /**
     * 统计指定项目代码的记录数（用于新增时唯一性校验）。
     * <p>
     * <b>注意：</b>此查询<b>不</b>带 {@code deleted = 0} 过滤条件，会统计所有记录
     * （包括已逻辑删除的记录）。原因是 API 文档要求 code 唯一性必须覆盖软删记录，
     * 否则删除某项目后允许重建同 code 会破坏业务语义。
     * </p>
     *
     * @param code 项目代码
     * @return 所有（含软删）记录数
     */
    public Long countByCode(String code) {
        return jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM as_event WHERE code = ?",
                Long.class,
                code
        );
    }

    /**
     * 统计指定项目代码的记录数，排除指定 ID（用于更新时唯一性校验）。
     * <p>
     * <b>注意：</b>此查询<b>不</b>带 {@code deleted = 0} 过滤条件，理由同 {@link #countByCode(String)}。
     * </p>
     *
     * @param code       项目代码
     * @param excludeId 需排除的项目ID（通常为正在更新的记录自身 ID）
     * @return 所有（含软删、排除指定 ID）记录数
     */
    public Long countByCodeExcludeId(String code, Long excludeId) {
        return jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM as_event WHERE code = ? AND id != ?",
                Long.class,
                code, excludeId
        );
    }

    // ==================== 引用完整性检查 ====================
    // 三张引用表当前无 Entity/Mapper，使用 JdbcTemplate 原生 SQL
    // 注意：与 as_event 唯一性校验不同，这里需要带 deleted = 0，
    //       因为我们只关心"未删除的引用"是否阻止项目删除，已删除的引用不算数。

    /**
     * 检查指定项目下是否存在未删除的个人参赛记录（as_participation）。
     *
     * @param eventId 项目ID
     * @return 关联的未删除个人参赛记录数量
     */
    public Long countParticipationsByEventId(Long eventId) {
        return jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM as_participation WHERE event_id = ? AND deleted = 0",
                Long.class,
                eventId
        );
    }

    /**
     * 检查指定项目下是否存在未删除的集体参赛记录（as_team_participation）。
     *
     * @param eventId 项目ID
     * @return 关联的未删除集体参赛记录数量
     */
    public Long countTeamParticipationsByEventId(Long eventId) {
        return jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM as_team_participation WHERE event_id = ? AND deleted = 0",
                Long.class,
                eventId
        );
    }

    /**
     * 检查指定项目是否被未删除的比赛（as_competition）引用。
     *
     * @param eventId 项目ID
     * @return 关联的未删除比赛数量
     */
    public Long countCompetitionsByEventId(Long eventId) {
        return jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM as_competition WHERE event_id = ? AND deleted = 0",
                Long.class,
                eventId
        );
    }

    // ==================== 写入操作 ====================

    /**
     * 插入比赛项目（自动填充状态、逻辑删除标记、创建时间、更新时间、默认个人报名次数）。
     * <p>
     * <b>注意：</b>本方法不处理 maxParticipants 业务规则（个人项目强制为 null），
     * 该业务规则由 Logic 层负责处理。
     * </p>
     *
     * @param event 比赛项目实体（不含主键和审计字段）
     * @return 插入后的比赛项目实体（含自动生成的主键）
     */
    public Event insert(Event event) {
        event.setStatus(EventConstants.STATUS_ENABLED);
        event.setDeleted(0);
        event.setCreatedAt(LocalDateTime.now());
        event.setUpdatedAt(LocalDateTime.now());
        if (event.getMaxIndividualEvents() == null) {
            event.setMaxIndividualEvents(EventConstants.DEFAULT_MAX_INDIVIDUAL_EVENTS);
        }
        eventMapper.insert(event);
        // 从数据库重新提取，确保字段完整
        return eventMapper.selectById(event.getId());
    }

    /**
     * 更新比赛项目（自动更新修改时间）
     *
     * @param event 比赛项目实体
     * @return 更新后的比赛项目实体
     */
    public Event update(Event event) {
        event.setUpdatedAt(LocalDateTime.now());
        eventMapper.updateById(event);
        return eventMapper.selectById(event.getId());
    }

    /**
     * 逻辑删除比赛项目（MyBatis-Plus 自动处理 @TableLogic）
     *
     * @param id 项目ID
     */
    public void deleteById(Long id) {
        eventMapper.deleteById(id);
    }
}
