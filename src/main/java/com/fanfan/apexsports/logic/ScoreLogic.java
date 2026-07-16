package com.fanfan.apexsports.logic;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.fanfan.apexsports.config.UserContext;
import com.fanfan.apexsports.constants.CompetitionConstants;
import com.fanfan.apexsports.constants.EventConstants;
import com.fanfan.apexsports.constants.ScoreConstants;
import com.fanfan.apexsports.constants.UserConstants;
import com.fanfan.apexsports.daos.AthleteDAO;
import com.fanfan.apexsports.daos.CollegeDAO;
import com.fanfan.apexsports.daos.CollegeScoreSummaryDAO;
import com.fanfan.apexsports.daos.CompetitionDAO;
import com.fanfan.apexsports.daos.EventDAO;
import com.fanfan.apexsports.daos.ParticipationDAO;
import com.fanfan.apexsports.daos.RecordDAO;
import com.fanfan.apexsports.daos.ScoreDAO;
import com.fanfan.apexsports.daos.TeamParticipationDAO;
import com.fanfan.apexsports.dtos.BatchScoreDTO;
import com.fanfan.apexsports.dtos.BatchTeamScoreDTO;
import com.fanfan.apexsports.dtos.ScoreItemDTO;
import com.fanfan.apexsports.dtos.ScoreQueryDTO;
import com.fanfan.apexsports.dtos.ScoreUpdateDTO;
import com.fanfan.apexsports.dtos.SingleScoreDTO;
import com.fanfan.apexsports.dtos.TeamScoreItemDTO;
import com.fanfan.apexsports.entities.Athlete;
import com.fanfan.apexsports.entities.College;
import com.fanfan.apexsports.entities.Competition;
import com.fanfan.apexsports.entities.Event;
import com.fanfan.apexsports.entities.Participation;
import com.fanfan.apexsports.entities.Record;
import com.fanfan.apexsports.entities.Score;
import com.fanfan.apexsports.entities.TeamParticipation;
import com.fanfan.apexsports.exception.BusinessException;
import com.fanfan.apexsports.exception.ErrorCode;
import com.fanfan.apexsports.services.ScoreService;
import com.fanfan.apexsports.vos.CompetitionScoreVO;
import com.fanfan.apexsports.vos.ScoreDetailVO;
import com.fanfan.apexsports.vos.ScoreEntryResultVO;
import com.fanfan.apexsports.vos.ScoreItemVO;
import com.fanfan.apexsports.vos.ScoreVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 成绩模块业务实现（Todo 9 + Todo 11）。
 * <p>
 * 实现个人/团体成绩录入（含破纪录判定、积分联动、终态拦截与重复校验）三类写操作
 * （Todo 9: 71/72/73），以及成绩分页查询、单条修改、删除与比赛场次汇总共四类
 * 查询/维护操作（Todo 11: 74/75/76/77）。
 * </p>
 * <p>
 * <b>破纪录判定：</b>仅校记录（recordType=1）。径赛（category=1）取 score 升序首条作为最优，
 * 田赛（category=2）取 score 降序首条作为最优。无校记录时视为破纪录（首次创建校记录）；
 * 破纪录时插入新 Record、作废旧 Record（as_score 表无 record_id 列，破纪录副作用全部在校记录表内闭环）。
 * </p>
 * <p>
 * <b>积分联动：</b>积分 delta 由 Logic 算好传入 DAO，按学院维度 upsert 到积分汇总行；
 * 个人成绩累加到 individualPoints，团体成绩累加到 teamPoints；
 * rank ∈ {1,2,3} 时分别累加金/银/铜牌计数。
 * </p>
 * <p>
 * <b>架构红线：</b>Logic 层不直接调用 Mapper（通过 DAO），不直接操作缓存（通过 Cache 层），
 * 不出现缓存/存储层相关字样。DO 不得出现在方法返回值，统一转换为 VO。
 * </p>
 *
 * @author fanfan187
 * @version v1.0.0
 * @since v1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(rollbackFor = Exception.class)
public class ScoreLogic implements ScoreService {

    // ==================== 依赖注入 ====================

    private final ScoreDAO scoreDAO;
    private final RecordDAO recordDAO;
    private final CollegeScoreSummaryDAO collegeScoreSummaryDAO;
    private final CompetitionDAO competitionDAO;
    private final EventDAO eventDAO;
    private final ParticipationDAO participationDAO;
    private final TeamParticipationDAO teamParticipationDAO;
    private final AthleteDAO athleteDAO;
    private final CollegeDAO collegeDAO;

    // ==================== 71. 批量录入个人成绩 ====================

    /**
     * {@inheritDoc}
     * <p>
     * 校验链：管理员权限 → 比赛存在 → 比赛未结束 → 逐条处理（重复校验 → 查参赛记录 → 查项目 →
     * 算积分 → 判破纪录 → 写 Score → 积分联动）。
     * </p>
     */
    @Override
    public ScoreEntryResultVO batchAddScores(BatchScoreDTO dto) {
        checkAdmin();

        Competition competition = getCompetitionOrThrow(dto.getCompetitionId());
        ensureCompetitionNotFinished(competition);

        boolean anyRecordBroken = false;
        int successCount = 0;
        for (ScoreItemDTO item : dto.getItems()) {
            if (processIndividualItem(competition, item.getParticipationId(),
                    item.getRank(), null, item.getScoreValue(), item.getScoreDisplay())) {
                anyRecordBroken = true;
            }
            successCount++;
        }
        log.info("批量录入个人成绩: competitionId={}, successCount={}, recordBroken={}",
                dto.getCompetitionId(), successCount, anyRecordBroken);
        return new ScoreEntryResultVO(anyRecordBroken, successCount);
    }

    // ==================== 72. 录入单条个人成绩 ====================

    /**
     * {@inheritDoc}
     * <p>
     * 复用 {@link #processIndividualItem} 的单条处理逻辑，传入 dto.points 作为积分覆盖（非空时生效）。
     * </p>
     */
    @Override
    public ScoreEntryResultVO addScore(SingleScoreDTO dto, Long competitionId) {
        checkAdmin();

        Competition competition = getCompetitionOrThrow(competitionId);
        ensureCompetitionNotFinished(competition);

        boolean broken = processIndividualItem(competition, dto.getParticipationId(),
                dto.getRank(), dto.getPoints(), dto.getScoreValue(), dto.getScoreDisplay());
        log.info("录入单条个人成绩: competitionId={}, participationId={}, recordBroken={}",
                competitionId, dto.getParticipationId(), broken);
        return new ScoreEntryResultVO(broken, 1);
    }

    // ==================== 73. 批量录入团体成绩 ====================

    /**
     * {@inheritDoc}
     * <p>
     * 校验链同 {@link #batchAddScores}，差异：使用 TeamParticipationDAO 校验唯一性、按学院维度判破纪录
     * （Record.athleteId 置 null，Record.collegeId 取 TeamParticipation.collegeId）、积分联动 isTeam=true。
     * </p>
     */
    @Override
    public ScoreEntryResultVO batchAddTeamScores(BatchTeamScoreDTO dto) {
        checkAdmin();

        Competition competition = getCompetitionOrThrow(dto.getCompetitionId());
        ensureCompetitionNotFinished(competition);

        boolean anyRecordBroken = false;
        int successCount = 0;
        for (TeamScoreItemDTO item : dto.getItems()) {
            if (processTeamItem(competition, item.getTeamParticipationId(),
                    item.getRank(), item.getScoreValue(), item.getScoreDisplay())) {
                anyRecordBroken = true;
            }
            successCount++;
        }
        log.info("批量录入团体成绩: competitionId={}, successCount={}, recordBroken={}",
                dto.getCompetitionId(), successCount, anyRecordBroken);
        return new ScoreEntryResultVO(anyRecordBroken, successCount);
    }

    // ==================== 以下 4 方法为查询/修改/删除/汇总实现 ====================

    /**
     * {@inheritDoc}
     * <p>
     * 委托 {@link ScoreDAO#findByPage} 执行 5 表 LEFT JOIN 分页查询，结果已为
     * {@link ScoreDetailVO}（含 athleteName/collegeName/eventName/competitionName），
     * Logic 层透传即可，无需二次组装。分页参数缺省由 DAO 补齐 1/10。
     * </p>
     */
    @Override
    public IPage<ScoreDetailVO> getScorePage(ScoreQueryDTO query) {
        return scoreDAO.findByPage(query);
    }

    /**
     * {@inheritDoc}
     * <p>
     * 校验链：管理员 → 成绩存在 → 比赛未结束 → 算旧/新 delta → 积分回滚+重算 →
     * 作废旧破纪录 → 部分更新 Score → 按新 score 重新判破纪录 → 返回更新后 VO。
     * </p>
     * <p>
     * <b>积分重算策略：</b>先按旧 points/rank 回滚学院汇总增量，再按新 points/rank
     * 重新 upsert 增量，避免直接差值计算的边界问题（如学院汇总行可能已不存在）。
     * </p>
     * <p>
     * <b>破纪录重判：</b>无论 score 是否变更均重新判定：先作废当前最优 Record（as_score 无 record_id 列，
     * 按 eventId 维度当前最优定位），再用 post-update score 调 {@link #isRecordBroken} +
     * {@link #insertNewRecordAndInvalidateOld}。若 score 未变但旧 Record 已作废，重判会基于次优记录重新比较，语义自洽。
     * </p>
     */
    @Override
    public ScoreVO updateScore(ScoreUpdateDTO dto) {
        checkAdmin();

        // 1. 成绩存在校验
        Score existing = scoreDAO.getById(dto.getId());
        if (existing == null) {
            throw new BusinessException(ErrorCode.SCORE_NOT_FOUND);
        }

        // 2. 终态拦截（已结束/已取消不可修改）
        Competition competition = getCompetitionOrThrow(existing.getCompetitionId());
        ensureCompetitionNotFinished(competition);

        // 3. 取项目（as_score 无 event_id 列，项目维度走 competition）
        Event event = competition.getEventId() == null
                ? null
                : eventDAO.getById(competition.getEventId());
        if (event == null) {
            throw new BusinessException(ErrorCode.EVENT_NOT_FOUND);
        }

        // 4. 算旧 delta（基于 existing）
        int oldPoints = existing.getPoints() != null ? existing.getPoints() : 0;
        int oldRank = existing.getRank() != null ? existing.getRank() : 0;

        // 5. 算新 delta（nullable 语义：null=沿用旧值或自动重算）
        Integer newRank = dto.getRank() != null ? dto.getRank() : oldRank;
        int newPoints = dto.getPoints() != null
                ? dto.getPoints()
                : ScoreConstants.getPoints(newRank);

        // 6. 积分回滚 + 重算（isTeam 由 participationId/teamParticipationId 互斥决定）
        boolean isTeam = existing.getTeamParticipationId() != null;
        rollbackScoreDelta(existing.getCollegeId(), oldPoints, oldRank, isTeam);
        applyCollegeScoreDelta(existing.getCollegeId(), newPoints, newRank, isTeam);

        // 7. 若旧成绩破纪录 → 作废对应最优 Record
        //    （as_score 无 record_id 列，按 eventId 维度当前最优定位作废）
        if (existing.getIsRecordBreaking() != null
                && existing.getIsRecordBreaking() == 1) {
            Record priorBest = recordDAO.getBestRecord(event.getId(),
                    ScoreConstants.RECORD_TYPE_SCHOOL, directionFor(event));
            if (priorBest != null) {
                recordDAO.invalidateRecord(priorBest.getId());
            }
        }

        // 8. 组装 patch（rank/score/scoreDisplay 按 Objects.nonNull 判定，points 始终刷新为新值）
        BigDecimal effectiveScoreValue = dto.getScoreValue() != null
                ? dto.getScoreValue()
                : existing.getScore();
        String effectiveScoreDisplay = dto.getScoreDisplay() != null
                ? dto.getScoreDisplay()
                : existing.getScoreDisplay();

        Score patch = new Score();
        patch.setId(existing.getId());
        if (Objects.nonNull(dto.getRank())) {
            patch.setRank(dto.getRank());
        }
        if (Objects.nonNull(dto.getScoreValue())) {
            patch.setScore(dto.getScoreValue());
        }
        if (Objects.nonNull(dto.getScoreDisplay())) {
            patch.setScoreDisplay(dto.getScoreDisplay());
        }
        patch.setPoints(newPoints);

        // 9. 重新判破纪录（用 post-update score + 项目方向）
        if (isRecordBroken(event, effectiveScoreValue)) {
            insertNewRecordAndInvalidateOld(event, effectiveScoreValue,
                    effectiveScoreDisplay, existing.getAthleteId(),
                    existing.getCollegeId(), null);
            patch.setIsRecordBreaking(1);
        } else {
            patch.setIsRecordBreaking(0);
        }

        // 10. 落库 + 转 VO
        Score updated = scoreDAO.update(patch);
        log.info("修改成绩: id={}, newRank={}, newPoints={}, recordBroken={}",
                updated.getId(), newRank, newPoints, updated.getIsRecordBreaking());
        return toScoreVO(updated);
    }

    /**
     * {@inheritDoc}
     * <p>
     * 校验链：管理员 → 成绩存在 → 比赛未结束 → 积分回滚 → 作废破纪录 → 逻辑删除。
     * </p>
     */
    @Override
    public void deleteScoreById(Long id) {
        checkAdmin();

        Score existing = scoreDAO.getById(id);
        if (existing == null) {
            throw new BusinessException(ErrorCode.SCORE_NOT_FOUND);
        }

        Competition competition = getCompetitionOrThrow(existing.getCompetitionId());
        ensureCompetitionNotFinished(competition);

        // 积分回滚（基于 existing 的 points/rank）
        boolean isTeam = existing.getTeamParticipationId() != null;
        int oldPoints = existing.getPoints() != null ? existing.getPoints() : 0;
        int oldRank = existing.getRank() != null ? existing.getRank() : 0;
        rollbackScoreDelta(existing.getCollegeId(), oldPoints, oldRank, isTeam);

        // 破纪录联动：作废对应最优 Record
        // （as_score 无 record_id 列，按 eventId 维度当前最优定位作废；项目维度走 competition）
        if (existing.getIsRecordBreaking() != null
                && existing.getIsRecordBreaking() == 1
                && competition.getEventId() != null) {
            Event event = eventDAO.getById(competition.getEventId());
            if (event != null) {
                Record priorBest = recordDAO.getBestRecord(event.getId(),
                        ScoreConstants.RECORD_TYPE_SCHOOL, directionFor(event));
                if (priorBest != null) {
                    recordDAO.invalidateRecord(priorBest.getId());
                }
            }
        }

        scoreDAO.deleteById(id);
        log.info("删除成绩: id={}, competitionId={}, collegeId={}",
                id, existing.getCompetitionId(), existing.getCollegeId());
    }

    /**
     * {@inheritDoc}
     * <p>
     * 比赛存在校验 → 拉取该场次全部成绩 → 按 rank 升序排序 → 批量查 athlete/college/event
     * 名称组装为 {@link ScoreItemVO} → 统计破纪录总数 → 组装 {@link CompetitionScoreVO}。
     * </p>
     * <p>
     * <b>N+1 规避：</b>athlete/college 用 distinct id 集合做批量查询，转 Map 后回填；
     * event/competition 单条查询（场次维度仅一个 event + 一个 competition）。
     * </p>
     */
    @Override
    public CompetitionScoreVO getCompetitionScores(Long competitionId) {
        Competition competition = competitionDAO.getById(competitionId);
        if (competition == null) {
            throw new BusinessException(ErrorCode.COMPETITION_NOT_FOUND);
        }

        List<Score> scores = scoreDAO.findByCompetitionId(competitionId);
        // 按 rank 升序（null 视为最大值排末尾）
        scores.sort(Comparator.comparingInt(
                s -> s.getRank() == null ? Integer.MAX_VALUE : s.getRank()));

        // 收集 distinct athleteId / collegeId
        Set<Long> athleteIds = new LinkedHashSet<>();
        Set<Long> collegeIds = new LinkedHashSet<>();
        for (Score s : scores) {
            if (s.getAthleteId() != null) {
                athleteIds.add(s.getAthleteId());
            }
            if (s.getCollegeId() != null) {
                collegeIds.add(s.getCollegeId());
            }
        }

        // 批量查 athlete → Map
        Map<Long, Athlete> athleteMap = athleteIds.isEmpty()
                ? Map.of()
                : athleteDAO.findByIds(athleteIds).stream()
                .collect(Collectors.toMap(Athlete::getId, Function.identity(), (a, b) -> a));
        // 批量查 college → Map（CollegeDAO 无 findByIds，按 distinct id 逐条查并合并）
        Map<Long, College> collegeMap = collegeIds.isEmpty()
                ? Map.of()
                : collegeIds.stream()
                .map(collegeDAO::getById)
                .filter(Objects::nonNull)
                .collect(Collectors.toMap(College::getId, Function.identity(), (a, b) -> a));

        // 查 event 名称（场次维度仅一个 eventId）
        Event event = competition.getEventId() == null
                ? null
                : eventDAO.getById(competition.getEventId());
        String eventName = event != null ? event.getName() : null;
        // competition 实体无 name 列，取 remark 作为可读标签（无 remark 则 null）
        String competitionName = competition.getRemark();

        // 组装 ScoreItemVO 列表 + 统计破纪录总数
        int totalRecords = 0;
        List<ScoreItemVO> rankings = new ArrayList<>(scores.size());
        for (Score s : scores) {
            String athleteName = s.getAthleteId() == null ? null
                    : (athleteMap.containsKey(s.getAthleteId())
                            ? athleteMap.get(s.getAthleteId()).getName()
                            : null);
            String collegeName = s.getCollegeId() == null ? null
                    : (collegeMap.containsKey(s.getCollegeId())
                            ? collegeMap.get(s.getCollegeId()).getName()
                            : null);
            if (s.getIsRecordBreaking() != null && s.getIsRecordBreaking() == 1) {
                totalRecords++;
            }
            rankings.add(new ScoreItemVO(
                    s.getId(),
                    s.getParticipationId(),
                    s.getTeamParticipationId(),
                    athleteName,
                    collegeName,
                    s.getRank(),
                    s.getPoints(),
                    s.getScore(),
                    s.getScoreDisplay(),
                    s.getIsRecordBreaking()
            ));
        }

        return new CompetitionScoreVO(competitionId, competitionName, eventName,
                rankings, totalRecords);
    }

    // ==================== 私有辅助方法 ====================

    /**
     * 处理单条个人成绩条目（重复校验 → 查参赛记录 → 查运动员取学院 → 查项目 →
     * 算积分 → 判破纪录 → 写 Score → 积分联动）。
     * <p>
     * 个人成绩的学院 ID 取自运动员实体（Participation 表无 collegeId 字段）。
     * </p>
     *
     * @param competition     比赛场次实体（已校验未结束）
     * @param participationId 个人参赛记录ID
     * @param rank            名次
     * @param pointsOverride  积分覆盖值（null 时按 rank 自动计算）
     * @param scoreValue      成绩数值
     * @param scoreDisplay    成绩显示文本
     * @return 是否触发破纪录
     */
    private boolean processIndividualItem(Competition competition, Long participationId,
                                          Integer rank, Integer pointsOverride,
                                          BigDecimal scoreValue, String scoreDisplay) {
        // 1. 重复录入校验
        if (scoreDAO.existsByParticipationId(participationId)) {
            throw new BusinessException(ErrorCode.SCORE_ALREADY_EXISTS);
        }

        // 2. 查参赛记录
        Participation participation = participationDAO.getById(participationId);
        if (participation == null) {
            throw new BusinessException(ErrorCode.PARTICIPATION_NOT_FOUND);
        }

        // 3. 查运动员取学院ID（Participation 表无 collegeId 字段，需通过 athlete 反查）
        Athlete athlete = athleteDAO.getById(participation.getAthleteId());
        if (athlete == null) {
            throw new BusinessException(ErrorCode.ATHLETE_NOT_FOUND);
        }
        Long collegeId = athlete.getCollegeId();

        // 4. 查项目以判定成绩方向（径赛越小越优 / 田赛越大越优）
        Event event = eventDAO.getById(participation.getEventId());
        if (event == null) {
            throw new BusinessException(ErrorCode.EVENT_NOT_FOUND);
        }

        // 5. 算积分（pointsOverride 非空时覆盖）
        int points = pointsOverride != null ? pointsOverride : ScoreConstants.getPoints(rank);

        // 6. 判破纪录（as_score 无 record_id 列，破纪录副作用全部在校记录表内闭环）
        boolean broken = isRecordBroken(event, scoreValue);
        if (broken) {
            insertNewRecordAndInvalidateOld(event, scoreValue, scoreDisplay,
                    participation.getAthleteId(), collegeId, null);
        }

        // 7. 写 Score
        Score score = new Score()
                .setParticipationId(participationId)
                .setTeamParticipationId(null)
                .setAthleteId(participation.getAthleteId())
                .setCollegeId(collegeId)
                .setCompetitionId(competition.getId())
                .setRank(rank)
                .setPoints(points)
                .setScore(scoreValue)
                .setScoreDisplay(scoreDisplay)
                .setIsRecordBreaking(broken ? 1 : 0);
        scoreDAO.insert(score);

        // 8. 积分联动（个人维度）
        applyCollegeScoreDelta(collegeId, points, rank, false);
        return broken;
    }

    /**
     * 处理单条团体成绩条目（重复校验 → 查团体参赛记录 → 查项目 → 算积分 → 判破纪录 →
     * 写 Score → 积分联动）。
     * <p>
     * 团体成绩的学院 ID 直接取自 TeamParticipation.collegeId，破纪录按学院维度
     * （Record.athleteId 置 null，Record.collegeId 取 TeamParticipation.collegeId）。
     * </p>
     *
     * @param competition         比赛场次实体（已校验未结束）
     * @param teamParticipationId 团体参赛记录ID
     * @param rank                名次
     * @param scoreValue          成绩数值
     * @param scoreDisplay        成绩显示文本
     * @return 是否触发破纪录
     */
    private boolean processTeamItem(Competition competition, Long teamParticipationId,
                                    Integer rank, BigDecimal scoreValue, String scoreDisplay) {
        // 1. 重复录入校验
        if (scoreDAO.existsByTeamParticipationId(teamParticipationId)) {
            throw new BusinessException(ErrorCode.TEAM_SCORE_ALREADY_EXISTS);
        }

        // 2. 查团体参赛记录
        TeamParticipation teamParticipation = teamParticipationDAO.getById(teamParticipationId);
        if (teamParticipation == null) {
            throw new BusinessException(ErrorCode.TEAM_PARTICIPATION_NOT_FOUND);
        }
        Long collegeId = teamParticipation.getCollegeId();

        // 3. 查项目以判定成绩方向
        Event event = eventDAO.getById(teamParticipation.getEventId());
        if (event == null) {
            throw new BusinessException(ErrorCode.EVENT_NOT_FOUND);
        }

        // 4. 算积分
        int points = ScoreConstants.getPoints(rank);

        // 5. 判破纪录（按学院维度，athleteId 置 null；as_score 无 record_id 列，副作用在校记录表内闭环）
        boolean broken = isRecordBroken(event, scoreValue);
        if (broken) {
            insertNewRecordAndInvalidateOld(event, scoreValue, scoreDisplay,
                    null, collegeId, null);
        }

        // 6. 写 Score
        Score score = new Score()
                .setParticipationId(null)
                .setTeamParticipationId(teamParticipationId)
                .setAthleteId(null)
                .setCollegeId(collegeId)
                .setCompetitionId(competition.getId())
                .setRank(rank)
                .setPoints(points)
                .setScore(scoreValue)
                .setScoreDisplay(scoreDisplay)
                .setIsRecordBreaking(broken ? 1 : 0);
        scoreDAO.insert(score);

        // 7. 积分联动（团体维度）
        applyCollegeScoreDelta(collegeId, points, rank, true);
        return broken;
    }

    /**
     * 判定本次成绩是否破校记录。
     * <p>
     * 径赛（category=1）：scoreValue 严格小于最优校记录即破纪录；
     * 田赛（category=2）：scoreValue 严格大于最优校记录即破纪录；
     * 无校记录时视为破纪录（首次创建校记录）。
     * </p>
     *
     * @param event      项目实体（取 category 决定方向）
     * @param scoreValue 本次成绩数值
     * @return true 表示破纪录
     */
    private boolean isRecordBroken(Event event, BigDecimal scoreValue) {
        int direction = directionFor(event);
        Record best = recordDAO.getBestRecord(event.getId(),
                ScoreConstants.RECORD_TYPE_SCHOOL, direction);
        if (best == null) {
            // 无校记录视为首次破纪录（后续将创建首条校记录）
            return true;
        }
        int cmp = scoreValue.compareTo(best.getScore());
        if (direction == ScoreConstants.SCORE_DIRECTION_FIELD) {
            return cmp > 0;
        }
        return cmp < 0;
    }

    /**
     * 根据项目类别映射成绩方向（径赛/田赛）。
     *
     * @param event 项目实体
     * @return {@link ScoreConstants#SCORE_DIRECTION_TRACK} 或 {@link ScoreConstants#SCORE_DIRECTION_FIELD}
     */
    private int directionFor(Event event) {
        if (event.getCategory() != null
                && event.getCategory() == EventConstants.CATEGORY_FIELD) {
            return ScoreConstants.SCORE_DIRECTION_FIELD;
        }
        return ScoreConstants.SCORE_DIRECTION_TRACK;
    }

    /**
     * 插入新 Record 并作废旧 Record，返回新 Record ID。
     * <p>
     * 个人成绩 athleteId 非空、collegeId 可空；团体成绩 athleteId 置 null、collegeId 非空。
     * 二者通过参数显式区分，本方法不区分逻辑。
     * </p>
     *
     * @param event         项目实体（取 eventId）
     * @param scoreValue    成绩数值
     * @param scoreDisplay  成绩显示
     * @param athleteId     运动员ID（团体成绩传 null）
     * @param collegeId     学院ID（个人成绩可由调用方传运动员学院）
     * @param competitionName 比赛名称（当前未取，传 null；如需冗余写入由后续 todo 补全）
     * @return 新插入的 Record ID
     */
    private Long insertNewRecordAndInvalidateOld(Event event, BigDecimal scoreValue,
                                                 String scoreDisplay, Long athleteId,
                                                 Long collegeId, String competitionName) {
        int direction = directionFor(event);
        Record bestExisting = recordDAO.getBestRecord(event.getId(),
                ScoreConstants.RECORD_TYPE_SCHOOL, direction);

        Record newRecord = new Record()
                .setEventId(event.getId())
                .setRecordType(ScoreConstants.RECORD_TYPE_SCHOOL)
                .setAthleteId(athleteId)
                .setCollegeId(collegeId)
                .setScore(scoreValue)
                .setScoreDisplay(scoreDisplay)
                .setRecordDate(LocalDate.now())
                .setCompetitionName(competitionName)
                .setStatus(1);
        Record inserted = recordDAO.insertRecord(newRecord);
        if (inserted == null || inserted.getId() == null) {
            throw new BusinessException(ErrorCode.SCORE_RECORD_OPERATION_FAILED);
        }
        if (bestExisting != null) {
            recordDAO.invalidateRecord(bestExisting.getId());
        }
        return inserted.getId();
    }

    /**
     * 应用学院积分增量到汇总行（含奖牌计数）。
     *
     * @param collegeId 学院ID
     * @param points    积分值
     * @param rank      名次（用于奖牌计数：1=金/2=银/3=铜）
     * @param isTeam    true=团体维度，false=个人维度
     */
    private void applyCollegeScoreDelta(Long collegeId, int points, int rank, boolean isTeam) {
        int deltaGold = (rank == 1) ? 1 : 0;
        int deltaSilver = (rank == 2) ? 1 : 0;
        int deltaBronze = (rank == 3) ? 1 : 0;
        collegeScoreSummaryDAO.upsertIncrement(collegeId, BigDecimal.valueOf(points),
                deltaGold, deltaSilver, deltaBronze, isTeam);
    }

    /**
     * 回滚学院积分增量（与 {@link #applyCollegeScoreDelta} 对称的反向操作）。
     * <p>
     * 用于成绩修改/删除前的积分回滚：将此前已 apply 的 delta 反向减回，
     * 委托 {@link CollegeScoreSummaryDAO#rollbackIncrement}（不存在汇总行则记 warn 跳过）。
     * </p>
     *
     * @param collegeId 学院ID
     * @param points    原积分值
     * @param rank      原名次（用于奖牌计数回滚：1=金/2=银/3=铜）
     * @param isTeam    true=团体维度，false=个人维度
     */
    private void rollbackScoreDelta(Long collegeId, int points, int rank, boolean isTeam) {
        int deltaGold = (rank == 1) ? 1 : 0;
        int deltaSilver = (rank == 2) ? 1 : 0;
        int deltaBronze = (rank == 3) ? 1 : 0;
        collegeScoreSummaryDAO.rollbackIncrement(collegeId, BigDecimal.valueOf(points),
                deltaGold, deltaSilver, deltaBronze, isTeam);
    }

    /**
     * 将更新后的 Score DO 转换为 {@link ScoreVO}（含 athleteName/collegeName/eventName/competitionName
     * 关联字段查询）。
     * <p>
     * 单条查询场景：athlete/college/event/competition 各做一次 getById。批量场景（如
     * {@link #getCompetitionScores}）应在调用方做批量查询并自行组装，避免 N+1。
     * </p>
     *
     * @param score 已更新的成绩实体
     * @return 成绩 VO
     */
    private ScoreVO toScoreVO(Score score) {
        if (score == null) {
            return null;
        }
        String athleteName = score.getAthleteId() == null ? null
                : fetchNameById(score.getAthleteId(), athleteDAO::getById, Athlete::getName);
        String collegeName = score.getCollegeId() == null ? null
                : fetchNameById(score.getCollegeId(), collegeDAO::getById, College::getName);
        // 项目/场次名称都通过 competition 反查（as_score 无 event_id 列）
        String eventName = null;
        String competitionName = null;
        if (score.getCompetitionId() != null) {
            Competition competition = competitionDAO.getById(score.getCompetitionId());
            if (competition != null) {
                competitionName = competition.getRemark();
                if (competition.getEventId() != null) {
                    Event event = eventDAO.getById(competition.getEventId());
                    eventName = event != null ? event.getName() : null;
                }
            }
        }
        return new ScoreVO(
                score.getId(),
                score.getParticipationId(),
                score.getTeamParticipationId(),
                athleteName,
                collegeName,
                eventName,
                competitionName,
                score.getRank(),
                score.getPoints(),
                score.getScore(),
                score.getScoreDisplay(),
                score.getIsRecordBreaking(),
                score.getCreatedAt()
        );
    }

    /**
     * 通过 id 查询实体并提取名称字段的小工具，避免 N+1 内联样板。
     *
     * @param id       实体ID
     * @param fetcher  按 id 查询实体的函数
     * @param nameExtractor 从实体提取名称的函数
     * @param <T>       实体类型
     * @return 名称；实体为 null 时返回 null
     */
    private <T> String fetchNameById(Long id, Function<Long, T> fetcher,
                                     Function<T, String> nameExtractor) {
        T entity = fetcher.apply(id);
        return entity == null ? null : nameExtractor.apply(entity);
    }

    /**
     * 校验比赛场次未处于终态（已结束/已取消）。
     *
     * @param competition 比赛场次实体
     */
    private void ensureCompetitionNotFinished(Competition competition) {
        if (competition.getStatus() == CompetitionConstants.STATUS_FINISHED
                || competition.getStatus() == CompetitionConstants.STATUS_CANCELLED) {
            throw new BusinessException(ErrorCode.COMPETITION_FINISHED);
        }
    }

    /**
     * 获取比赛场次，不存在则抛异常。
     *
     * @param id 比赛场次ID
     * @return 比赛场次实体
     */
    private Competition getCompetitionOrThrow(Long id) {
        Competition competition = competitionDAO.getById(id);
        if (competition == null) {
            throw new BusinessException(ErrorCode.COMPETITION_NOT_FOUND);
        }
        return competition;
    }

    /**
     * 检查当前用户是否为管理员。
     * <p>
     * 非管理员抛出 {@link BusinessException}（FORBIDDEN）。
     * </p>
     */
    private void checkAdmin() {
        Integer role = UserContext.getRole();
        if (role == null || UserConstants.ROLE_ADMIN != role) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
    }
}
