package com.fanfan.apexsports.logic;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fanfan.apexsports.common.result.PageResult;
import com.fanfan.apexsports.config.UserContext;
import com.fanfan.apexsports.constants.AthleteConstants;
import com.fanfan.apexsports.constants.EventConstants;
import com.fanfan.apexsports.constants.ParticipationConstants;
import com.fanfan.apexsports.constants.UserConstants;
import com.fanfan.apexsports.daos.AthleteDAO;
import com.fanfan.apexsports.daos.CollegeDAO;
import com.fanfan.apexsports.daos.EventDAO;
import com.fanfan.apexsports.daos.ParticipationDAO;
import com.fanfan.apexsports.dtos.ParticipationCreateDTO;
import com.fanfan.apexsports.entities.Athlete;
import com.fanfan.apexsports.entities.College;
import com.fanfan.apexsports.entities.Competition;
import com.fanfan.apexsports.entities.Event;
import com.fanfan.apexsports.entities.Participation;
import com.fanfan.apexsports.exception.BusinessException;
import com.fanfan.apexsports.exception.ErrorCode;
import com.fanfan.apexsports.services.ParticipationService;
import com.fanfan.apexsports.vos.ParticipationVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 个人参赛记录业务实现
 * <p>
 * 实现个人参赛记录的分页查询、详情查询、创建（含运动员存在性、运动员状态、项目存在性、项目状态、
 * 项目类型、报名上限、同运动员+项目唯一性共 7 条校验）、删除（取消报名，含已编排拦截）、检录
 * （含运动员是否被编排、比赛场次可检录状态、当前状态为已报名共 4 条校验）等业务逻辑。
 * 所有写操作均需要管理员权限，读操作对所有角色开放。
 * </p>
 * <p>
 * <b>架构红线：</b>Logic 层不直接调用 Mapper（通过 DAO），不直接操作 Redis（通过 Cache 层），
 * 不出现 db/rdb/redis/database 等字样。
 * </p>
 *
 * @author fanfan187
 * @version v1.0.0
 * @since v1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ParticipationLogic implements ParticipationService {

    private final ParticipationDAO participationDAO;
    private final AthleteDAO athleteDAO;
    private final EventDAO eventDAO;
    private final CollegeDAO collegeDAO;

    /**
     * 分页查询个人参赛记录
     * <p>
     * 所有角色均可查询，无需管理员权限。由于 participation 表不含 collegeId 字段，
     * 当按学院筛选时，先查询该学院的运动员ID集合，再通过 athleteIds 参数筛选。
     * 批量查询运动员/学院/项目名称以构建列表视图，避免 N+1 查询。
     * </p>
     *
     * @param current       当前页码
     * @param size          每页大小
     * @param eventId       项目ID（可空）
     * @param collegeId     学院ID（可空，Logic 层转换为 athleteIds）
     * @param athleteId     运动员ID（可空）
     * @param status        状态（可空）
     * @param competitionId 比赛场次ID（可空）
     * @return 分页结果
     */
    @Override
    public PageResult<ParticipationVO> page(Integer current, Integer size, Long eventId, Long collegeId,
                                            Long athleteId, Integer status, Long competitionId) {
        int pageNum = (current == null || current < 1) ? 1 : current;
        int pageSize = (size == null || size < 1) ? 10 : Math.min(size, 100);

        // collegeId → athleteIds 转换（participation 表无 collegeId 字段）
        Collection<Long> athleteIds = null;
        if (athleteId != null) {
            athleteIds = Set.of(athleteId);
        } else if (collegeId != null) {
            Page<Athlete> athletePage = athleteDAO.page(1, 1000, collegeId, null, null, null, null);
            athleteIds = athletePage.getRecords().stream()
                    .map(Athlete::getId)
                    .collect(Collectors.toSet());
            if (athleteIds.isEmpty()) {
                return new PageResult<>(List.of(), 0, pageNum, pageSize, 0);
            }
        }

        Page<Participation> page = participationDAO.page(pageNum, pageSize, eventId, athleteIds, status, competitionId);

        // 批量构建运动员映射（name/studentNo/collegeId）
        Set<Long> pageAthleteIds = page.getRecords().stream()
                .map(Participation::getAthleteId)
                .collect(Collectors.toSet());
        Map<Long, Athlete> athleteMap = athleteDAO.findByIds(pageAthleteIds).stream()
                .collect(Collectors.toMap(Athlete::getId, a -> a));

        // 批量构建学院名称映射
        Map<Long, String> collegeNameMap = collegeDAO.findAll().stream()
                .collect(Collectors.toMap(College::getId, College::getName));

        // 批量构建项目名称映射
        Set<Long> eventIds = page.getRecords().stream()
                .map(Participation::getEventId)
                .collect(Collectors.toSet());
        Map<Long, String> eventNameMap = eventDAO.findByIds(eventIds).stream()
                .collect(Collectors.toMap(Event::getId, Event::getName));

        List<ParticipationVO> records = page.getRecords().stream()
                .map(p -> {
                    Athlete athlete = athleteMap.get(p.getAthleteId());
                    String athleteName = athlete != null ? athlete.getName() : null;
                    String studentNo = athlete != null ? athlete.getStudentNo() : null;
                    Long cid = athlete != null ? athlete.getCollegeId() : null;
                    String collegeName = cid != null ? collegeNameMap.get(cid) : null;
                    String eventName = eventNameMap.get(p.getEventId());
                    return ParticipationVO.from(p, athleteName, studentNo, cid, collegeName, p.getEventId(), eventName);
                })
                .toList();

        return new PageResult<>(records, page.getTotal(), page.getCurrent(), page.getSize(), page.getPages());
    }

    /**
     * 根据 ID 查询参赛记录详情
     *
     * @param id 参赛记录ID
     * @return 参赛记录 VO
     */
    @Override
    public ParticipationVO getById(Long id) {
        Participation participation = getParticipationOrThrow(id);
        Athlete athlete = athleteDAO.getById(participation.getAthleteId());
        College college = athlete != null ? collegeDAO.getById(athlete.getCollegeId()) : null;
        Event event = eventDAO.getById(participation.getEventId());
        return ParticipationVO.from(participation, athlete, college, event);
    }

    /**
     * 新增个人参赛记录
     * <p>
     * 需要管理员权限。执行 7 条校验：运动员存在性、运动员状态正常、项目存在性、项目状态启用、
     * 项目类型为个人项目、运动员已报名项目数未超上限、同运动员+项目不重复报名。
     * 状态默认为 0（已报名）。
     * </p>
     *
     * @param dto 新增参赛记录请求数据
     * @return 新创建的参赛记录 VO
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public ParticipationVO create(ParticipationCreateDTO dto) {
        checkAdmin();

        // 校验 1：运动员是否存在
        Athlete athlete = athleteDAO.getById(dto.getAthleteId());
        if (athlete == null) {
            throw new BusinessException(ErrorCode.ATHLETE_NOT_FOUND);
        }

        // 校验 2：运动员状态必须为正常（status=1）
        if (AthleteConstants.STATUS_ACTIVE != athlete.getStatus()) {
            throw new BusinessException(ErrorCode.ATHLETE_BANNED);
        }

        // 校验 3：项目是否存在
        Event event = eventDAO.getById(dto.getEventId());
        if (event == null) {
            throw new BusinessException(ErrorCode.EVENT_NOT_FOUND);
        }

        // 校验 4：项目状态必须为启用（status=1）
        if (EventConstants.STATUS_ENABLED != event.getStatus()) {
            throw new BusinessException(ErrorCode.EVENT_DISABLED);
        }

        // 校验 5：项目类型必须为个人项目（type=1）
        if (EventConstants.TYPE_INDIVIDUAL != event.getType()) {
            throw new BusinessException(ErrorCode.EVENT_NOT_INDIVIDUAL);
        }

        // 校验 6：已报名个人项目数不得超过上限
        int limit = event.getMaxIndividualEvents() != null
                ? event.getMaxIndividualEvents()
                : EventConstants.DEFAULT_MAX_INDIVIDUAL_EVENTS;
        Long currentCount = participationDAO.countByAthleteId(dto.getAthleteId());
        if (currentCount != null && currentCount >= limit) {
            throw new BusinessException(ErrorCode.ATHLETE_EVENT_LIMIT_EXCEEDED);
        }

        // 校验 7：同运动员+项目不得重复报名
        Participation existing = participationDAO.findByAthleteIdAndEventId(dto.getAthleteId(), dto.getEventId());
        if (existing != null) {
            throw new BusinessException(ErrorCode.ATHLETE_EVENT_DUPLICATE);
        }

        // 构建实体，状态默认为 0（已报名）
        Participation participation = new Participation()
                .setAthleteId(dto.getAthleteId())
                .setEventId(dto.getEventId())
                .setStatus(ParticipationConstants.STATUS_REGISTERED);

        Participation inserted = participationDAO.insert(participation);
        log.info("创建个人参赛记录: id={}, athleteId={}, eventId={}", inserted.getId(), inserted.getAthleteId(), inserted.getEventId());

        // 重新查询关联数据构建完整 VO
        College college = collegeDAO.getById(athlete.getCollegeId());
        return ParticipationVO.from(inserted, athlete, college, event);
    }

    /**
     * 删除参赛记录（取消报名）
     * <p>
     * 需要管理员权限。若记录已被编排到比赛场次（competitionId 非空），则不允许取消，需先移除编排。
     * </p>
     *
     * @param id 参赛记录ID
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteById(Long id) {
        checkAdmin();

        Participation participation = getParticipationOrThrow(id);

        // 若已被编排到比赛场次，不可取消
        if (participation.getCompetitionId() != null) {
            throw new BusinessException(ErrorCode.PARTICIPATION_ALREADY_SCHEDULED);
        }

        participationDAO.deleteById(id);
        log.info("取消个人参赛记录: id={}", id);
    }

    /**
     * 检录（将状态从"已报名"更新为"检录通过"）
     * <p>
     * 需要管理员权限。执行 4 条校验：记录存在性、运动员已被编排到比赛场次、关联比赛处于可检录状态、
     * 当前状态为"已报名"。
     * </p>
     *
     * @param id 参赛记录ID
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void checkIn(Long id) {
        checkAdmin();

        Participation participation = getParticipationOrThrow(id);

        // 校验 1：运动员必须已被编排到比赛场次
        if (participation.getCompetitionId() == null) {
            throw new BusinessException(ErrorCode.ATHLETE_NOT_REGISTERED);
        }

        // 校验 2：关联比赛场次状态必须允许检录（status ∈ {0, 1}）
        Competition competition = participationDAO.getCompetitionById(participation.getCompetitionId());
        if (competition == null || (competition.getStatus() != 0 && competition.getStatus() != 1)) {
            throw new BusinessException(ErrorCode.COMPETITION_NOT_CHECKABLE);
        }

        // 校验 3：当前状态必须为"已报名"
        if (ParticipationConstants.STATUS_REGISTERED != participation.getStatus()) {
            throw new BusinessException(ErrorCode.PARTICIPATION_ALREADY_CHECKED);
        }

        // 更新状态为"检录通过"
        participation.setStatus(ParticipationConstants.STATUS_CHECKED_IN);
        participationDAO.update(participation);
        log.info("个人检录成功: id={}", id);
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
     * 获取参赛记录，不存在则抛异常
     *
     * @param id 参赛记录ID
     * @return 参赛记录实体
     */
    private Participation getParticipationOrThrow(Long id) {
        Participation participation = participationDAO.getById(id);
        if (participation == null) {
            throw new BusinessException(ErrorCode.PARTICIPATION_NOT_FOUND);
        }
        return participation;
    }
}
