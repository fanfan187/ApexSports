package com.fanfan.apexsports.daos;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fanfan.apexsports.entities.Venue;
import com.fanfan.apexsports.mappers.VenueMapper;
import com.fanfan.apexsports.vos.VenueScheduleVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 场地数据调度层
 * <p>
 * 协调 {@link VenueMapper}，负责场地数据的存储、查询和调度。
 * 作为普通 Spring Bean（{@code @Component}），不继承 ServiceImpl，
 * 内部自动填充 createdAt / updatedAt，并统一通过 LambdaQueryWrapper 构建条件。
 * </p>
 * <p>
 * 除常规 CRUD 外，额外提供：
 * <ul>
 *   <li>{@link #countByCode} / {@link #countByCodeExcludeId}：用于 code 唯一性校验</li>
 *   <li>{@link #countCompetitionsByVenueId}：删除前占用检查（复用赛程查询）</li>
 *   <li>{@link #findScheduleByVenueId}：跨表赛程查询（委派给 Mapper 的 join SQL）</li>
 * </ul>
 * </p>
 *
 * @author fanfan187
 * @version v1.0.0
 * @since v1.0.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class VenueDAO {

    private final VenueMapper venueMapper;

    // ==================== 查询操作 ====================

    /**
     * 全量查询场地列表（不分页），支持 type / status 可选筛选。
     * <p>
     * 两个筛选参数均为可选（null 表示不过滤），结果按 {@code createdAt DESC} 排序。
     * 不做业务校验，不转 VO，直接返回 DO 列表，由 Logic 层负责 DO→VO 转换。
     * </p>
     *
     * @param type   场地类型筛选（可选）：1-径赛 / 2-田赛 / 3-综合
     * @param status 状态筛选（可选）：0-不可用 / 1-可用
     * @return 场地实体列表（按创建时间倒序）
     */
    public List<Venue> findAll(Integer type, Integer status) {
        LambdaQueryWrapper<Venue> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(type != null, Venue::getType, type)
                .eq(status != null, Venue::getStatus, status)
                .orderByDesc(Venue::getCreatedAt);
        return venueMapper.selectList(wrapper);
    }

    /**
     * 根据 ID 查询场地
     *
     * @param id 场地ID
     * @return 场地实体，不存在返回 null
     */
    public Venue getById(Long id) {
        return venueMapper.selectById(id);
    }

    /**
     * 统计指定 code 的未删除记录数（用于新增时唯一性校验）
     * <p>
     * MBP {@code @TableLogic} 会自动追加 {@code deleted = 0} 条件，
     * 因此只统计未逻辑删除的记录。
     * </p>
     *
     * @param code 场地代码
     * @return 记录数
     */
    public Long countByCode(String code) {
        return venueMapper.selectCount(
                new LambdaQueryWrapper<Venue>().eq(Venue::getCode, code)
        );
    }

    /**
     * 统计指定 code 的记录数，排除指定 ID（用于更新时唯一性校验）
     * <p>
     * 排除当前 ID 自身，避免更新时误判 code 与自身冲突。
     * </p>
     *
     * @param code       场地代码
     * @param excludeId  需排除的场地ID
     * @return 记录数
     */
    public Long countByCodeExcludeId(String code, Long excludeId) {
        return venueMapper.selectCount(
                new LambdaQueryWrapper<Venue>()
                        .eq(Venue::getCode, code)
                        .ne(Venue::getId, excludeId)
        );
    }

    /**
     * 统计该场地关联的未删除赛程数（删除前占用检查）
     * <p>
     * 由于 {@link VenueMapper} 仅继承 {@code BaseMapper<Venue>}，无法直接查 as_competition 表，
     * 这里复用 {@link VenueMapper#selectVenueSchedule(Long, LocalDate)} 跨表 join 查询，
     * 取其结果集大小作为赛程数。逻辑删除过滤已在 SQL 中处理（{@code c.deleted = 0 AND e.deleted = 0}）。
     * </p>
     *
     * @param venueId 场地ID
     * @return 关联的未删除赛程数
     */
    public Long countCompetitionsByVenueId(Long venueId) {
        List<VenueScheduleVO> schedule = venueMapper.selectVenueSchedule(venueId, null);
        return (long) schedule.size();
    }

    /**
     * 查询指定场地在指定日期的赛程列表
     * <p>
     * 委派给 {@link VenueMapper#selectVenueSchedule(Long, LocalDate)}，直接返回 join 查询产物。
     * 此方法属于 join 查询例外，直接出 VO，无需 Logic 层再做 DO→VO 转换。
     * </p>
     *
     * @param venueId 场地ID（必填）
     * @param date    日期（可选，null 时不过滤日期，返回全部赛程）
     * @return 赛程列表（按开始时间升序）
     */
    public List<VenueScheduleVO> findScheduleByVenueId(Long venueId, LocalDate date) {
        return venueMapper.selectVenueSchedule(venueId, date);
    }

    // ==================== 写入操作 ====================

    /**
     * 插入场地（自动填充 createdAt / updatedAt）
     * <p>
     * Venue 实体使用 {@code @Accessors(chain = true)}，支持链式 setter。
     * insert 后重新查询以确保返回 DB 自动生成字段（主键等）的完整对象。
     * </p>
     *
     * @param venue 场地实体（不含主键和审计字段）
     * @return 插入后的场地实体（含自动生成的主键），若插入失败可能返回 null
     */
    public Venue insert(Venue venue) {
        LocalDateTime now = LocalDateTime.now();
        venue.setCreatedAt(now).setUpdatedAt(now);
        venueMapper.insert(venue);
        // 从数据库重新提取，确保字段完整
        return venueMapper.selectById(venue.getId());
    }

    /**
     * 更新场地（自动填充 updatedAt）
     * <p>
     * 仅刷新 updatedAt，其余字段以传入对象为准。update 后重新查询返回完整对象。
     * </p>
     *
     * @param venue 场地实体（必须含主键）
     * @return 更新后的场地实体，若 ID 不存在可能返回 null
     */
    public Venue update(Venue venue) {
        venue.setUpdatedAt(LocalDateTime.now());
        venueMapper.updateById(venue);
        return venueMapper.selectById(venue.getId());
    }

    /**
     * 逻辑删除场地（MyBatis-Plus 自动处理 {@code @TableLogic}）
     *
     * @param id 场地ID
     */
    public void deleteById(Long id) {
        venueMapper.deleteById(id);
    }
}
