package com.fanfan.apexsports.logic;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fanfan.apexsports.common.result.PageResult;
import com.fanfan.apexsports.config.UserContext;
import com.fanfan.apexsports.constants.CompetitionConstants;
import com.fanfan.apexsports.constants.EventConstants;
import com.fanfan.apexsports.constants.UserConstants;
import com.fanfan.apexsports.daos.AthleteDAO;
import com.fanfan.apexsports.daos.CollegeDAO;
import com.fanfan.apexsports.daos.CompetitionDAO;
import com.fanfan.apexsports.daos.CompetitionEquipmentDAO;
import com.fanfan.apexsports.daos.EquipmentDAO;
import com.fanfan.apexsports.daos.EventDAO;
import com.fanfan.apexsports.daos.ParticipationDAO;
import com.fanfan.apexsports.daos.RefereeAssignmentDAO;
import com.fanfan.apexsports.daos.RefereeDAO;
import com.fanfan.apexsports.daos.ScoreDAO;
import com.fanfan.apexsports.daos.TeamParticipationDAO;
import com.fanfan.apexsports.daos.VenueDAO;
import com.fanfan.apexsports.dtos.BatchArrangeDTO;
import com.fanfan.apexsports.dtos.BatchTeamArrangeDTO;
import com.fanfan.apexsports.dtos.CompetitionCreateDTO;
import com.fanfan.apexsports.dtos.CompetitionQueryDTO;
import com.fanfan.apexsports.dtos.CompetitionUpdateDTO;
import com.fanfan.apexsports.dtos.UpdateCompetitionStatusDTO;
import com.fanfan.apexsports.entities.Athlete;
import com.fanfan.apexsports.entities.College;
import com.fanfan.apexsports.entities.Competition;
import com.fanfan.apexsports.entities.CompetitionEquipment;
import com.fanfan.apexsports.entities.Equipment;
import com.fanfan.apexsports.entities.Event;
import com.fanfan.apexsports.entities.Participation;
import com.fanfan.apexsports.entities.Referee;
import com.fanfan.apexsports.entities.RefereeAssignment;
import com.fanfan.apexsports.entities.TeamParticipation;
import com.fanfan.apexsports.entities.Venue;
import com.fanfan.apexsports.exception.BusinessException;
import com.fanfan.apexsports.exception.ErrorCode;
import com.fanfan.apexsports.services.CompetitionService;
import com.fanfan.apexsports.vos.CompetitionDetailVO;
import com.fanfan.apexsports.vos.CompetitionEquipmentVO;
import com.fanfan.apexsports.vos.CompetitionVO;
import com.fanfan.apexsports.vos.ParticipantArrangementVO;
import com.fanfan.apexsports.vos.RefereeAssignmentVO;
import com.fanfan.apexsports.vos.ScheduleBookletVO;
import com.fanfan.apexsports.vos.TeamParticipantArrangementVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 比赛场次业务实现（第一部分：场次 CRUD + 状态流转 + 秩序册）
 * <p>
 * 实现比赛场次的创建（含径赛互斥与场地冲突检测）、分页查询、详情聚合查询、
 * 修改（部分更新、终态拦截、冲突重检）、删除（状态校验、级联清理裁判分配与器材归还）、
 * 状态流转（正向合法性校验）以及秩序册聚合查询等业务逻辑。
 * 所有写操作均需要管理员权限，读操作对所有角色开放。
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
public class CompetitionLogic implements CompetitionService {

    // ==================== 依赖注入 ====================

    private final CompetitionDAO competitionDAO;
    private final EventDAO eventDAO;
    private final VenueDAO venueDAO;
    private final RefereeAssignmentDAO refereeAssignmentDAO;
    private final CompetitionEquipmentDAO competitionEquipmentDAO;
    private final ParticipationDAO participationDAO;
    private final TeamParticipationDAO teamParticipationDAO;
    private final RefereeDAO refereeDAO;
    private final ScoreDAO scoreDAO;
    private final AthleteDAO athleteDAO;
    private final CollegeDAO collegeDAO;
    private final EquipmentDAO equipmentDAO;

    // ==================== 接口方法实现 ====================

    /**
     * {@inheritDoc}
     * <p>
     * 校验链：管理员权限 → 时间合法性 → 项目存在且启用 → 场地存在且可用 →
     * 径赛互斥检测 → 场地冲突检测 → 构建实体并插入 → 重新查库构建 VO。
     * </p>
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public CompetitionVO create(CompetitionCreateDTO dto) {
        checkAdmin();

        // 校验 1：startTime 必须早于 endTime
        LocalDateTime startTime = parseDateTime(dto.getStartTime());
        LocalDateTime endTime = parseDateTime(dto.getEndTime());
        if (!startTime.isBefore(endTime)) {
            throw new BusinessException(ErrorCode.PARAM_VALIDATION_FAILED);
        }

        // 校验 2：项目存在且启用
        Event event = eventDAO.getById(dto.getEventId());
        if (event == null) {
            throw new BusinessException(ErrorCode.EVENT_NOT_FOUND);
        }
        if (EventConstants.STATUS_ENABLED != event.getStatus()) {
            throw new BusinessException(ErrorCode.EVENT_DISABLED);
        }

        // 校验 3：场地存在且可用
        Venue venue = venueDAO.getById(dto.getVenueId());
        if (venue == null) {
            throw new BusinessException(ErrorCode.VENUE_NOT_FOUND);
        }
        if (EventConstants.STATUS_ENABLED != venue.getStatus()) {
            throw new BusinessException(ErrorCode.VENUE_NOT_FOUND);
        }

        // 校验 4：径赛互斥检测（category=1 径赛时，同时间段不得有其他径赛场次）
        if (EventConstants.CATEGORY_TRACK == event.getCategory()) {
            Long trackConflicts = competitionDAO.countTrackCompetitionsInTimeRange(startTime, endTime, null);
            if (trackConflicts != null && trackConflicts > 0) {
                throw new BusinessException(ErrorCode.TRACK_TIME_CONFLICT);
            }
        }

        // 校验 5：场地冲突检测（同场地同时间段不得有其他场次）
        Long venueConflicts = competitionDAO.countVenueConflicts(dto.getVenueId(), startTime, endTime, null);
        if (venueConflicts != null && venueConflicts > 0) {
            throw new BusinessException(ErrorCode.VENUE_TIME_CONFLICT);
        }

        // 构建实体（DAO 层会自动填充 status 默认值、groupNo/round 默认值、审计字段、逻辑删除标记）
        Competition competition = new Competition()
                .setEventId(dto.getEventId())
                .setVenueId(dto.getVenueId())
                .setRound(dto.getRound())
                .setGroupNo(dto.getGroupNo() != null ? dto.getGroupNo() : CompetitionConstants.DEFAULT_GROUP_NO)
                .setStartTime(startTime)
                .setEndTime(endTime)
                .setRemark(dto.getRemark())
                .setStatus(CompetitionConstants.DEFAULT_STATUS);

        Competition inserted = competitionDAO.insert(competition);
        // insert 内部已驱逐所有秩序册缓存，此处无需重复调用
        log.info("创建比赛场次: id={}, eventId={}, venueId={}", inserted.getId(), inserted.getEventId(), inserted.getVenueId());

        // 重新查库构建完整 VO（含 eventName/eventType/eventCategory/venueName）
        return buildCompetitionVO(inserted, event, venue);
    }

    /**
     * {@inheritDoc}
     * <p>
     * 无需管理员权限（读操作开放）。批量关联项目与场地名称构建列表视图，避免 N+1 查询。
     * </p>
     */
    @Override
    public PageResult<CompetitionVO> page(CompetitionQueryDTO dto) {
        int pageNum = (dto.getCurrent() == null || dto.getCurrent() < 1) ? 1 : dto.getCurrent();
        int pageSize = (dto.getSize() == null || dto.getSize() < 1) ? 10 : Math.min(dto.getSize(), 100);

        Page<Competition> page = competitionDAO.page(pageNum, pageSize, dto.getDate(),
                dto.getVenueId(), dto.getEventId(), dto.getStatus(), dto.getRound());

        if (page.getRecords().isEmpty()) {
            return new PageResult<>(List.of(), page.getTotal(), page.getCurrent(), page.getSize(), page.getPages());
        }

        // 批量构建项目信息映射（name/type/category）
        Set<Long> eventIds = page.getRecords().stream()
                .map(Competition::getEventId)
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, Event> eventMap = eventDAO.findByIds(eventIds).stream()
                .collect(Collectors.toMap(Event::getId, e -> e, (a, b) -> a));

        // 批量构建场地名称映射（VenueDAO 无 findByIds，使用 findAll 全量构建 Map，场地表数据量小）
        Map<Long, String> venueNameMap = venueDAO.findAll(null, null).stream()
                .collect(Collectors.toMap(Venue::getId, Venue::getName, (a, b) -> a));

        List<CompetitionVO> records = page.getRecords().stream()
                .map(c -> {
                    Event event = c.getEventId() == null ? null : eventMap.get(c.getEventId());
                    String eventName = event != null ? event.getName() : null;
                    Integer eventType = event != null ? event.getType() : null;
                    Integer eventCategory = event != null ? event.getCategory() : null;
                    String venueName = c.getVenueId() == null ? null : venueNameMap.get(c.getVenueId());
                    return CompetitionVO.from(c, eventName, eventType, eventCategory, venueName);
                })
                .toList();

        return new PageResult<>(records, page.getTotal(), page.getCurrent(), page.getSize(), page.getPages());
    }

    /**
     * {@inheritDoc}
     * <p>
     * 无需管理员权限（读操作开放）。聚合比赛基本信息、裁判分配、器材分配、
     * 个人参赛者与团体参赛者列表，各子列表均通过批量查询关联名称避免 N+1。
     * </p>
     */
    @Override
    public CompetitionDetailVO getById(Long id) {
        Competition competition = getCompetitionOrThrow(id);

        // 构建比赛基本信息 VO
        Event event = competition.getEventId() == null ? null : eventDAO.getById(competition.getEventId());
        Venue venue = competition.getVenueId() == null ? null : venueDAO.getById(competition.getVenueId());
        CompetitionVO competitionVO = buildCompetitionVO(competition, event, venue);

        // 查询裁判分配列表（按 role 升序）+ 批量查裁判名称
        List<RefereeAssignment> assignments = refereeAssignmentDAO.findByCompetitionId(id);
        Set<Long> refereeIds = assignments.stream()
                .map(RefereeAssignment::getRefereeId)
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toSet());
        // RefereeDAO 无 findByIds 批量方法，单场次裁判数极少（≤4），逐条查询可接受
        Map<Long, String> refereeNameMap = refereeIds.stream()
                .collect(Collectors.toMap(
                        rid -> rid,
                        rid -> {
                            Referee referee = refereeDAO.getById(rid);
                            return referee != null ? referee.getName() : null;
                        },
                        (a, b) -> a
                ));
        List<RefereeAssignmentVO> referees = assignments.stream()
                .map(a -> RefereeAssignmentVO.from(a, refereeNameMap.get(a.getRefereeId())))
                .toList();

        // 查询器材分配列表 + 批量查器材名称
        List<CompetitionEquipment> equipments = competitionEquipmentDAO.findByCompetitionId(id);
        Set<Long> equipmentIds = equipments.stream()
                .map(CompetitionEquipment::getEquipmentId)
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toSet());
        // EquipmentDAO 无 findByIds 批量方法，单场次器材种类有限，逐条查询可接受
        Map<Long, String> equipmentNameMap = equipmentIds.stream()
                .collect(Collectors.toMap(
                        eid -> eid,
                        eid -> {
                            Equipment equipment = equipmentDAO.getById(eid);
                            return equipment != null ? equipment.getName() : null;
                        },
                        (a, b) -> a
                ));
        List<CompetitionEquipmentVO> equipmentVOs = equipments.stream()
                .map(e -> CompetitionEquipmentVO.from(e, equipmentNameMap.get(e.getEquipmentId())))
                .toList();

        // 查询个人参赛者列表（按 competitionId 筛选）+ 批量查运动员与学院名称
        Page<Participation> participationPage = participationDAO.page(
                1, 1000, null, null, null, id
        );
        List<Participation> participations = participationPage.getRecords();
        Set<Long> athleteIds = participations.stream()
                .map(Participation::getAthleteId)
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, Athlete> athleteMap = athleteDAO.findByIds(athleteIds).stream()
                .collect(Collectors.toMap(Athlete::getId, a -> a, (a, b) -> a));
        Map<Long, String> collegeNameMap = collegeDAO.findAll().stream()
                .collect(Collectors.toMap(College::getId, College::getName, (a, b) -> a));
        List<ParticipantArrangementVO> participantVOs = participations.stream()
                .map(p -> {
                    Athlete athlete = athleteMap.get(p.getAthleteId());
                    String athleteName = athlete != null ? athlete.getName() : null;
                    Long collegeId = athlete != null ? athlete.getCollegeId() : null;
                    String collegeName = collegeId != null ? collegeNameMap.get(collegeId) : null;
                    return ParticipantArrangementVO.from(p, athleteName, collegeName);
                })
                .toList();

        // 查询团体参赛者列表（按 competitionId 筛选）+ 批量查学院名称
        Page<TeamParticipation> teamPage = teamParticipationDAO.page(
                1, 1000, null, null, null, id
        );
        List<TeamParticipation> teamParticipations = teamPage.getRecords();
        List<TeamParticipantArrangementVO> teamParticipantVOs = teamParticipations.stream()
                .map(tp -> {
                    String collegeName = tp.getCollegeId() != null ? collegeNameMap.get(tp.getCollegeId()) : null;
                    return TeamParticipantArrangementVO.from(tp, collegeName);
                })
                .toList();

        return CompetitionDetailVO.from(competitionVO, referees, equipmentVOs, participantVOs, teamParticipantVOs);
    }

    /**
     * {@inheritDoc}
     * <p>
     * 校验链：管理员权限 → 场次存在 → 终态拦截（status=2/3 不可修改）→
     * 部分更新字段 → 时间/场地/项目变更时重新触发冲突检测（排除自身）→ 更新 → 构建 VO。
     * </p>
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public CompetitionVO update(Long id, CompetitionUpdateDTO dto) {
        checkAdmin();

        Competition competition = getCompetitionOrThrow(id);

        // 状态校验：已结束(2)或已取消(3)的场次不可修改
        if (CompetitionConstants.STATUS_FINISHED == competition.getStatus()
                || CompetitionConstants.STATUS_CANCELLED == competition.getStatus()) {
            throw new BusinessException(ErrorCode.COMPETITION_FINISHED);
        }

        // 记录是否修改了影响冲突检测的字段
        boolean timeOrVenueOrEventChanged = false;
        LocalDateTime newStartTime = competition.getStartTime();
        LocalDateTime newEndTime = competition.getEndTime();
        Long effectiveVenueId = competition.getVenueId();
        Long effectiveEventId = competition.getEventId();

        // 部分更新：仅更新非空字段（参考 EventLogic.update 的 null 检查模式）
        if (dto.getEventId() != null) {
            competition.setEventId(dto.getEventId());
            effectiveEventId = dto.getEventId();
            timeOrVenueOrEventChanged = true;
        }
        if (dto.getVenueId() != null) {
            competition.setVenueId(dto.getVenueId());
            effectiveVenueId = dto.getVenueId();
            timeOrVenueOrEventChanged = true;
        }
        if (dto.getRound() != null) {
            competition.setRound(dto.getRound());
        }
        if (dto.getGroupNo() != null) {
            competition.setGroupNo(dto.getGroupNo());
        }
        if (dto.getStartTime() != null) {
            newStartTime = parseDateTime(dto.getStartTime());
            competition.setStartTime(newStartTime);
            timeOrVenueOrEventChanged = true;
        }
        if (dto.getEndTime() != null) {
            newEndTime = parseDateTime(dto.getEndTime());
            competition.setEndTime(newEndTime);
            timeOrVenueOrEventChanged = true;
        }
        if (dto.getRemark() != null) {
            competition.setRemark(dto.getRemark());
        }

        // 若修改了时间/场地/项目，重新触发冲突检测
        if (timeOrVenueOrEventChanged) {
            // 时间合法性校验（任一端变更后需重新校验 start < end）
            if (!newStartTime.isBefore(newEndTime)) {
                throw new BusinessException(ErrorCode.PARAM_VALIDATION_FAILED);
            }

            // 校验项目存在且启用（若修改了项目）
            Event event = eventDAO.getById(effectiveEventId);
            if (event == null) {
                throw new BusinessException(ErrorCode.EVENT_NOT_FOUND);
            }
            if (EventConstants.STATUS_ENABLED != event.getStatus()) {
                throw new BusinessException(ErrorCode.EVENT_DISABLED);
            }

            // 校验场地存在且可用（若修改了场地）
            Venue venue = venueDAO.getById(effectiveVenueId);
            if (venue == null) {
                throw new BusinessException(ErrorCode.VENUE_NOT_FOUND);
            }
            if (EventConstants.STATUS_ENABLED != venue.getStatus()) {
                throw new BusinessException(ErrorCode.VENUE_NOT_FOUND);
            }

            // 径赛互斥检测（排除自身）
            if (EventConstants.CATEGORY_TRACK == event.getCategory()) {
                Long trackConflicts = competitionDAO.countTrackCompetitionsInTimeRange(
                        newStartTime, newEndTime, id
                );
                if (trackConflicts != null && trackConflicts > 0) {
                    throw new BusinessException(ErrorCode.TRACK_TIME_CONFLICT);
                }
            }

            // 场地冲突检测（排除自身）
            Long venueConflicts = competitionDAO.countVenueConflicts(
                    effectiveVenueId, newStartTime, newEndTime, id
            );
            if (venueConflicts != null && venueConflicts > 0) {
                throw new BusinessException(ErrorCode.VENUE_TIME_CONFLICT);
            }
        }

        Competition updated = competitionDAO.update(competition);
        // update 内部已驱逐所有秩序册缓存，此处无需重复调用
        log.info("修改比赛场次: id={}", id);

        // 重新查库构建完整 VO
        Event event = updated.getEventId() == null ? null : eventDAO.getById(updated.getEventId());
        Venue venue = updated.getVenueId() == null ? null : venueDAO.getById(updated.getVenueId());
        return buildCompetitionVO(updated, event, venue);
    }

    /**
     * {@inheritDoc}
     * <p>
     * 校验链：管理员权限 → 场次存在 → 进行中(1)拦截 → 成绩校验占位（TODO）→
     * 级联清理裁判分配与器材归还 → 删除场次。
     * </p>
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteById(Long id) {
        checkAdmin();

        Competition competition = getCompetitionOrThrow(id);

        // 状态校验：已结束(2)/已取消(3)的场次不可删除
        if (CompetitionConstants.STATUS_FINISHED == competition.getStatus()
                || CompetitionConstants.STATUS_CANCELLED == competition.getStatus()) {
            throw new BusinessException(ErrorCode.COMPETITION_FINISHED);
        }

        // 状态校验：进行中(1)的场次不可删除
        if (CompetitionConstants.STATUS_IN_PROGRESS == competition.getStatus()) {
            throw new BusinessException(ErrorCode.COMPETITION_IN_PROGRESS);
        }

        // 成绩校验：该场次下已录入成绩则禁止删除，避免成绩孤立
        if (!scoreDAO.findByCompetitionId(id).isEmpty()) {
            throw new BusinessException(ErrorCode.SCORE_ALREADY_EXISTS);
        }

        // 级联处理 1：裁判分配逻辑删除
        List<RefereeAssignment> assignments = refereeAssignmentDAO.findByCompetitionId(id);
        for (RefereeAssignment assignment : assignments) {
            refereeAssignmentDAO.deleteById(assignment.getId());
        }

        // 级联处理 2：器材使用记录标记归还 + 恢复库存
        // 先恢复未归还记录（status != 2）占用的库存，再统一标记为已归还
        List<CompetitionEquipment> equipmentRecords = competitionEquipmentDAO.findByCompetitionId(id);
        Map<Long, Integer> restoreMap = new HashMap<>();
        for (CompetitionEquipment record : equipmentRecords) {
            if (record.getStatus() != null
                    && record.getStatus() != CompetitionConstants.EQUIPMENT_STATUS_RETURNED) {
                int qty = record.getQuantity() != null ? record.getQuantity() : 0;
                if (qty > 0) {
                    restoreMap.merge(record.getEquipmentId(), qty, Integer::sum);
                }
            }
        }
        restoreMap.forEach((equipmentId, qty) -> equipmentDAO.restoreAvailable(equipmentId, qty));
        competitionEquipmentDAO.updateStatusToReturnedByCompetitionId(id);

        competitionDAO.deleteById(id);
        // deleteById 内部已驱逐所有秩序册缓存，此处无需重复调用
        log.info("删除比赛场次: id={}, 级联清理裁判分配 {} 条、器材记录 {} 条已标记归还并恢复库存",
                id, assignments.size(), equipmentRecords.size());
    }

    /**
     * {@inheritDoc}
     * <p>
     * 校验链：管理员权限 → 场次存在 → 状态流转合法性校验
     * （0→1/3, 1→2, 2/3 为终态）→ 更新状态。
     * </p>
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateStatus(Long id, UpdateCompetitionStatusDTO dto) {
        checkAdmin();

        Competition competition = getCompetitionOrThrow(id);

        // 状态流转合法性校验
        if (!isValidStatusTransition(competition.getStatus(), dto.getStatus())) {
            throw new BusinessException(ErrorCode.INVALID_STATUS_TRANSITION);
        }

        competitionDAO.updateStatus(id, dto.getStatus());
        // updateStatus 内部已驱逐所有秩序册缓存，此处无需重复调用
        log.info("比赛状态流转: id={}, {} -> {}", id, competition.getStatus(), dto.getStatus());
    }

    /**
     * {@inheritDoc}
     * <p>
     * 无需管理员权限（读操作开放）。委托 CompetitionDAO.getBooklet 获取缓存聚合结果。
     * </p>
     */
    @Override
    public List<ScheduleBookletVO> getBooklet(String date) {
        return competitionDAO.getBooklet(date);
    }

    // ==================== 第二部分：运动员编排 + 团体编排 ====================

    /**
     * {@inheritDoc}
     * <p>
     * 校验链：管理员权限 → 场次存在且未结束 → 项目为个人项目 →
     * 逐条校验参赛记录（存在、属于该项目运动员、未被编排、无运动员时间冲突）→
     * 批量写入 competitionId/laneNo/bibNumber → 批量关联运动员/学院名称构建 VO → 驱逐秩序册缓存。
     * </p>
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<ParticipantArrangementVO> arrangeParticipants(Long competitionId, BatchArrangeDTO dto) {
        checkAdmin();

        Competition competition = getCompetitionOrThrow(competitionId);
        if (CompetitionConstants.STATUS_FINISHED == competition.getStatus()
                || CompetitionConstants.STATUS_CANCELLED == competition.getStatus()) {
            throw new BusinessException(ErrorCode.COMPETITION_FINISHED);
        }

        Event event = eventDAO.getById(competition.getEventId());
        if (event == null || EventConstants.TYPE_INDIVIDUAL != event.getType()) {
            throw new BusinessException(ErrorCode.EVENT_NOT_INDIVIDUAL);
        }

        LocalDateTime compStart = competition.getStartTime();
        LocalDateTime compEnd = competition.getEndTime();

        // 逐条校验：第一个失败即抛出
        for (BatchArrangeDTO.ArrangeItem item : dto.getParticipants()) {
            Participation participation = participationDAO.getById(item.getParticipationId());
            if (participation == null) {
                throw new BusinessException(ErrorCode.PARTICIPATION_NOT_FOUND);
            }
            if (!java.util.Objects.equals(participation.getEventId(), competition.getEventId())) {
                throw new BusinessException(ErrorCode.PARTICIPATION_EVENT_MISMATCH);
            }
            if (participation.getCompetitionId() != null) {
                throw new BusinessException(ErrorCode.PARTICIPATION_ALREADY_ARRANGED);
            }
            Long conflicts = competitionDAO.countAthleteTimeConflicts(
                    participation.getAthleteId(), compStart, compEnd, competitionId
            );
            if (conflicts != null && conflicts > 0) {
                throw new BusinessException(ErrorCode.ATHLETE_TIME_CONFLICT);
            }
        }

        // 全部校验通过，批量写入 competitionId/laneNo/bibNumber
        List<Participation> updated = new ArrayList<>(dto.getParticipants().size());
        for (BatchArrangeDTO.ArrangeItem item : dto.getParticipants()) {
            Participation patch = new Participation();
            patch.setId(item.getParticipationId());
            patch.setCompetitionId(competitionId);
            if (item.getLaneNo() != null) {
                patch.setLaneNo(item.getLaneNo());
            }
            if (item.getBibNumber() != null) {
                patch.setBibNumber(item.getBibNumber());
            }
            updated.add(participationDAO.update(patch));
        }

        List<ParticipantArrangementVO> result = buildParticipantArrangementVOs(updated);
        competitionDAO.evictAllBooklets();
        log.info("编排个人参赛者到场次: competitionId={}, count={}", competitionId, updated.size());
        return result;
    }

    /**
     * {@inheritDoc}
     * <p>
     * 无需管理员权限（读操作开放）。按 competitionId 筛选参赛记录，
     * 批量关联运动员与学院名称。空列表早返回。
     * </p>
     */
    @Override
    public List<ParticipantArrangementVO> getParticipants(Long competitionId) {
        Page<Participation> participationPage = participationDAO.page(
                1, 1000, null, null, null, competitionId
        );
        List<Participation> participations = participationPage.getRecords();
        if (participations.isEmpty()) {
            return List.of();
        }
        return buildParticipantArrangementVOs(participations);
    }

    /**
     * {@inheritDoc}
     * <p>
     * 校验链：管理员权限 → 场次存在且未结束 → 参赛记录存在 →
     * 参赛记录确实编排到此场次 → 清空 competitionId/laneNo/bibNumber → 驱逐秩序册缓存。
     * </p>
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void removeParticipant(Long competitionId, Long participationId) {
        checkAdmin();

        Competition competition = getCompetitionOrThrow(competitionId);
        if (CompetitionConstants.STATUS_FINISHED == competition.getStatus()
                || CompetitionConstants.STATUS_CANCELLED == competition.getStatus()) {
            throw new BusinessException(ErrorCode.COMPETITION_FINISHED);
        }

        Participation participation = participationDAO.getById(participationId);
        if (participation == null) {
            throw new BusinessException(ErrorCode.PARTICIPATION_NOT_FOUND);
        }
        if (!java.util.Objects.equals(participation.getCompetitionId(), competitionId)) {
            throw new BusinessException(ErrorCode.PARTICIPATION_NOT_IN_COMPETITION);
        }

        // 成绩校验：该个人编排已有成绩记录则禁止移除，避免成绩孤立
        if (scoreDAO.existsByParticipationId(participationId)) {
            throw new BusinessException(ErrorCode.SCORE_ALREADY_EXISTS);
        }

        // 使用 LambdaUpdateWrapper 显式置 null，绕开 updateById 默认 NOT_NULL 策略
        participationDAO.clearCompetitionArrangement(participationId);

        competitionDAO.evictAllBooklets();
        log.info("移除个人参赛编排: competitionId={}, participationId={}", competitionId, participationId);
    }

    /**
     * {@inheritDoc}
     * <p>
     * 校验链：管理员权限 → 场次存在且未结束 → 项目为集体项目 →
     * 逐条校验团体参赛记录（存在、未被编排、无学院集体时间冲突）→
     * 批量写入 competitionId → 批量关联学院名称构建 VO → 驱逐秩序册缓存。
     * </p>
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<TeamParticipantArrangementVO> arrangeTeamParticipants(Long competitionId, BatchTeamArrangeDTO dto) {
        checkAdmin();

        Competition competition = getCompetitionOrThrow(competitionId);
        if (CompetitionConstants.STATUS_FINISHED == competition.getStatus()
                || CompetitionConstants.STATUS_CANCELLED == competition.getStatus()) {
            throw new BusinessException(ErrorCode.COMPETITION_FINISHED);
        }

        Event event = eventDAO.getById(competition.getEventId());
        if (event == null || EventConstants.TYPE_TEAM != event.getType()) {
            throw new BusinessException(ErrorCode.EVENT_NOT_TEAM);
        }

        LocalDateTime compStart = competition.getStartTime();
        LocalDateTime compEnd = competition.getEndTime();

        // 逐条校验：第一个失败即抛出
        for (BatchTeamArrangeDTO.TeamArrangeItem item : dto.getTeamParticipants()) {
            TeamParticipation teamParticipation = teamParticipationDAO.getById(item.getTeamParticipationId());
            if (teamParticipation == null) {
                throw new BusinessException(ErrorCode.TEAM_PARTICIPATION_NOT_FOUND);
            }
            if (teamParticipation.getCompetitionId() != null) {
                throw new BusinessException(ErrorCode.TEAM_ALREADY_ARRANGED);
            }
            Long conflicts = competitionDAO.countCollegeTeamTimeConflicts(
                    teamParticipation.getCollegeId(), compStart, compEnd, competitionId
            );
            if (conflicts != null && conflicts > 0) {
                throw new BusinessException(ErrorCode.COLLEGE_TIME_CONFLICT);
            }
        }

        // 全部校验通过，批量写入 competitionId
        List<TeamParticipation> updated = new ArrayList<>(dto.getTeamParticipants().size());
        for (BatchTeamArrangeDTO.TeamArrangeItem item : dto.getTeamParticipants()) {
            TeamParticipation patch = new TeamParticipation();
            patch.setId(item.getTeamParticipationId());
            patch.setCompetitionId(competitionId);
            updated.add(teamParticipationDAO.update(patch));
        }

        List<TeamParticipantArrangementVO> result = buildTeamParticipantArrangementVOs(updated);
        competitionDAO.evictAllBooklets();
        log.info("编排团体参赛者到场次: competitionId={}, count={}", competitionId, updated.size());
        return result;
    }

    /**
     * {@inheritDoc}
     * <p>
     * 校验链：管理员权限 → 场次存在且未结束 → 团体参赛记录存在 →
     * 团体参赛记录确实编排到此场次 → 清空 competitionId → 驱逐秩序册缓存。
     * </p>
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void removeTeamParticipant(Long competitionId, Long teamParticipationId) {
        checkAdmin();

        Competition competition = getCompetitionOrThrow(competitionId);
        if (CompetitionConstants.STATUS_FINISHED == competition.getStatus()
                || CompetitionConstants.STATUS_CANCELLED == competition.getStatus()) {
            throw new BusinessException(ErrorCode.COMPETITION_FINISHED);
        }

        TeamParticipation teamParticipation = teamParticipationDAO.getById(teamParticipationId);
        if (teamParticipation == null) {
            throw new BusinessException(ErrorCode.TEAM_PARTICIPATION_NOT_FOUND);
        }
        if (!java.util.Objects.equals(teamParticipation.getCompetitionId(), competitionId)) {
            throw new BusinessException(ErrorCode.TEAM_NOT_IN_COMPETITION);
        }

        // 成绩校验：该团体编排已有成绩记录则禁止移除，避免成绩孤立
        if (scoreDAO.existsByTeamParticipationId(teamParticipationId)) {
            throw new BusinessException(ErrorCode.TEAM_SCORE_ALREADY_EXISTS);
        }

        // 使用 LambdaUpdateWrapper 显式置 null，绕开 updateById 默认 NOT_NULL 策略
        teamParticipationDAO.clearCompetitionArrangement(teamParticipationId);

        competitionDAO.evictAllBooklets();
        log.info("移除团体参赛编排: competitionId={}, teamParticipationId={}", competitionId, teamParticipationId);
    }

    // ==================== 私有辅助方法 ====================

    /**
     * 检查当前用户是否为管理员
     * <p>
     * 非管理员抛出 {@link BusinessException}（FORBIDDEN）
     * </p>
     */
    private void checkAdmin() {
        Integer role = UserContext.getRole();
        if (role == null || UserConstants.ROLE_ADMIN != role) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
    }

    /**
     * 获取比赛场次，不存在则抛异常
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
     * 校验状态流转合法性
     * <p>
     * 合法流转：未开始(0)→进行中(1)/已取消(3)、进行中(1)→已结束(2)；
     * 已结束(2)与已取消(3)为终态，不可流转。
     * </p>
     *
     * @param current 当前状态
     * @param target  目标状态
     * @return true=合法流转，false=非法流转
     */
    private boolean isValidStatusTransition(int current, int target) {
        return switch (current) {
            case CompetitionConstants.STATUS_NOT_STARTED ->
                    target == CompetitionConstants.STATUS_IN_PROGRESS
                            || target == CompetitionConstants.STATUS_CANCELLED;
            case CompetitionConstants.STATUS_IN_PROGRESS ->
                    target == CompetitionConstants.STATUS_FINISHED;
            default -> false; // 已结束(2)与已取消(3)为终态
        };
    }

    /**
     * 将 ISO datetime 字符串解析为 {@link LocalDateTime}
     * <p>
     * 解析失败抛出 {@link BusinessException}（PARAM_VALIDATION_FAILED）
     * </p>
     *
     * @param dateTimeStr ISO datetime 字符串（如 "2026-05-15T08:00:00"）
     * @return 解析后的 LocalDateTime
     */
    private LocalDateTime parseDateTime(String dateTimeStr) {
        try {
            return LocalDateTime.parse(dateTimeStr);
        } catch (DateTimeParseException e) {
            throw new BusinessException(ErrorCode.PARAM_VALIDATION_FAILED);
        }
    }

    /**
     * 构建比赛场次 VO（含项目名称/类型/类别、场地名称）
     * <p>
     * 通过已查询的 Event 和 Venue 实体组装 VO，避免重复查库。
     * 当 event 或 venue 为 null 时，对应关联字段为 null。
     * </p>
     *
     * @param competition 比赛场次实体
     * @param event       项目实体（可为 null）
     * @param venue       场地实体（可为 null）
     * @return 比赛场次 VO
     */
    private CompetitionVO buildCompetitionVO(Competition competition, Event event, Venue venue) {
        String eventName = event != null ? event.getName() : null;
        Integer eventType = event != null ? event.getType() : null;
        Integer eventCategory = event != null ? event.getCategory() : null;
        String venueName = venue != null ? venue.getName() : null;
        return CompetitionVO.from(competition, eventName, eventType, eventCategory, venueName);
    }

    /**
     * 批量构建个人参赛安排 VO 列表
     * <p>
     * 批量查询运动员与学院名称（避免 N+1），组装 {@link ParticipantArrangementVO}。
     * 空列表早返回 {@code List.of()}。
     * </p>
     *
     * @param participations 参赛记录列表
     * @return 个人参赛安排 VO 列表
     */
    private List<ParticipantArrangementVO> buildParticipantArrangementVOs(List<Participation> participations) {
        if (participations == null || participations.isEmpty()) {
            return List.of();
        }
        Set<Long> athleteIds = participations.stream()
                .map(Participation::getAthleteId)
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, Athlete> athleteMap = athleteDAO.findByIds(athleteIds).stream()
                .collect(Collectors.toMap(Athlete::getId, a -> a, (a, b) -> a));
        Map<Long, String> collegeNameMap = collegeDAO.findAll().stream()
                .collect(Collectors.toMap(College::getId, College::getName, (a, b) -> a));
        return participations.stream()
                .map(p -> {
                    Athlete athlete = athleteMap.get(p.getAthleteId());
                    String athleteName = athlete != null ? athlete.getName() : null;
                    Long collegeId = athlete != null ? athlete.getCollegeId() : null;
                    String collegeName = collegeId != null ? collegeNameMap.get(collegeId) : null;
                    return ParticipantArrangementVO.from(p, athleteName, collegeName);
                })
                .toList();
    }

    /**
     * 批量构建团体参赛安排 VO 列表
     * <p>
     * 批量查询学院名称（避免 N+1），组装 {@link TeamParticipantArrangementVO}。
     * 空列表早返回 {@code List.of()}。
     * </p>
     *
     * @param teamParticipations 团体参赛记录列表
     * @return 团体参赛安排 VO 列表
     */
    private List<TeamParticipantArrangementVO> buildTeamParticipantArrangementVOs(
            List<TeamParticipation> teamParticipations) {
        if (teamParticipations == null || teamParticipations.isEmpty()) {
            return List.of();
        }
        Map<Long, String> collegeNameMap = collegeDAO.findAll().stream()
                .collect(Collectors.toMap(College::getId, College::getName, (a, b) -> a));
        return teamParticipations.stream()
                .map(tp -> {
                    String collegeName = tp.getCollegeId() != null
                            ? collegeNameMap.get(tp.getCollegeId())
                            : null;
                    return TeamParticipantArrangementVO.from(tp, collegeName);
                })
                .toList();
    }
}
