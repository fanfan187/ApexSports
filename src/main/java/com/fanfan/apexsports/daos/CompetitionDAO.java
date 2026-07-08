package com.fanfan.apexsports.daos;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fanfan.apexsports.cache.CompetitionCache;
import com.fanfan.apexsports.constants.CompetitionConstants;
import com.fanfan.apexsports.entities.Competition;
import com.fanfan.apexsports.entities.Event;
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
import com.fanfan.apexsports.vos.ScheduleBookletVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
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
 *   <li>6 个冲突检测查询均使用 {@link JdbcTemplate} 原生 SQL，涉及跨表 JOIN
 *       （as_event / as_referee_assignment / as_participation / as_team_participation）。
 *       时间冲突判定采用标准区间交集公式：{@code c.start_time < :endTime AND c.end_time > :startTime}。
 *       所有冲突查询统一排除已取消场次（status != 3）与逻辑删除记录（deleted = 0），
 *       并通过动态拼接 {@code excludeCompetitionId} 条件支持"排除自身"语义
 *       （{@code null} 时不附加该条件，避免 {@code id != null} 误伤全表）。</li>
 *   <li>秩序册聚合采用 cache-aside：先查 {@link CompetitionCache#getBooklet(String)}，
 *       未命中则批量查询场次/项目/场地/裁判/参赛人数，组装 {@link ScheduleBookletVO} 列表后回写缓存。
 *       聚合 VO 构建在 DAO 完成（属于缓存聚合结果，非单条 DO 转换）。</li>
 *   <li>写操作（增/改/删/状态变更）统一驱逐所有秩序册缓存，因为任意赛程变更都可能影响任意日期的聚合结果。</li>
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
    private final JdbcTemplate jdbcTemplate;
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
        Long individual = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM as_participation WHERE competition_id = ? AND deleted = 0",
                Long.class, competitionId
        );
        Long teamSum = jdbcTemplate.queryForObject(
                "SELECT COALESCE(SUM(participant_count), 0) FROM as_team_participation WHERE competition_id = ? AND deleted = 0",
                Long.class, competitionId
        );
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
        List<String> names = jdbcTemplate.query(
                "SELECT r.name FROM as_referee_assignment ra " +
                        "JOIN as_referee r ON r.id = ra.referee_id AND r.deleted = 0 " +
                        "WHERE ra.competition_id = ? AND ra.role = ? AND ra.deleted = 0 " +
                        "LIMIT 1",
                (rs, rowNum) -> rs.getString("name"),
                competitionId, CompetitionConstants.REFEREE_ROLE_CHIEF
        );
        return names.isEmpty() ? null : names.get(0);
    }

    // ==================== 冲突检测查询（JdbcTemplate 原生 SQL） ====================
    // 时间交集公式：c.start_time < :endTime AND c.end_time > :startTime
    // 统一排除：已取消（status != 3）、逻辑删除（deleted = 0）、自身（excludeCompetitionId 非 null 时附加）

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
        StringBuilder sql = new StringBuilder(
                "SELECT COUNT(*) FROM as_competition c " +
                        "JOIN as_event e ON e.id = c.event_id AND e.deleted = 0 " +
                        "WHERE e.category = ? " +
                        "AND c.start_time < ? AND c.end_time > ? " +
                        "AND c.status != ? AND c.deleted = 0"
        );
        List<Object> params = new ArrayList<>();
        params.add(1); // 径赛 category=1
        params.add(endTime);
        params.add(startTime);
        params.add(CompetitionConstants.STATUS_CANCELLED);
        if (excludeCompetitionId != null) {
            sql.append(" AND c.id != ?");
            params.add(excludeCompetitionId);
        }
        return jdbcTemplate.queryForObject(sql.toString(), Long.class, params.toArray());
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
        StringBuilder sql = new StringBuilder(
                "SELECT COUNT(*) FROM as_competition c " +
                        "WHERE c.venue_id = ? " +
                        "AND c.start_time < ? AND c.end_time > ? " +
                        "AND c.status != ? AND c.deleted = 0"
        );
        List<Object> params = new ArrayList<>();
        params.add(venueId);
        params.add(endTime);
        params.add(startTime);
        params.add(CompetitionConstants.STATUS_CANCELLED);
        if (excludeCompetitionId != null) {
            sql.append(" AND c.id != ?");
            params.add(excludeCompetitionId);
        }
        return jdbcTemplate.queryForObject(sql.toString(), Long.class, params.toArray());
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
        StringBuilder sql = new StringBuilder(
                "SELECT COUNT(*) FROM as_competition c " +
                        "JOIN as_referee_assignment ra ON ra.competition_id = c.id " +
                        "AND ra.referee_id = ? AND ra.deleted = 0 " +
                        "WHERE c.start_time < ? AND c.end_time > ? " +
                        "AND c.status != ? AND c.deleted = 0"
        );
        List<Object> params = new ArrayList<>();
        params.add(refereeId);
        params.add(endTime);
        params.add(startTime);
        params.add(CompetitionConstants.STATUS_CANCELLED);
        if (excludeCompetitionId != null) {
            sql.append(" AND c.id != ?");
            params.add(excludeCompetitionId);
        }
        return jdbcTemplate.queryForObject(sql.toString(), Long.class, params.toArray());
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
        Long count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM as_referee_assignment " +
                        "WHERE competition_id = ? AND role = ? AND deleted = 0",
                Long.class, competitionId, CompetitionConstants.REFEREE_ROLE_CHIEF
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
        StringBuilder sql = new StringBuilder(
                "SELECT COUNT(*) FROM as_competition c " +
                        "JOIN as_participation p ON p.competition_id = c.id " +
                        "AND p.athlete_id = ? AND p.deleted = 0 " +
                        "WHERE c.start_time < ? AND c.end_time > ? " +
                        "AND c.status != ? AND c.deleted = 0"
        );
        List<Object> params = new ArrayList<>();
        params.add(athleteId);
        params.add(endTime);
        params.add(startTime);
        params.add(CompetitionConstants.STATUS_CANCELLED);
        if (excludeCompetitionId != null) {
            sql.append(" AND c.id != ?");
            params.add(excludeCompetitionId);
        }
        return jdbcTemplate.queryForObject(sql.toString(), Long.class, params.toArray());
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
        StringBuilder sql = new StringBuilder(
                "SELECT COUNT(*) FROM as_competition c " +
                        "JOIN as_team_participation tp ON tp.competition_id = c.id " +
                        "AND tp.college_id = ? AND tp.deleted = 0 " +
                        "WHERE c.start_time < ? AND c.end_time > ? " +
                        "AND c.status != ? AND c.deleted = 0"
        );
        List<Object> params = new ArrayList<>();
        params.add(collegeId);
        params.add(endTime);
        params.add(startTime);
        params.add(CompetitionConstants.STATUS_CANCELLED);
        if (excludeCompetitionId != null) {
            sql.append(" AND c.id != ?");
            params.add(excludeCompetitionId);
        }
        return jdbcTemplate.queryForObject(sql.toString(), Long.class, params.toArray());
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
        // 写操作：驱逐所有秩序册缓存（赛程变更影响任意日期聚合结果）
        competitionCache.evictAllBooklets();
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
        competitionCache.evictAllBooklets();
        return updated;
    }

    /**
     * 逻辑删除比赛场次（MyBatis-Plus 自动处理 @TableLogic）。
     *
     * @param id 场次ID
     */
    public void deleteById(Long id) {
        competitionMapper.deleteById(id);
        competitionCache.evictAllBooklets();
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
        competitionCache.evictAllBooklets();
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
     * （如参赛队变更、裁判指派）后手动驱逐缓存。
     * </p>
     */
    public void evictAllBooklets() {
        competitionCache.evictAllBooklets();
    }

    // ==================== 秩序册聚合内部构建 ====================

    /**
     * 批量构建秩序册 VO 列表。
     * <p>
     * 通过批量查询项目/场地/裁判/参赛人数，避免 N+1 查询：
     * <ul>
     *   <li>项目信息（name/type/category）：{@code eventMapper.selectBatchIds}</li>
     *   <li>场地名称：{@code venueMapper.selectBatchIds}</li>
     *   <li>主裁判姓名：单条 SQL join as_referee_assignment + as_referee，IN 批量查询</li>
     *   <li>参赛人数：两条 SQL 分别 GROUP BY 统计个人/团体参赛数，合并为 Map</li>
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
        String placeholders = buildInPlaceholders(competitionIds.size());
        String sql = "SELECT ra.competition_id AS competition_id, r.name AS name " +
                "FROM as_referee_assignment ra " +
                "JOIN as_referee r ON r.id = ra.referee_id AND r.deleted = 0 " +
                "WHERE ra.role = ? AND ra.deleted = 0 " +
                "AND ra.competition_id IN (" + placeholders + ")";
        List<Object> params = new ArrayList<>();
        params.add(CompetitionConstants.REFEREE_ROLE_CHIEF);
        params.addAll(competitionIds);
        Map<Long, String> result = new HashMap<>();
        jdbcTemplate.query(sql, (rs, rowNum) -> {
            result.put(rs.getLong("competition_id"), rs.getString("name"));
            return null;
        }, params.toArray());
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
        String placeholders = buildInPlaceholders(competitionIds.size());
        Map<Long, Long> result = new HashMap<>();
        competitionIds.forEach(id -> result.put(id, 0L));

        // 个人参赛数
        String individualSql = "SELECT competition_id AS competition_id, COUNT(*) AS cnt " +
                "FROM as_participation WHERE deleted = 0 " +
                "AND competition_id IN (" + placeholders + ") " +
                "GROUP BY competition_id";
        jdbcTemplate.query(individualSql, (rs, rowNum) -> {
            result.merge(rs.getLong("competition_id"), rs.getLong("cnt"), Long::sum);
            return null;
        }, competitionIds.toArray());

        // 团体参赛数（按 participant_count 求和）
        String teamSql = "SELECT competition_id AS competition_id, COALESCE(SUM(participant_count), 0) AS cnt " +
                "FROM as_team_participation WHERE deleted = 0 " +
                "AND competition_id IN (" + placeholders + ") " +
                "GROUP BY competition_id";
        jdbcTemplate.query(teamSql, (rs, rowNum) -> {
            result.merge(rs.getLong("competition_id"), rs.getLong("cnt"), Long::sum);
            return null;
        }, competitionIds.toArray());

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
