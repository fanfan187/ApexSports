package com.fanfan.apexsports.logic;

import com.fanfan.apexsports.config.UserContext;
import com.fanfan.apexsports.constants.CompetitionConstants;
import com.fanfan.apexsports.constants.RefereeConstants;
import com.fanfan.apexsports.constants.UserConstants;
import com.fanfan.apexsports.daos.CompetitionDAO;
import com.fanfan.apexsports.daos.RefereeAssignmentDAO;
import com.fanfan.apexsports.daos.RefereeDAO;
import com.fanfan.apexsports.dtos.AssignRefereeDTO;
import com.fanfan.apexsports.entities.Competition;
import com.fanfan.apexsports.entities.Referee;
import com.fanfan.apexsports.entities.RefereeAssignment;
import com.fanfan.apexsports.exception.BusinessException;
import com.fanfan.apexsports.exception.ErrorCode;
import com.fanfan.apexsports.services.RefereeAssignmentService;
import com.fanfan.apexsports.vos.RefereeAssignmentVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 裁判分配业务实现
 * <p>
 * 实现为比赛场次分配裁判（含裁判可用性、时间冲突、重复分配、主裁判唯一性等校验）、
 * 查询某场比赛的裁判分配列表（批量填充裁判姓名）、移除裁判分配（含比赛状态校验）等业务逻辑。
 * 分配与移除操作均需要管理员权限，列表查询对所有角色开放。
 * </p>
 * <p>
 * 写操作完成后驱逐秩序册缓存，因为裁判分配影响秩序册的 chiefReferee 聚合字段
 * </p>
 *
 * @author fanfan187
 * @version v1.0.0
 * @since v1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RefereeAssignmentLogic implements RefereeAssignmentService {

    private final RefereeAssignmentDAO refereeAssignmentDAO;
    private final CompetitionDAO competitionDAO;
    private final RefereeDAO refereeDAO;

    /**
     * 为比赛场次分配裁判
     * <p>
     * 校验链：管理员权限 → 比赛存在 → 裁判存在且可用 → 裁判时间不冲突 →
     * 未重复分配 → 主裁判唯一性（role=1 时）
     * </p>
     *
     * @param competitionId 比赛场次ID
     * @param dto           分配裁判请求数据
     * @return 新创建的裁判分配 VO
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public RefereeAssignmentVO assign(Long competitionId, AssignRefereeDTO dto) {
        checkAdmin();

        // 1. 校验比赛场次存在
        Competition competition = competitionDAO.getById(competitionId);
        if (competition == null) {
            throw new BusinessException(ErrorCode.COMPETITION_NOT_FOUND);
        }

        // 2. 校验裁判存在且状态可用
        Referee referee = refereeDAO.getById(dto.getRefereeId());
        if (referee == null) {
            throw new BusinessException(ErrorCode.REFEREE_NOT_FOUND);
        }
        if (!Objects.equals(RefereeConstants.STATUS_AVAILABLE, referee.getStatus())) {
            // 裁判状态非"可用"（status=1）
            throw new BusinessException(ErrorCode.REFEREE_UNAVAILABLE);
        }

        // 3. 裁判时间冲突校验：同一裁判在同一时间段只能被分配到一场比赛
        Long conflictCount = competitionDAO.countRefereeConflicts(
                dto.getRefereeId(),
                competition.getStartTime(),
                competition.getEndTime(),
                competitionId
        );
        if (conflictCount > 0) {
            throw new BusinessException(ErrorCode.REFEREE_TIME_CONFLICT);
        }

        // 4. 重复分配校验：同一裁判不可重复分配到同一场次
        RefereeAssignment existing = refereeAssignmentDAO.findByCompetitionIdAndRefereeId(
                competitionId, dto.getRefereeId()
        );
        if (existing != null) {
            throw new BusinessException(ErrorCode.REFEREE_ASSIGNMENT_DUPLICATE);
        }

        // 5. 主裁判唯一性校验：每场比赛只能有一名主裁判（role=1）
        if (CompetitionConstants.REFEREE_ROLE_CHIEF == dto.getRole()) {
            Long chiefCount = competitionDAO.countChiefReferee(competitionId);
            if (chiefCount > 0) {
                throw new BusinessException(ErrorCode.CHIEF_REFEREE_EXISTS);
            }
        }

        // 6. 构建实体并插入（DAO 内部填充 status=1、审计字段）
        RefereeAssignment assignment = new RefereeAssignment()
                .setCompetitionId(competitionId)
                .setRefereeId(dto.getRefereeId())
                .setRole(dto.getRole());
        RefereeAssignment inserted = refereeAssignmentDAO.insert(assignment);

        // 7. 驱逐秩序册缓存（裁判分配影响 chiefReferee 字段）
        competitionDAO.evictAllBooklets();

        log.info("分配裁判: assignmentId={}, competitionId={}, refereeId={}, role={}",
                inserted.getId(), competitionId, dto.getRefereeId(), dto.getRole());
        return RefereeAssignmentVO.from(inserted, referee.getName());
    }

    /**
     * 查询某场比赛的全部裁判分配列表
     * <p>
     * 所有角色均可查询，按角色升序返回。批量查询裁判姓名，避免 N+1 查询
     * </p>
     *
     * @param competitionId 比赛场次ID
     * @return 裁判分配 VO 列表
     */
    @Override
    public List<RefereeAssignmentVO> listByCompetitionId(Long competitionId) {
        List<RefereeAssignment> assignments = refereeAssignmentDAO.findByCompetitionId(competitionId);
        if (assignments.isEmpty()) {
            return List.of();
        }

        // 批量查询裁判姓名，构建 refereeId → name 映射
        List<Long> refereeIds = assignments.stream()
                .map(RefereeAssignment::getRefereeId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        Map<Long, String> refereeNameMap = refereeDAO.findByIds(refereeIds).stream()
                .collect(Collectors.toMap(Referee::getId, Referee::getName, (a, b) -> a));

        return assignments.stream()
                .map(a -> RefereeAssignmentVO.from(a, refereeNameMap.get(a.getRefereeId())))
                .toList();
    }

    /**
     * 移除裁判分配（逻辑删除）
     * <p>
     * 校验链：管理员权限 → 比赛存在 → 比赛未结束（status=2 时禁止）→ 分配记录存在。
     * 移除后驱逐秩序册缓存
     * </p>
     *
     * @param competitionId 比赛场次ID
     * @param assignmentId  裁判分配记录ID
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void remove(Long competitionId, Long assignmentId) {
        checkAdmin();

        // 1. 校验比赛场次存在
        Competition competition = competitionDAO.getById(competitionId);
        if (competition == null) {
            throw new BusinessException(ErrorCode.COMPETITION_NOT_FOUND);
        }

        // 2. 比赛状态校验：已结束（status=2）时禁止移除裁判
        if (CompetitionConstants.STATUS_FINISHED == competition.getStatus()) {
            throw new BusinessException(ErrorCode.COMPETITION_FINISHED);
        }

        // 3. 校验分配记录存在
        RefereeAssignment assignment = refereeAssignmentDAO.getById(assignmentId);
        if (assignment == null) {
            throw new BusinessException(ErrorCode.REFEREE_ASSIGNMENT_NOT_FOUND);
        }

        // 4. 逻辑删除并驱逐秩序册缓存
        refereeAssignmentDAO.deleteById(assignmentId);
        competitionDAO.evictAllBooklets();

        log.info("移除裁判分配: assignmentId={}, competitionId={}", assignmentId, competitionId);
    }

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
}
