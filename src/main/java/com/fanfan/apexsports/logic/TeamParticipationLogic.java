package com.fanfan.apexsports.logic;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fanfan.apexsports.common.result.PageResult;
import com.fanfan.apexsports.config.UserContext;
import com.fanfan.apexsports.constants.EventConstants;
import com.fanfan.apexsports.constants.TeamParticipationConstants;
import com.fanfan.apexsports.constants.UserConstants;
import com.fanfan.apexsports.daos.CollegeDAO;
import com.fanfan.apexsports.daos.EventDAO;
import com.fanfan.apexsports.daos.TeamParticipationDAO;
import com.fanfan.apexsports.dtos.TeamParticipationCreateDTO;
import com.fanfan.apexsports.entities.College;
import com.fanfan.apexsports.entities.Competition;
import com.fanfan.apexsports.entities.Event;
import com.fanfan.apexsports.entities.TeamParticipation;
import com.fanfan.apexsports.exception.BusinessException;
import com.fanfan.apexsports.exception.ErrorCode;
import com.fanfan.apexsports.services.TeamParticipationService;
import com.fanfan.apexsports.vos.TeamParticipationVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 团体参赛记录业务实现
 * <p>
 * 实现团体参赛记录的分页查询、详情查询、创建（含学院存在性、项目存在性、项目状态、项目类型、
 * 参赛人数上限、同学院+项目唯一性共 6 条校验）、删除（取消报名，含已编排拦截）、检录
 * （含团体是否被编排、比赛场次可检录状态、当前状态为已报名共 4 条校验）等业务逻辑。
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
public class TeamParticipationLogic implements TeamParticipationService {

    private final TeamParticipationDAO teamParticipationDAO;
    private final CollegeDAO collegeDAO;
    private final EventDAO eventDAO;

    /**
     * 分页查询团体参赛记录
     * <p>
     * 所有角色均可查询，无需管理员权限。批量查询学院名称与项目名称以构建列表视图，避免 N+1 查询。
     * 由于 team_participation 表自带 college_id 字段，可直接按 collegeId 筛选，无需 Logic 层做转换。
     * </p>
     *
     * @param current       当前页码
     * @param size          每页大小
     * @param eventId       项目ID（可空）
     * @param collegeId     学院ID（可空）
     * @param status        状态（可空）
     * @param competitionId 比赛场次ID（可空）
     * @return 分页结果
     */
    @Override
    public PageResult<TeamParticipationVO> page(Integer current, Integer size, Long eventId, Long collegeId,
                                                Integer status, Long competitionId) {
        int pageNum = (current == null || current < 1) ? 1 : current;
        int pageSize = (size == null || size < 1) ? 10 : Math.min(size, 100);

        Page<TeamParticipation> page = teamParticipationDAO.page(pageNum, pageSize, eventId, collegeId, status, competitionId);

        // 批量构建学院名称映射（学院数量通常很少，一次性加载）
        Map<Long, String> collegeNameMap = collegeDAO.findAll().stream()
                .collect(Collectors.toMap(College::getId, College::getName));

        // 批量构建项目名称映射，避免 N+1 查询
        Set<Long> eventIds = page.getRecords().stream()
                .map(TeamParticipation::getEventId)
                .collect(Collectors.toSet());
        Map<Long, String> eventNameMap = eventDAO.findByIds(eventIds).stream()
                .collect(Collectors.toMap(Event::getId, Event::getName));

        List<TeamParticipationVO> records = page.getRecords().stream()
                .map(tp -> TeamParticipationVO.from(tp, collegeNameMap.get(tp.getCollegeId()), eventNameMap.get(tp.getEventId())))
                .toList();

        return new PageResult<>(records, page.getTotal(), page.getCurrent(), page.getSize(), page.getPages());
    }

    /**
     * 根据 ID 查询团体参赛记录详情
     * <p>
     * 所有角色均可查询，返回包含学院名称与项目名称的详情视图
     * </p>
     *
     * @param id 团体参赛记录ID
     * @return 团体参赛记录 VO
     */
    @Override
    public TeamParticipationVO getById(Long id) {
        TeamParticipation teamParticipation = getTeamParticipationOrThrow(id);
        College college = collegeDAO.getById(teamParticipation.getCollegeId());
        Event event = eventDAO.getById(teamParticipation.getEventId());
        String collegeName = college != null ? college.getName() : null;
        String eventName = event != null ? event.getName() : null;
        return TeamParticipationVO.from(teamParticipation, collegeName, eventName);
    }

    /**
     * 新增团体参赛记录
     * <p>
     * 需要管理员权限。执行 6 条校验：学院存在性、项目存在性、项目状态启用、项目类型为集体项目、
     * 参赛人数不超过项目上限、同学院+项目不重复报名。状态默认为 0（已报名）。
     * </p>
     *
     * @param dto 新增团体参赛记录请求数据
     * @return 新创建的团体参赛记录 VO
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public TeamParticipationVO create(TeamParticipationCreateDTO dto) {
        checkAdmin();

        // 校验 1：所属学院是否存在
        College college = collegeDAO.getById(dto.getCollegeId());
        if (college == null) {
            throw new BusinessException(ErrorCode.COLLEGE_NOT_FOUND);
        }

        // 校验 2：所属项目是否存在
        Event event = eventDAO.getById(dto.getEventId());
        if (event == null) {
            throw new BusinessException(ErrorCode.EVENT_NOT_FOUND);
        }

        // 校验 3：项目状态必须为启用
        if (EventConstants.STATUS_ENABLED != event.getStatus()) {
            throw new BusinessException(ErrorCode.EVENT_DISABLED);
        }

        // 校验 4：项目类型必须为集体项目
        if (EventConstants.TYPE_TEAM != event.getType()) {
            throw new BusinessException(ErrorCode.EVENT_NOT_TEAM);
        }

        // 校验 5：参赛人数不得超过项目上限
        if (event.getMaxParticipants() != null && dto.getParticipantCount() > event.getMaxParticipants()) {
            throw new BusinessException(ErrorCode.PARTICIPANT_COUNT_EXCEEDED);
        }

        // 校验 6：同学院+项目不得重复报名
        TeamParticipation existing = teamParticipationDAO.findByCollegeIdAndEventId(dto.getCollegeId(), dto.getEventId());
        if (existing != null) {
            throw new BusinessException(ErrorCode.COLLEGE_EVENT_DUPLICATE);
        }

        // 构建实体，状态默认为 0（已报名）
        TeamParticipation teamParticipation = new TeamParticipation()
                .setCollegeId(dto.getCollegeId())
                .setEventId(dto.getEventId())
                .setParticipantCount(dto.getParticipantCount())
                .setStatus(TeamParticipationConstants.STATUS_REGISTERED);

        TeamParticipation inserted = teamParticipationDAO.insert(teamParticipation);
        log.info("创建团体参赛记录: id={}, collegeId={}, eventId={}", inserted.getId(), inserted.getCollegeId(), inserted.getEventId());
        return TeamParticipationVO.from(inserted, college.getName(), event.getName());
    }

    /**
     * 删除团体参赛记录（取消报名）
     * <p>
     * 需要管理员权限。若记录已被编排到比赛场次（competitionId 非空），则不允许取消，需先移除编排。
     * </p>
     *
     * @param id 团体参赛记录ID
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteById(Long id) {
        checkAdmin();

        TeamParticipation teamParticipation = getTeamParticipationOrThrow(id);

        // 若已被编排到比赛场次，不可取消
        if (teamParticipation.getCompetitionId() != null) {
            throw new BusinessException(ErrorCode.PARTICIPATION_ALREADY_SCHEDULED);
        }

        teamParticipationDAO.deleteById(id);
        log.info("取消团体参赛记录: id={}", id);
    }

    /**
     * 检录（将状态从"已报名"更新为"检录通过"）
     * <p>
     * 需要管理员权限。执行 4 条校验：记录存在性、团体已被编排到比赛场次、关联比赛处于可检录状态、
     * 当前状态为"已报名"。
     * </p>
     *
     * @param id 团体参赛记录ID
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void checkIn(Long id) {
        checkAdmin();

        TeamParticipation teamParticipation = getTeamParticipationOrThrow(id);

        // 校验 1：团体必须已被编排到比赛场次
        if (teamParticipation.getCompetitionId() == null) {
            throw new BusinessException(ErrorCode.TEAM_NOT_SCHEDULED);
        }

        // 校验 2：关联比赛场次状态必须允许检录（status ∈ {0, 1}）
        Competition competition = teamParticipationDAO.getCompetitionById(teamParticipation.getCompetitionId());
        if (competition == null || (competition.getStatus() != 0 && competition.getStatus() != 1)) {
            throw new BusinessException(ErrorCode.COMPETITION_NOT_CHECKABLE);
        }

        // 校验 3：当前状态必须为"已报名"
        if (TeamParticipationConstants.STATUS_REGISTERED != teamParticipation.getStatus()) {
            throw new BusinessException(ErrorCode.TEAM_PARTICIPATION_ALREADY_CHECKED);
        }

        // 更新状态为"检录通过"
        teamParticipation.setStatus(TeamParticipationConstants.STATUS_CHECKED_IN);
        teamParticipationDAO.update(teamParticipation);
        log.info("团体检录成功: id={}", id);
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
     * 获取团体参赛记录，不存在则抛异常
     *
     * @param id 团体参赛记录ID
     * @return 团体参赛记录实体
     */
    private TeamParticipation getTeamParticipationOrThrow(Long id) {
        TeamParticipation teamParticipation = teamParticipationDAO.getById(id);
        if (teamParticipation == null) {
            throw new BusinessException(ErrorCode.TEAM_PARTICIPATION_NOT_FOUND);
        }
        return teamParticipation;
    }
}
