package com.fanfan.apexsports.daos;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fanfan.apexsports.cache.DashboardCache;
import com.fanfan.apexsports.constants.CompetitionConstants;
import com.fanfan.apexsports.constants.DashboardConstants;
import com.fanfan.apexsports.constants.RecordConstants;
import com.fanfan.apexsports.entities.Competition;
import com.fanfan.apexsports.entities.Event;
import com.fanfan.apexsports.entities.Record;
import com.fanfan.apexsports.entities.Referee;
import com.fanfan.apexsports.entities.RefereeAssignment;
import com.fanfan.apexsports.entities.Venue;
import com.fanfan.apexsports.mappers.AthleteMapper;
import com.fanfan.apexsports.mappers.CollegeMapper;
import com.fanfan.apexsports.mappers.CompetitionMapper;
import com.fanfan.apexsports.mappers.EventMapper;
import com.fanfan.apexsports.mappers.ParticipationMapper;
import com.fanfan.apexsports.mappers.RecordMapper;
import com.fanfan.apexsports.mappers.RefereeAssignmentMapper;
import com.fanfan.apexsports.mappers.RefereeMapper;
import com.fanfan.apexsports.mappers.VenueMapper;
import com.fanfan.apexsports.vos.BreakingRecordItemVO;
import com.fanfan.apexsports.vos.BreakingRecordsVO;
import com.fanfan.apexsports.vos.DashboardOverviewVO;
import com.fanfan.apexsports.vos.TodayScheduleItemVO;
import com.fanfan.apexsports.vos.TodayScheduleVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 首页看板数据调度层
 * <p>
 * 协调各业务 Mapper 与 {@link DashboardCache}，负责看板三个只读聚合结果的 cache-aside
 * 查询：先查缓存，未命中则调用各 Mapper 聚合数据后回写缓存返回。
 * 作为普通 Spring Bean，<b>不继承 ServiceImpl</b>，内部不做业务校验、不做 DO→VO 业务转换。
 * </p>
 * <p>
 * <b>骨架阶段（T1）：</b>三个方法仅声明签名并抛出 {@link UnsupportedOperationException}，
 * cache-aside 实现与各 Mapper 聚合逻辑留待 T2/T3/T4 全链路实现时填充。
 * </p>
 * <p>
 * <b>红线遵守：</b>DAO 通过 {@link DashboardCache} 操作缓存（不直接 Redis）；
 * 不调 Logic；不暴露 DO 到 Logic 之外的视图层；不出现底层存储相关字样。
 * </p>
 *
 * @author fanfan187
 * @version v1.0.0
 * @since v1.0.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DashboardDAO {

    // ==================== 依赖注入 ====================

    private final AthleteMapper athleteMapper;
    private final CollegeMapper collegeMapper;
    private final EventMapper eventMapper;
    private final VenueMapper venueMapper;
    private final ParticipationMapper participationMapper;
    private final CompetitionMapper competitionMapper;
    private final RecordMapper recordMapper;
    private final RefereeAssignmentMapper refereeAssignmentMapper;
    private final RefereeMapper refereeMapper;
    private final RefereeAssignmentDAO refereeAssignmentDAO;
    private final DashboardCache dashboardCache;

    // ==================== 概览 ====================

    /**
     * 查询首页概览聚合数据（cache-aside）。
     * <p>
     * 流程：
     * <ol>
     *   <li>先查 {@link DashboardCache#getOverview()}，命中则直接返回；</li>
     *   <li>未命中则并行调用各业务 Mapper 聚合 9 项指标（其中 totalRegistrations
     *       含已取消报名，Docs §85 规则#4；totalCompetitions / todayCompetitions
     *       排除 CANCELLED=3）；</li>
     *   <li>组装 {@link DashboardOverviewVO}（{@code scheduleProgress} 留空 null，
     *       由 Logic 层计算）；</li>
     *   <li>回写缓存后返回。</li>
     * </ol>
     * </p>
     * <p>
     * {@code @TableLogic} 由 MyBatis-Plus 自动追加 {@code deleted = 0}，
     * selectCount(null) 表示全表有效行计数。
     * </p>
     *
     * @return 概览聚合 VO（{@code scheduleProgress=null}，待 Logic 计算）
     */
    public DashboardOverviewVO getOverviewData() {
        // 1. 先查缓存
        DashboardOverviewVO cached = dashboardCache.getOverview();
        if (cached != null) {
            return cached;
        }

        // 2. 缓存未命中，查库聚合
        LocalDate today = LocalDate.now();
        LocalDateTime todayStart = today.atStartOfDay();
        LocalDateTime tomorrowStart = today.plusDays(1).atStartOfDay();

        Long totalAthletes = athleteMapper.selectCount(null);
        Long totalColleges = collegeMapper.selectCount(null);
        Long totalEvents = eventMapper.selectCount(null);
        // 报名总数含已取消（Docs §85 规则#4，不加状态过滤）
        Long totalRegistrations = participationMapper.selectCount(null);
        // 比赛总场次：排除 CANCELLED=3
        Long totalCompetitions = competitionMapper.selectCount(
                new LambdaQueryWrapper<Competition>()
                        .ne(Competition::getStatus, CompetitionConstants.STATUS_CANCELLED)
        );
        // 已完成场次：status=FINISHED
        Long completedCompetitions = competitionMapper.selectCount(
                new LambdaQueryWrapper<Competition>()
                        .eq(Competition::getStatus, CompetitionConstants.STATUS_FINISHED)
        );
        // 今日场次：startTime 落在今日，且未取消
        Long todayCompetitions = competitionMapper.selectCount(
                new LambdaQueryWrapper<Competition>()
                        .ge(Competition::getStartTime, todayStart)
                        .lt(Competition::getStartTime, tomorrowStart)
                        .ne(Competition::getStatus, CompetitionConstants.STATUS_CANCELLED)
        );
        // 破纪录数：status=ACTIVE 的有效纪录条数
        Long recordBreakCount = recordMapper.selectCount(
                new LambdaQueryWrapper<Record>()
                        .eq(Record::getStatus, RecordConstants.STATUS_ACTIVE)
        );

        DashboardOverviewVO vo = new DashboardOverviewVO(
                toInt(totalAthletes),
                toInt(totalColleges),
                toInt(totalEvents),
                toInt(totalRegistrations),
                toInt(completedCompetitions),
                toInt(totalCompetitions),
                null, // scheduleProgress 由 Logic 层计算
                toInt(recordBreakCount),
                toInt(todayCompetitions)
        );

        // 3. 回写缓存后返回
        dashboardCache.cacheOverview(vo);
        return vo;
    }

    /**
     * 将 MyBatis-Plus 的 Long 计数安全转换为 Integer（VO 字段类型）。
     * <p>
     * selectCount 在无数据时可能返回 null，统一兜底为 0。
     * </p>
     */
    private Integer toInt(Long value) {
        return value == null ? 0 : value.intValue();
    }

    // ==================== 今日赛程 ====================

    /**
     * 查询今日赛程聚合数据（cache-aside）。
     * <p>
     * 流程：
     * <ol>
     *   <li>先查 {@link DashboardCache#getTodaySchedule(LocalDate)}，命中直接返回；</li>
     *   <li>未命中则按 startTime 落在今日 [{@code today 00:00}, {@code today+1 00:00}) 且
     *       {@code status != CANCELLED(3)} 查询比赛场次，按 startTime 升序排列；</li>
     *   <li>批量填充项目名/场地名/主裁判姓名/参赛人数（避免 N+1，仿
     *       {@link CompetitionDAO} 的 batch 范式）；</li>
     *   <li>组装 {@link TodayScheduleVO} 后回写缓存返回。</li>
     * </ol>
     * </p>
     * <p>
     * {@code @TableLogic} 由 MyBatis-Plus 自动追加 {@code deleted = 0}；XML 方法
     * （batchCountIndividualParticipants / batchSumTeamParticipants）已显式 {@code deleted = 0}。
     * </p>
     *
     * @param today 服务器今日日期
     * @return 今日赛程聚合 VO（无场次时 totalCompetitions=0、competitions 为空列表）
     */
    public TodayScheduleVO getTodayScheduleData(LocalDate today) {
        // 1. 先查缓存
        TodayScheduleVO cached = dashboardCache.getTodaySchedule(today);
        if (cached != null) {
            return cached;
        }

        // 2. 缓存未命中，查今日未取消场次（按 startTime 升序、id 升序保证稳定排序）
        LocalDateTime todayStart = today.atStartOfDay();
        LocalDateTime tomorrowStart = today.plusDays(1).atStartOfDay();
        List<Competition> competitions = competitionMapper.selectList(
                new LambdaQueryWrapper<Competition>()
                        .ge(Competition::getStartTime, todayStart)
                        .lt(Competition::getStartTime, tomorrowStart)
                        .ne(Competition::getStatus, CompetitionConstants.STATUS_CANCELLED)
                        .orderByAsc(Competition::getStartTime)
                        .orderByAsc(Competition::getId)
        );

        String dateStr = today.format(DashboardConstants.DATE_FMT);
        // 3. 空列表短路：直接组装空 VO 回写缓存返回
        if (competitions.isEmpty()) {
            TodayScheduleVO empty = new TodayScheduleVO(dateStr, 0, Collections.emptyList());
            dashboardCache.cacheTodaySchedule(today, empty);
            return empty;
        }

        // 4. 收集 ID 集合（distinct 去重，避免重复行）
        List<Long> competitionIds = competitions.stream()
                .map(Competition::getId).filter(Objects::nonNull).collect(Collectors.toList());
        List<Long> eventIds = competitions.stream()
                .map(Competition::getEventId).filter(Objects::nonNull)
                .distinct().collect(Collectors.toList());
        List<Long> venueIds = competitions.stream()
                .map(Competition::getVenueId).filter(Objects::nonNull)
                .distinct().collect(Collectors.toList());

        // 5. 批量查项目/场地名称（Map<id,name>）
        Map<Long, String> eventIdToName = eventIds.isEmpty()
                ? Collections.emptyMap()
                : eventMapper.selectBatchIds(eventIds).stream()
                        .collect(Collectors.toMap(Event::getId, Event::getName, (a, b) -> a));
        Map<Long, String> venueIdToName = venueIds.isEmpty()
                ? Collections.emptyMap()
                : venueMapper.selectBatchIds(venueIds).stream()
                        .collect(Collectors.toMap(Venue::getId, Venue::getName, (a, b) -> a));

        // 6. 批量查主裁判姓名（competitionId → refereeName，无主裁判不在 Map 中）
        Map<Long, String> chiefRefereeMap = batchQueryChiefRefereeNames(competitionIds);
        // 7. 批量统计参赛人数（个人 + 团体合并，未匹配场次为 0）
        Map<Long, Long> participantCountMap = batchQueryParticipantCounts(competitionIds);

        // 8. 组装 VO 列表（保持查询返回的升序顺序）
        List<TodayScheduleItemVO> items = new ArrayList<>(competitions.size());
        for (Competition c : competitions) {
            items.add(new TodayScheduleItemVO(
                    c.getId(),
                    c.getEventId() == null ? null : eventIdToName.get(c.getEventId()),
                    c.getVenueId() == null ? null : venueIdToName.get(c.getVenueId()),
                    c.getStartTime() == null ? null : c.getStartTime().format(DashboardConstants.HH_MM),
                    c.getEndTime() == null ? null : c.getEndTime().format(DashboardConstants.HH_MM),
                    toInt(participantCountMap.get(c.getId())),
                    chiefRefereeMap.get(c.getId()),
                    DashboardConstants.COMPETITION_STATUS_TEXT.get(c.getStatus()),
                    c.getGroupNo()
            ));
        }

        // 9. 组装 VO 并回写缓存后返回
        TodayScheduleVO vo = new TodayScheduleVO(dateStr, items.size(), items);
        dashboardCache.cacheTodaySchedule(today, vo);
        return vo;
    }

    /**
     * 批量查询多场次的主裁判姓名（每个场次取一名主裁判）。
     * <p>
     * 两步查询：refereeAssignmentMapper 取 role=1 主裁判指派 → refereeMapper 批量取姓名，
     * Java 层组装 competitionId → refereeName。等价 {@link CompetitionDAO} 同名私有方法。
     * </p>
     *
     * @param competitionIds 场次ID集合
     * @return {@code competitionId → 主裁判姓名} 映射，无主裁判的场次不在 Map 中
     */
    private Map<Long, String> batchQueryChiefRefereeNames(List<Long> competitionIds) {
        if (competitionIds == null || competitionIds.isEmpty()) {
            return Collections.emptyMap();
        }
        List<RefereeAssignment> assignments = refereeAssignmentMapper.selectList(
                new LambdaQueryWrapper<RefereeAssignment>()
                        .in(RefereeAssignment::getCompetitionId, competitionIds)
                        .eq(RefereeAssignment::getRole, CompetitionConstants.REFEREE_ROLE_CHIEF)
        );
        if (assignments.isEmpty()) {
            return Collections.emptyMap();
        }
        List<Long> refereeIds = assignments.stream()
                .map(RefereeAssignment::getRefereeId).filter(Objects::nonNull)
                .distinct().collect(Collectors.toList());
        Map<Long, String> refereeIdToName = refereeIds.isEmpty()
                ? Collections.emptyMap()
                : refereeMapper.selectBatchIds(refereeIds).stream()
                        .collect(Collectors.toMap(Referee::getId, Referee::getName, (a, b) -> a));
        Map<Long, String> result = new HashMap<>();
        for (RefereeAssignment ra : assignments) {
            String name = refereeIdToName.get(ra.getRefereeId());
            if (name != null && !result.containsKey(ra.getCompetitionId())) {
                result.put(ra.getCompetitionId(), name);
            }
        }
        return result;
    }

    /**
     * 批量统计多场次的参赛人数（个人 COUNT + 团体 SUM(participant_count)）。
     * <p>
     * 复用 {@link CompetitionMapper#batchCountIndividualParticipants(List)} 与
     * {@link CompetitionMapper#batchSumTeamParticipants(List)} 两个 XML 方法，
     * 等价 {@link CompetitionDAO#batchQueryParticipantCounts(List)} 私有方法。
     * </p>
     *
     * @param competitionIds 场次ID集合
     * @return {@code competitionId → 参赛人数} 映射，无记录的场次值为 0
     */
    private Map<Long, Long> batchQueryParticipantCounts(List<Long> competitionIds) {
        Map<Long, Long> result = new HashMap<>();
        if (competitionIds == null || competitionIds.isEmpty()) {
            return result;
        }
        competitionIds.forEach(id -> result.put(id, 0L));
        // 个人参赛数：XML 中 GROUP BY COUNT（已显式 deleted=0）
        List<Map<String, Object>> individualRows = competitionMapper.batchCountIndividualParticipants(competitionIds);
        if (individualRows != null) {
            for (Map<String, Object> row : individualRows) {
                Long competitionId = ((Number) row.get("competition_id")).longValue();
                Long cnt = ((Number) row.get("cnt")).longValue();
                result.merge(competitionId, cnt, Long::sum);
            }
        }
        // 团体参赛数：XML 中 GROUP BY SUM(participant_count)（已显式 deleted=0）
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

    // ==================== 破纪录汇总 ====================

    /**
     * 查询破纪录汇总聚合数据（cache-aside）。
     * <p>
     * 流程：
     * <ol>
     *   <li>先查 {@link DashboardCache#getBreakingRecords()}，命中直接返回；</li>
     *   <li>未命中则查所有 {@code status=ACTIVE} 的有效纪录，按 recordDate DESC、id DESC 排序
     *       （Option A：本届运动会语义，不引入"本届"日期过滤，所有 status=1 即当前届有效纪录）；</li>
     *   <li>空列表短路：组装空 VO 回写缓存返回；</li>
     *   <li>批量查询前驱旧记录（status=INVALID）按 (eventId, recordType) 分组取 recordDate+id DESC 首条，
     *       避免 N+1；in 条件可能匹配无关 (eventId, recordType) 组合，内存 reduce 过滤；</li>
     *   <li>批量查询项目名（eventMapper.selectBatchIds）；</li>
     *   <li>组装 {@link BreakingRecordItemVO} 列表（保持 active 列表顺序）后回写缓存返回。</li>
     * </ol>
     * </p>
     *
     * @return 破纪录汇总聚合 VO
     */
    public BreakingRecordsVO getBreakingRecordsData() {
        // 1. 先查缓存
        BreakingRecordsVO cached = dashboardCache.getBreakingRecords();
        if (cached != null) {
            return cached;
        }

        // 2. 查所有 status=ACTIVE 有效纪录，按 recordDate DESC、id DESC
        List<Record> records = recordMapper.selectList(
                new LambdaQueryWrapper<Record>()
                        .eq(Record::getStatus, RecordConstants.STATUS_ACTIVE)
                        .orderByDesc(Record::getRecordDate)
                        .orderByDesc(Record::getId)
        );

        // 3. 空列表短路
        if (records.isEmpty()) {
            BreakingRecordsVO empty = new BreakingRecordsVO(0, Collections.emptyList());
            dashboardCache.cacheBreakingRecords(empty);
            return empty;
        }

        // 4. 收集 active 列表涉及的 (eventId, recordType) 组合
        List<Long> eventIds = records.stream()
                .map(Record::getEventId).filter(Objects::nonNull)
                .distinct().collect(Collectors.toList());
        List<Integer> recordTypes = records.stream()
                .map(Record::getRecordType).filter(Objects::nonNull)
                .distinct().collect(Collectors.toList());
        Set<String> activeKeys = records.stream()
                .filter(r -> r.getEventId() != null && r.getRecordType() != null)
                .map(r -> r.getEventId() + ":" + r.getRecordType())
                .collect(Collectors.toSet());

        // 5. 批量查前驱旧记录（status=INVALID），按 (eventId, recordType) 分组取首条
        Map<String, Record> prevMap = batchQueryPreviousRecords(eventIds, recordTypes, activeKeys);

        // 6. 批量查项目名
        Map<Long, String> eventIdToName = eventIds.isEmpty()
                ? Collections.emptyMap()
                : eventMapper.selectBatchIds(eventIds).stream()
                        .collect(Collectors.toMap(Event::getId, Event::getName, (a, b) -> a));

        // 7. 组装 VO 列表（保持 active 列表顺序）
        List<BreakingRecordItemVO> items = new ArrayList<>(records.size());
        for (Record rec : records) {
            Record prev = prevMap.get(rec.getEventId() + ":" + rec.getRecordType());
            items.add(new BreakingRecordItemVO(
                    rec.getId(),
                    rec.getEventId() == null ? null : eventIdToName.get(rec.getEventId()),
                    rec.getAthleteName(),
                    rec.getCollegeName(),
                    rec.getScoreDisplay(),
                    prev == null ? null : prev.getScoreDisplay(),
                    DashboardConstants.RECORD_TYPE_CN.get(rec.getRecordType()),
                    rec.getRecordDate() == null ? null : rec.getRecordDate().format(DashboardConstants.DATE_FMT)
            ));
        }

        // 8. 组装 VO 并回写缓存后返回
        BreakingRecordsVO vo = new BreakingRecordsVO(items.size(), items);
        dashboardCache.cacheBreakingRecords(vo);
        return vo;
    }

    /**
     * 批量查询前驱旧记录（status=INVALID），按 (eventId, recordType) 分组取 recordDate+id DESC 首条。
     * <p>
     * SQL 的 in(eventIds) × in(recordTypes) 可能匹配无关的 (eventId, recordType) 组合
     * （如 (e1, t2) 不在 active 集合但被查出），故用 {@code activeKeys} 在内存 reduce 过滤。
     * 平局规则：recordDate DESC, id DESC 取首条。
     * </p>
     *
     * @param eventIds    active 列表涉及的 eventId 集合
     * @param recordTypes active 列表涉及的 recordType 集合
     * @param activeKeys  合法 (eventId:recordType) 字符串集合，用于内存过滤
     * @return {@code "eventId:recordType" → 前驱 Record} 映射，无前驱的组合不在 Map 中
     */
    private Map<String, Record> batchQueryPreviousRecords(List<Long> eventIds,
                                                          List<Integer> recordTypes,
                                                          Set<String> activeKeys) {
        if (eventIds.isEmpty() || recordTypes.isEmpty()) {
            return Collections.emptyMap();
        }
        List<Record> candidates = recordMapper.selectList(
                new LambdaQueryWrapper<Record>()
                        .eq(Record::getStatus, RecordConstants.STATUS_INVALID)
                        .in(Record::getEventId, eventIds)
                        .in(Record::getRecordType, recordTypes)
        );
        Map<String, Record> prevMap = new HashMap<>();
        candidates.stream()
                .filter(r -> r.getEventId() != null && r.getRecordType() != null
                        && activeKeys.contains(r.getEventId() + ":" + r.getRecordType()))
                .collect(Collectors.groupingBy(r -> r.getEventId() + ":" + r.getRecordType()))
                .forEach((k, list) -> list.stream()
                        .max(Comparator.comparing(Record::getRecordDate,
                                        Comparator.nullsFirst(Comparator.naturalOrder()))
                                .thenComparing(Record::getId))
                        .ifPresent(r -> prevMap.put(k, r)));
        return prevMap;
    }
}