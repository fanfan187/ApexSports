package com.fanfan.apexsports.daos;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fanfan.apexsports.cache.CompetitionCache;
import com.fanfan.apexsports.constants.CompetitionConstants;
import com.fanfan.apexsports.entities.Competition;
import com.fanfan.apexsports.entities.Event;
import com.fanfan.apexsports.entities.Participation;
import com.fanfan.apexsports.entities.Referee;
import com.fanfan.apexsports.entities.RefereeAssignment;
import com.fanfan.apexsports.entities.TeamParticipation;
import com.fanfan.apexsports.entities.Venue;
import com.fanfan.apexsports.mappers.AthleteMapper;
import com.fanfan.apexsports.mappers.CompetitionEquipmentMapper;
import com.fanfan.apexsports.mappers.CompetitionMapper;
import com.fanfan.apexsports.mappers.EquipmentMapper;
import com.fanfan.apexsports.mappers.EventMapper;
import com.fanfan.apexsports.mappers.ParticipationMapper;
import com.fanfan.apexsports.mappers.RefereeAssignmentMapper;
import com.fanfan.apexsports.mappers.RefereeMapper;
import com.fanfan.apexsports.mappers.TeamParticipationMapper;
import com.fanfan.apexsports.mappers.VenueMapper;
import com.fanfan.apexsports.utils.CacheEvictionUtil;
import com.fanfan.apexsports.vos.ScheduleBookletVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 比赛场次数据调度层
 * <p>
 * 协调 {@link CompetitionMapper}、关联 Mapper 与 {@link CompetitionCache}，负责比赛场次的
 * 存储、查询、调度，以及赛程编排的核心冲突检测。作为普通 Spring Bean，不继承 ServiceImpl，
 * 内部自动填充状态、轮次、组别、创建时间、更新时间等字段。
 * </p>
 * <p>
 * <b>关键技术点：</b>
 * <ul>
 *   <li>6 个冲突检测查询均使用 MyBatis-Plus 两步查询模式：先通过关联表 Mapper
 *       （EventMapper / RefereeAssignmentMapper / ParticipationMapper / TeamParticipationMapper）
 *       查询 ID 集合，再用 {@code LambdaQueryWrapper.in(Competition::getId, ids)} 进行场次筛选。
 *       时间冲突判定采用标准区间交集公式：{@code start_time < endTime AND end_time > startTime}。
 *       所有冲突查询统一排除已取消场次（{@code status != 3}），并通过动态追加
 *       {@code excludeCompetitionId} 条件支持"排除自身"语义
 *       （{@code null} 时不附加该条件，避免 {@code id != null} 误伤全表）。
 *       逻辑删除由 {@code @TableLogic} 自动追加 {@code deleted = 0}，无需手写。</li>
 *   <li>秩序册聚合采用 cache-aside：先查 {@link CompetitionCache#getBooklet(String)}，
 *       未命中则批量查询场次/项目/场地/裁判/参赛人数，组装 {@link ScheduleBookletVO} 列表后回写缓存。
 *       聚合 VO 构建在 DAO 完成（属于缓存聚合结果，非单条 DO 转换）。
 *       跨表聚合（参赛人数 GROUP BY、主裁判姓名）通过 CompetitionMapper XML 方法或两步查询实现。</li>
 *   <li>写操作（增/改/删/状态变更）统一驱逐所有秩序册缓存，因为任意赛程变更都可能影响任意日期的聚合结果。
 *       驱逐经 {@link CacheEvictionUtil#evictAfterCommit} 推迟到事务提交后执行，
 *       避免「事务未提交先驱逐 → 并发读回源旧数据回填缓存」的竞态。</li>
 * </ul>
 * </p>
 * <p>
 * <b>红线遵守：</b>DAO 层不直接操作 Redis（通过 {@link CompetitionCache}）、不做业务校验
 * （如 startTime &lt; endTime 在 Logic）、不做 DO→VO 转换（秩序册聚合除外）、不出现 redis/db/rdb 字样。
 * </p>
 *
 * @author fanfan187
 * @version v1.0.0
 * @since v1.0.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CompetitionDAO {

    // ==================== 日期解析常量 ====================

    /**
     * 日期字符串解析格式（{@code yyyy-MM-dd}），用于 {@code page} 与 {@code getBookletCompetitions} 的 date 参数。
     */
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    /**
     * 日期时间拼接格式（{@code yyyy-MM-dd HH:mm:ss}），用于将日期字符串拼接为零点时间戳后解析为 {@link LocalDateTime}。
     */
    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    // ==================== 依赖注入 ====================

    private final CompetitionMapper competitionMapper;
    private final CompetitionEquipmentMapper competitionEquipmentMapper;
    private final RefereeAssignmentMapper refereeAssignmentMapper;
    private final ParticipationMapper participationMapper;
    private final TeamParticipationMapper teamParticipationMapper;
    private final EventMapper eventMapper;
    private final VenueMapper venueMapper;
    private final AthleteMapper athleteMapper;
    private final RefereeMapper refereeMapper;
    private final EquipmentMapper equipmentMapper;
    private final CompetitionCache competitionCache;

    // ==================== 基础查询 ====================

    /**
     * 分页查询比赛场次（支持多条件筛选）。
     * <p>
     * date 筛选按"当天"过滤：{@code startTime >= date 00:00:00 AND startTime < date+1 00:00:00}。
     * MyBatis-Plus 的 {@code @TableLogic} 会自动追加 {@code deleted = 0} 条件。
     * </p>
     *
     * @param pageNum  当前页码
     * @param pageSize 每页大小
     * @param date     日期字符串（{@code yyyy-MM-dd}），可为 null 表示不按日期筛选
     * @param venueId  场地ID筛选（可选）
     * @param eventId  项目ID筛选（可选）
     * @param status   场次状态筛选（可选）
     * @param round    轮次筛选（可选）
     * @return 分页结果
     */
    public Page<Competition> page(int pageNum, int pageSize, String date,
                                  Long venueId, Long eventId, Integer status, Integer round) {
        LambdaQueryWrapper<Competition> wrapper = new LambdaQueryWrapper<>();
        if (date != null && !date.isBlank()) {
            LocalDateTime dayStart = LocalDateTime.parse(date + " 00:00:00", DATETIME_FORMATTER);
            LocalDateTime nextDayStart = dayStart.plusDays(1);
            wrapper.ge(Competition::getStartTime, dayStart)
                    .lt(Competition::getStartTime, nextDayStart);
        }
        wrapper.eq(venueId != null, Competition::getVenueId, venueId)
                .eq(eventId != null, Competition::getEventId, eventId)
                .eq(status != null, Competition::getStatus, status)
                .eq(round != null, Competition::getRound, round)
                .orderByAsc(Competition::getStartTime)
                .orderByAsc(Competition::getId);
        return competitionMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
    }

    /**
     * 根据 ID 查询比赛场次。
     *
     * @param id 场次ID
     * @return 场次实体，不存在返回 null
     */
    public Competition getById(Long id) {
        return competitionMapper.selectById(id);
    }

    /**
     * 根据 ID 集合批量查询比赛场次。
     *
     * @param ids 场次ID集合
     * @return 场次列表，集合为空时返回空列表
     */
    public List<Competition> findByIds(Collection<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        return competitionMapper.selectBatchIds(ids);
    }

    /**
     * 查询秩序册所需场次列表（按 startTime 升序）。
     *
     * @param date 日期字符串（{@code yyyy-MM-dd}），为 null 时返回全部未删除场次
     * @return 按 startTime 升序排列的场次列表
     */
    public List<Competition> getBookletCompetitions(String date) {
        LambdaQueryWrapper<Competition> wrapper = new LambdaQueryWrapper<>();
        if (date != null && !date.isBlank()) {
            LocalDateTime dayStart = LocalDateTime.parse(date + " 00:00:00", DATETIME_FORMATTER);
            LocalDateTime nextDayStart = dayStart.plusDays(1);
            wrapper.ge(Competition::getStartTime, dayStart)
                    .lt(Competition::getStartTime, nextDayStart);
        }
        wrapper.orderByAsc(Competition::getStartTime)
                .orderByAsc(Competition::getId);
        return competitionMapper.selectList(wrapper);
    }

    /**
     * 统计指定场次的参赛人数（个人 as_participation 行数 + 团体 as_team_participation 的 participant_count 之和）。
     *
     * @param competitionId 场次ID
     * @return 参赛人数，无记录返回 0
     */
    public Long countParticipantsByCompetitionId(Long competitionId) {
        // 个人参赛数：BaseMapper.selectCount（@TableLogic 自动追加 deleted=0）
        Long individual = participationMapper.selectCount(
                new LambdaQueryWrapper<Participation>()
                        .eq(Participation::getCompetitionId, competitionId)
        );
        // 团体参赛数：SUM(participant_count)，XML 中 COALESCE 保证非 null（@TableLogic 不走 XML，显式 deleted=0）
        Long teamSum = competitionMapper.countTeamParticipantSum(competitionId);
        long ind = individual != null ? individual : 0L;
        long team = teamSum != null ? teamSum : 0L;
        return ind + team;
    }

    /**
     * 查询指定场次的主裁判姓名。
     *
     * @param competitionId 场次ID
     * @return 主裁判姓名，无主裁判返回 null
     */
    public String getChiefRefereeName(Long competitionId) {
        // 第一步：查该场次的主裁判指派记录（@TableLogic 自动排除已删除指派）
        RefereeAssignment assignment = refereeAssignmentMapper.selectOne(
                new LambdaQueryWrapper<RefereeAssignment>()
                        .eq(RefereeAssignment::getCompetitionId, competitionId)
                        .eq(RefereeAssignment::getRole, CompetitionConstants.REFEREE_ROLE_CHIEF)
                        .last("LIMIT 1")
        );
        if (assignment == null) {
            return null;
        }
        // 第二步：查裁判姓名（@TableLogic 自动排除已删除裁判，与原 JOIN r.deleted=0 语义一致）
        Referee referee = refereeMapper.selectById(assignment.getRefereeId());
        return referee == null ? null : referee.getName();
    }

    // ==================== 冲突检测查询（MyBatis-Plus 两步查询） ====================
    // 时间交集公式：start_time < endTime AND end_time > startTime
    // 统一排除：已取消（status != 3）、自身（excludeCompetitionId 非 null 时附加）
    // 逻辑删除由 @TableLogic 自动追加 deleted=0，无需手写

    /**
     * 统计指定时间段内未取消且未删除的径赛场次数量（join as_event 取 category=1 径赛）。
     * <p>
     * 用于径赛场地资源占用校验：径赛场地同时只能承载一场径赛。
     * </p>
     *
     * @param startTime           待检测区间起点
     * @param endTime             待检测区间终点
     * @param excludeCompetitionId 需排除的场次ID（更新场景排除自身），可为 null
     * @return 时间交集的径赛场次数
     */
    public Long countTrackCompetitionsInTimeRange(LocalDateTime startTime, LocalDateTime endTime,
                                                  Long excludeCompetitionId) {
        // 第一步：查径赛项目 ID 集合（category=1 径赛；@TableLogic 自动排除已删除项目）
        List<Long> trackEventIds = eventMapper.selectList(
                        new LambdaQueryWrapper<Event>()
                                .eq(Event::getCategory, 1)
                                .select(Event::getId)
                ).stream()
                .map(Event::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        if (trackEventIds.isEmpty()) {
            // 无径赛项目，短路返回
            return 0L;
        }
        // 第二步：统计这些项目下、时间区间内未取消的场次数
        LambdaQueryWrapper<Competition> wrapper = new LambdaQueryWrapper<Competition>()
                .in(Competition::getEventId, trackEventIds)
                .lt(Competition::getStartTime, endTime)
                .gt(Competition::getEndTime, startTime)
                .ne(Competition::getStatus, CompetitionConstants.STATUS_CANCELLED);
        if (excludeCompetitionId != null) {
            wrapper.ne(Competition::getId, excludeCompetitionId);
        }
        return competitionMapper.selectCount(wrapper);
    }

    /**
     * 统计同场地同时间段内未取消且未删除的场次数（场地占用冲突检测）。
     *
     * @param venueId             场地ID
     * @param startTime           待检测区间起点
     * @param endTime             待检测区间终点
     * @param excludeCompetitionId 需排除的场次ID（更新场景排除自身），可为 null
     * @return 同场地时间交集的场次数
     */
    public Long countVenueConflicts(Long venueId, LocalDateTime startTime, LocalDateTime endTime,
                                    Long excludeCompetitionId) {
        // @TableLogic 自动追加 deleted=0；时间交集公式：start_time < endTime AND end_time > startTime
        LambdaQueryWrapper<Competition> wrapper = new LambdaQueryWrapper<Competition>()
                .eq(Competition::getVenueId, venueId)
                .lt(Competition::getStartTime, endTime)
                .gt(Competition::getEndTime, startTime)
                .ne(Competition::getStatus, CompetitionConstants.STATUS_CANCELLED);
        if (excludeCompetitionId != null) {
            // null 时不附加，避免 id != null 误伤全表
            wrapper.ne(Competition::getId, excludeCompetitionId);
        }
        return competitionMapper.selectCount(wrapper);
    }

    /**
     * 统计指定裁判在同时间段内未取消且未删除的场次数（裁判时间冲突检测）。
     * <p>
     * 通过 join as_referee_assignment 找到该裁判已指派的场次，再比时间交集。
     * </p>
     *
     * @param refereeId           裁判ID
     * @param startTime           待检测区间起点
     * @param endTime             待检测区间终点
     * @param excludeCompetitionId 需排除的场次ID（更新场景排除自身），可为 null
     * @return 裁判时间交集的场次数
     */
    public Long countRefereeConflicts(Long refereeId, LocalDateTime startTime, LocalDateTime endTime,
                                      Long excludeCompetitionId) {
        // 第一步：查该裁判已指派的场次 ID（@TableLogic 自动排除已删除指派）
        List<Long> competitionIds = refereeAssignmentMapper.selectList(
                        new LambdaQueryWrapper<RefereeAssignment>()
                                .eq(RefereeAssignment::getRefereeId, refereeId)
                                .select(RefereeAssignment::getCompetitionId)
                ).stream()
                .map(RefereeAssignment::getCompetitionId)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        if (competitionIds.isEmpty()) {
            // 裁判无任何指派，无冲突
            return 0L;
        }
        // 第二步：统计这些场次中时间冲突的场次数
        LambdaQueryWrapper<Competition> wrapper = new LambdaQueryWrapper<Competition>()
                .in(Competition::getId, competitionIds)
                .lt(Competition::getStartTime, endTime)
                .gt(Competition::getEndTime, startTime)
                .ne(Competition::getStatus, CompetitionConstants.STATUS_CANCELLED);
        if (excludeCompetitionId != null) {
            wrapper.ne(Competition::getId, excludeCompetitionId);
        }
        return competitionMapper.selectCount(wrapper);
    }

    /**
     * 统计指定场次已指派的主裁判数量（role=1）。
     * <p>
     * 用于"一场比赛仅一名主裁判"约束校验。
     * </p>
     *
     * @param competitionId 场次ID
     * @return 主裁判数量
     */
    public Long countChiefReferee(Long competitionId) {
        // @TableLogic 自动追加 deleted=0
        Long count = refereeAssignmentMapper.selectCount(
                new LambdaQueryWrapper<RefereeAssignment>()
                        .eq(RefereeAssignment::getCompetitionId, competitionId)
                        .eq(RefereeAssignment::getRole, CompetitionConstants.REFEREE_ROLE_CHIEF)
        );
        return count != null ? count : 0L;
    }

    /**
     * 统计指定运动员在同时间段内未取消且未删除的场次数（运动员时间冲突检测）。
     * <p>
     * 通过 join as_participation 找到该运动员已编排的场次，再比时间交集。
     * </p>
     *
     * @param athleteId           运动员ID
     * @param startTime           待检测区间起点
     * @param endTime             待检测区间终点
     * @param excludeCompetitionId 需排除的场次ID（更新场景排除自身），可为 null
     * @return 运动员时间交集的场次数
     */
    public Long countAthleteTimeConflicts(Long athleteId, LocalDateTime startTime, LocalDateTime endTime,
                                          Long excludeCompetitionId) {
        // 第一步：查该运动员已编排的场次 ID（@TableLogic 自动排除已删除参赛记录）
        List<Long> competitionIds = participationMapper.selectList(
                        new LambdaQueryWrapper<Participation>()
                                .eq(Participation::getAthleteId, athleteId)
                                .select(Participation::getCompetitionId)
                ).stream()
                .map(Participation::getCompetitionId)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        if (competitionIds.isEmpty()) {
            // 运动员未参加任何场次，无冲突
            return 0L;
        }
        // 第二步：统计这些场次中时间冲突的场次数
        LambdaQueryWrapper<Competition> wrapper = new LambdaQueryWrapper<Competition>()
                .in(Competition::getId, competitionIds)
                .lt(Competition::getStartTime, endTime)
                .gt(Competition::getEndTime, startTime)
                .ne(Competition::getStatus, CompetitionConstants.STATUS_CANCELLED);
        if (excludeCompetitionId != null) {
            wrapper.ne(Competition::getId, excludeCompetitionId);
        }
        return competitionMapper.selectCount(wrapper);
    }

    /**
     * 统计指定学院在同时间段内未取消且未删除的团体场次数（学院团体时间冲突检测）。
     * <p>
     * 通过 join as_team_participation 找到该学院已编排的团体场次，再比时间交集。
     * </p>
     *
     * @param collegeId           学院ID
     * @param startTime           待检测区间起点
     * @param endTime             待检测区间终点
     * @param excludeCompetitionId 需排除的场次ID（更新场景排除自身），可为 null
     * @return 学院团体时间交集的场次数
     */
    public Long countCollegeTeamTimeConflicts(Long collegeId, LocalDateTime startTime, LocalDateTime endTime,
                                              Long excludeCompetitionId) {
        // 第一步：查该学院已编排的团体场次 ID（@TableLogic 自动排除已删除团体参赛记录）
        List<Long> competitionIds = teamParticipationMapper.selectList(
                        new LambdaQueryWrapper<TeamParticipation>()
                                .eq(TeamParticipation::getCollegeId, collegeId)
                                .select(TeamParticipation::getCompetitionId)
                ).stream()
                .map(TeamParticipation::getCompetitionId)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        if (competitionIds.isEmpty()) {
            // 学院未参加任何团体场次，无冲突
            return 0L;
        }
        // 第二步：统计这些场次中时间冲突的场次数
        LambdaQueryWrapper<Competition> wrapper = new LambdaQueryWrapper<Competition>()
                .in(Competition::getId, competitionIds)
                .lt(Competition::getStartTime, endTime)
                .gt(Competition::getEndTime, startTime)
                .ne(Competition::getStatus, CompetitionConstants.STATUS_CANCELLED);
        if (excludeCompetitionId != null) {
            wrapper.ne(Competition::getId, excludeCompetitionId);
        }
        return competitionMapper.selectCount(wrapper);
    }

    // ==================== 写入操作 ====================

    /**
     * 插入比赛场次（自动填充状态、轮次、组别、逻辑删除标记、创建时间、更新时间）。
     *
     * @param competition 比赛场次实体（不含主键和审计字段）
     * @return 插入后的比赛场次实体（含自动生成的主键，重新查库保证字段完整）
     */
    public Competition insert(Competition competition) {
        competition.setStatus(CompetitionConstants.DEFAULT_STATUS);
        competition.setDeleted(0);
        competition.setCreatedAt(LocalDateTime.now());
        competition.setUpdatedAt(LocalDateTime.now());
        if (competition.getGroupNo() == null) {
            competition.setGroupNo(CompetitionConstants.DEFAULT_GROUP_NO);
        }
        if (competition.getRound() == null) {
            competition.setRound(CompetitionConstants.ROUND_PRELIMINARY);
        }
        competitionMapper.insert(competition);
        Competition inserted = competitionMapper.selectById(competition.getId());
        // 写操作：驱逐所有秩序册缓存（赛程变更影响任意日期聚合结果，事务提交后执行）
        CacheEvictionUtil.evictAfterCommit(() -> competitionCache.evictAllBooklets());
        return inserted;
    }

    /**
     * 更新比赛场次（自动更新修改时间）。
     *
     * @param competition 比赛场次实体
     * @return 更新后的比赛场次实体
     */
    public Competition update(Competition competition) {
        competition.setUpdatedAt(LocalDateTime.now());
        competitionMapper.updateById(competition);
        Competition updated = competitionMapper.selectById(competition.getId());
        // 写操作：驱逐所有秩序册缓存（事务提交后执行）
        CacheEvictionUtil.evictAfterCommit(() -> competitionCache.evictAllBooklets());
        return updated;
    }

    /**
     * 逻辑删除比赛场次（MyBatis-Plus 自动处理 @TableLogic）。
     *
     * @param id 场次ID
     */
    public void deleteById(Long id) {
        competitionMapper.deleteById(id);
        // 写操作：驱逐所有秩序册缓存（事务提交后执行）
        CacheEvictionUtil.evictAfterCommit(() -> competitionCache.evictAllBooklets());
    }

    /**
     * 更新场次状态。
     *
     * @param id     场次ID
     * @param status 新状态
     */
    public void updateStatus(Long id, Integer status) {
        Competition patch = new Competition();
        patch.setId(id);
        patch.setStatus(status);
        patch.setUpdatedAt(LocalDateTime.now());
        competitionMapper.updateById(patch);
        // 写操作：驱逐所有秩序册缓存（事务提交后执行）
        CacheEvictionUtil.evictAfterCommit(() -> competitionCache.evictAllBooklets());
    }

    // ==================== 秩序册聚合缓存（cache-aside） ====================

    /**
     * 获取秩序册聚合结果（cache-aside）。
     * <p>
     * 先查 {@link CompetitionCache#getBooklet(String)}，未命中则批量查询场次/项目/场地/裁判/参赛人数，
     * 组装 {@link ScheduleBookletVO} 列表后回写缓存。
     * </p>
     * <p>
     * dateKey 规则：{@code "all"} 表示全部日期聚合，形如 {@code "2026-05-15"} 表示指定日期聚合。
     * </p>
     *
     * @param date 日期字符串（{@code yyyy-MM-dd}），为 null 或空时聚合全部日期（dateKey="all"）
     * @return 秩序册条目列表
     */
    public List<ScheduleBookletVO> getBooklet(String date) {
        String dateKey = (date == null || date.isBlank()) ? "all" : date;
        // 1. 先查缓存
        List<ScheduleBookletVO> cached = competitionCache.getBooklet(dateKey);
        if (cached != null) {
            return cached;
        }
        // 2. 缓存未命中，查库聚合（dateKey="all" 时传 null，查询全部未删除场次）
        List<Competition> competitions = getBookletCompetitions("all".equals(dateKey) ? null : date);
        if (competitions.isEmpty()) {
            List<ScheduleBookletVO> empty = List.of();
            competitionCache.cacheBooklet(dateKey, empty);
            return empty;
        }
        List<ScheduleBookletVO> booklets = buildBookletVos(competitions);
        // 3. 回写缓存
        competitionCache.cacheBooklet(dateKey, booklets);
        return booklets;
    }

    /**
     * 驱逐所有秩序册缓存。
     * <p>
     * 委托 {@link CompetitionCache#evictAllBooklets()}，供 Logic 层在跨模块写操作
     * （如参赛队变更、裁判指派）后手动驱逐缓存。驱逐经
     * {@link CacheEvictionUtil#evictAfterCommit} 推迟到事务提交后执行，
     * 与调用方 Logic 的 {@code @Transactional} 边界对齐，避免事务未提交先驱逐
     * 导致并发读回填旧数据。
     * </p>
     */
    public void evictAllBooklets() {
        CacheEvictionUtil.evictAfterCommit(() -> competitionCache.evictAllBooklets());
    }

    // ==================== 秩序册聚合内部构建 ====================

    /**
     * 批量构建秩序册 VO 列表。
     * <p>
     * 通过批量查询项目/场地/裁判/参赛人数，避免 N+1 查询：
     * <ul>
     *   <li>项目信息（name/type/category）：{@code eventMapper.selectBatchIds}</li>
     *   <li>场地名称：{@code venueMapper.selectBatchIds}</li>
     *   <li>主裁判姓名：两步查询（refereeAssignmentMapper.selectList + refereeMapper.selectBatchIds）</li>
     *   <li>参赛人数：CompetitionMapper XML 方法分别 GROUP BY 统计个人/团体参赛数，合并为 Map</li>
     * </ul>
     * </p>
     *
     * @param competitions 场次列表
     * @return 秩序册 VO 列表（与入参顺序一致）
     */
    private List<ScheduleBookletVO> buildBookletVos(List<Competition> competitions) {
        List<Long> competitionIds = competitions.stream()
                .map(Competition::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        List<Long> eventIds = competitions.stream()
                .map(Competition::getEventId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        List<Long> venueIds = competitions.stream()
                .map(Competition::getVenueId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());

        // 批量查项目
        Map<Long, Event> eventMap = eventIds.isEmpty()
                ? Collections.emptyMap()
                : eventMapper.selectBatchIds(eventIds).stream()
                .collect(Collectors.toMap(Event::getId, e -> e, (a, b) -> a));
        // 批量查场地
        Map<Long, Venue> venueMap = venueIds.isEmpty()
                ? Collections.emptyMap()
                : venueMapper.selectBatchIds(venueIds).stream()
                .collect(Collectors.toMap(Venue::getId, v -> v, (a, b) -> a));
        // 批量查主裁判姓名
        Map<Long, String> chiefRefereeMap = batchQueryChiefReferees(competitionIds);
        // 批量查参赛人数（个人+团体合并）
        Map<Long, Long> participantCountMap = batchQueryParticipantCounts(competitionIds);

        return competitions.stream()
                .map(c -> {
                    Event event = c.getEventId() == null ? null : eventMap.get(c.getEventId());
                    Venue venue = c.getVenueId() == null ? null : venueMap.get(c.getVenueId());
                    String eventName = event != null ? event.getName() : null;
                    Integer eventType = event != null ? event.getType() : null;
                    Integer eventCategory = event != null ? event.getCategory() : null;
                    String venueName = venue != null ? venue.getName() : null;
                    String chiefReferee = chiefRefereeMap.get(c.getId());
                    long participantCount = participantCountMap.getOrDefault(c.getId(), 0L);
                    return ScheduleBookletVO.from(c, eventName, eventType, eventCategory,
                            venueName, (int) participantCount, chiefReferee);
                })
                .collect(Collectors.toList());
    }

    /**
     * 批量查询多场次的主裁判姓名（每个场次最多取一名主裁判）。
     *
     * @param competitionIds 场次ID集合
     * @return {@code competitionId → 主裁判姓名} 映射，无主裁判的场次不在 Map 中
     */
    private Map<Long, String> batchQueryChiefReferees(List<Long> competitionIds) {
        if (competitionIds == null || competitionIds.isEmpty()) {
            return Collections.emptyMap();
        }
        // 第一步：批量查这些场次的主裁判指派记录（@TableLogic 自动排除已删除指派）
        List<RefereeAssignment> assignments = refereeAssignmentMapper.selectList(
                new LambdaQueryWrapper<RefereeAssignment>()
                        .in(RefereeAssignment::getCompetitionId, competitionIds)
                        .eq(RefereeAssignment::getRole, CompetitionConstants.REFEREE_ROLE_CHIEF)
        );
        if (assignments.isEmpty()) {
            return Collections.emptyMap();
        }
        // 第二步：批量查裁判姓名（@TableLogic 自动排除已删除裁判，等价原 JOIN r.deleted=0）
        List<Long> refereeIds = assignments.stream()
                .map(RefereeAssignment::getRefereeId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        Map<Long, String> refereeNameById = refereeIds.isEmpty()
                ? Collections.emptyMap()
                : refereeMapper.selectBatchIds(refereeIds).stream()
                        .collect(Collectors.toMap(Referee::getId, Referee::getName, (a, b) -> a));
        // 第三步：Java 层组装 competitionId → refereeName
        Map<Long, String> result = new HashMap<>();
        for (RefereeAssignment ra : assignments) {
            String name = refereeNameById.get(ra.getRefereeId());
            if (name != null) {
                // 裁判被逻辑删除时跳过（与原 JOIN 行为一致）
                result.put(ra.getCompetitionId(), name);
            }
        }
        return result;
    }

    /**
     * 批量统计多场次的参赛人数（个人 as_participation 行数 + 团体 as_team_participation 的 participant_count 之和）。
     *
     * @param competitionIds 场次ID集合
     * @return {@code competitionId → 参赛人数} 映射，无记录的场次值为 0
     */
    private Map<Long, Long> batchQueryParticipantCounts(List<Long> competitionIds) {
        if (competitionIds == null || competitionIds.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<Long, Long> result = new HashMap<>();
        competitionIds.forEach(id -> result.put(id, 0L));

        // 个人参赛数：XML 中 GROUP BY COUNT（XML 不走 @TableLogic，已显式 deleted=0）
        List<Map<String, Object>> individualRows = competitionMapper.batchCountIndividualParticipants(competitionIds);
        if (individualRows != null) {
            for (Map<String, Object> row : individualRows) {
                // 列驱动可能返回 BigDecimal/BigInteger/Integer，统一用 Number.longValue() 转换最安全
                Long competitionId = ((Number) row.get("competition_id")).longValue();
                Long cnt = ((Number) row.get("cnt")).longValue();
                result.merge(competitionId, cnt, Long::sum);
            }
        }

        // 团体参赛数：XML 中 GROUP BY SUM(participant_count)
        List<Map<String, Object>> teamRows = competitionMapper.batchSumTeamParticipants(competitionIds);
        if (teamRows != null) {
            for (Map<String, Object> row : teamRows) {
                Long competitionId = ((Number) row.get("competition_id")).longValue();
                Long cnt = ((Number) row.get("cnt")).longValue();
                result.merge(competitionId, cnt, Long::sum);
            }
        }

        return result;
    }

    /**
     * 根据 IN 子句的参数个数生成占位符字符串（如 3 个参数生成 {@code "?,?,?"}）。
     *
     * @param count 参数个数
     * @return 占位符字符串
     */
    private String buildInPlaceholders(int count) {
        if (count <= 0) {
            // 理论上调用方已做非空校验，此处兜底返回不可能匹配的占位符，避免 SQL 语法错误
            return "NULL";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < count; i++) {
            if (i > 0) {
                sb.append(',');
            }
            sb.append('?');
        }
        return sb.toString();
    }
}
