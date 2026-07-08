package com.fanfan.apexsports.logic;

import com.fanfan.apexsports.config.UserContext;
import com.fanfan.apexsports.constants.CompetitionConstants;
import com.fanfan.apexsports.constants.UserConstants;
import com.fanfan.apexsports.daos.CompetitionDAO;
import com.fanfan.apexsports.daos.CompetitionEquipmentDAO;
import com.fanfan.apexsports.daos.EquipmentDAO;
import com.fanfan.apexsports.dtos.AssignEquipmentDTO;
import com.fanfan.apexsports.dtos.UpdateEquipmentStatusDTO;
import com.fanfan.apexsports.entities.Competition;
import com.fanfan.apexsports.entities.CompetitionEquipment;
import com.fanfan.apexsports.entities.Equipment;
import com.fanfan.apexsports.exception.BusinessException;
import com.fanfan.apexsports.exception.ErrorCode;
import com.fanfan.apexsports.services.CompetitionEquipmentService;
import com.fanfan.apexsports.vos.CompetitionEquipmentVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 赛程器材分配业务实现
 * <p>
 * 实现为比赛场次分配器材（含库存校验与原子扣减）、查询某场次的器材使用列表、
 * 更新器材状态（已分配→使用中→已归还，归还时恢复库存）等业务逻辑。
 * 写操作均需要管理员权限，读操作对所有角色开放。
 * </p>
 * <p>
 * <b>架构红线：</b>Logic 层不直接调用 Mapper（通过 DAO），不直接操作缓存（通过 Cache 层），
 * 不出现缓存/存储层相关字样。库存扣减/恢复通过 {@link EquipmentDAO} 跨模块协作完成，
 * 与分配记录写入/状态更新在同一事务内原子完成。
 * </p>
 *
 * @author fanfan187
 * @version v1.0.0
 * @since v1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CompetitionEquipmentLogic implements CompetitionEquipmentService {

    private final CompetitionEquipmentDAO competitionEquipmentDAO;
    private final CompetitionDAO competitionDAO;
    private final EquipmentDAO equipmentDAO;

    /**
     * 为比赛场次分配器材，同步扣减库存
     * <p>
     * 需要管理员权限。校验比赛场次存在性、器材存在性与可用状态、库存充足性，
     * 随后插入分配记录（状态默认为已分配）并原子扣减器材可用库存，
     * 最后驱逐秩序册缓存以保证聚合视图新鲜度。整个流程在同一事务内完成。
     * </p>
     *
     * @param competitionId 比赛场次ID
     * @param dto           分配器材请求数据
     * @return 新创建的器材分配 VO
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public CompetitionEquipmentVO assign(Long competitionId, AssignEquipmentDTO dto) {
        checkAdmin();

        // 校验 1：比赛场次是否存在
        Competition competition = competitionDAO.getById(competitionId);
        if (competition == null) {
            throw new BusinessException(ErrorCode.COMPETITION_NOT_FOUND);
        }

        // 校验 2：器材是否存在
        Equipment equipment = equipmentDAO.getById(dto.getEquipmentId());
        if (equipment == null) {
            throw new BusinessException(ErrorCode.EQUIPMENT_NOT_FOUND);
        }

        // 校验 3：器材状态必须为可用（status=1）
        if (equipment.getStatus() == null || equipment.getStatus() != 1) {
            throw new BusinessException(ErrorCode.EQUIPMENT_DISABLED);
        }

        // 校验 4：库存充足性（quantity ≤ availableQuantity）
        Integer available = equipment.getAvailableQuantity();
        if (available == null || dto.getQuantity() > available) {
            throw new BusinessException(ErrorCode.EQUIPMENT_QUANTITY_INVALID);
        }

        // 构建分配记录实体（状态由 DAO 插入时强制置为已分配，此处显式设置以表达意图）
        CompetitionEquipment record = new CompetitionEquipment()
                .setCompetitionId(competitionId)
                .setEquipmentId(dto.getEquipmentId())
                .setQuantity(dto.getQuantity())
                .setRemark(dto.getRemark())
                .setStatus(CompetitionConstants.EQUIPMENT_STATUS_ASSIGNED);

        CompetitionEquipment inserted = competitionEquipmentDAO.insert(record);

        // 原子扣减库存（条件 SQL 防超卖，与记录插入同事务）
        boolean deducted = equipmentDAO.deductAvailable(dto.getEquipmentId(), dto.getQuantity());
        if (!deducted) {
            // 并发场景下库存被抢占，触发事务回滚
            throw new BusinessException(ErrorCode.EQUIPMENT_QUANTITY_INVALID);
        }

        // 驱逐秩序册缓存
        competitionDAO.evictAllBooklets();

        log.info("分配器材: recordId={}, competitionId={}, equipmentId={}, quantity={}",
                inserted.getId(), competitionId, dto.getEquipmentId(), dto.getQuantity());

        return CompetitionEquipmentVO.from(inserted, equipment.getName());
    }

    /**
     * 查询某场比赛的所有器材使用记录
     * <p>
     * 所有角色均可查询，无需管理员权限。批量查询器材名称以构建列表视图，避免 N+1 查询。
     * </p>
     *
     * @param competitionId 比赛场次ID
     * @return 器材使用记录列表，无记录返回空集合
     */
    @Override
    public List<CompetitionEquipmentVO> listByCompetitionId(Long competitionId) {
        List<CompetitionEquipment> records = competitionEquipmentDAO.findByCompetitionId(competitionId);
        if (records.isEmpty()) {
            return List.of();
        }

        // 批量查询器材名称（同一场次器材记录数量有限，按需缓存查询结果）
        Map<Long, String> equipmentNameMap = new HashMap<>();
        for (CompetitionEquipment record : records) {
            equipmentNameMap.computeIfAbsent(record.getEquipmentId(), id -> {
                Equipment equipment = equipmentDAO.getById(id);
                return equipment != null ? equipment.getName() : null;
            });
        }

        return records.stream()
                .map(record -> CompetitionEquipmentVO.from(record, equipmentNameMap.get(record.getEquipmentId())))
                .toList();
    }

    /**
     * 更新器材使用状态（已分配→使用中→已归还），归还时恢复库存
     * <p>
     * 需要管理员权限。校验记录存在性与归属、已归还不可再变更、状态流转合法性，
     * 若流转为已归还则原子恢复器材库存。整个流程在同一事务内完成。
     * </p>
     *
     * @param competitionId 比赛场次ID
     * @param recordId      器材使用记录ID
     * @param dto           状态更新请求数据
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateStatus(Long competitionId, Long recordId, UpdateEquipmentStatusDTO dto) {
        checkAdmin();

        CompetitionEquipment record = competitionEquipmentDAO.getById(recordId);
        if (record == null) {
            throw new BusinessException(ErrorCode.COMPETITION_EQUIPMENT_NOT_FOUND);
        }

        // 归属校验：记录必须属于该比赛场次
        if (record.getCompetitionId() == null
                || !record.getCompetitionId().equals(competitionId)) {
            throw new BusinessException(ErrorCode.COMPETITION_EQUIPMENT_NOT_FOUND);
        }

        int currentStatus = record.getStatus() == null
                ? CompetitionConstants.EQUIPMENT_STATUS_ASSIGNED : record.getStatus();
        int targetStatus = dto.getStatus();

        // 已归还的记录不可再次变更
        if (currentStatus == CompetitionConstants.EQUIPMENT_STATUS_RETURNED) {
            throw new BusinessException(ErrorCode.EQUIPMENT_RETURNED);
        }

        // 状态流转合法性：已分配(0)→使用中(1) 或 使用中(1)→已归还(2)
        boolean validTransition =
                (currentStatus == CompetitionConstants.EQUIPMENT_STATUS_ASSIGNED
                        && targetStatus == CompetitionConstants.EQUIPMENT_STATUS_IN_USE)
                || (currentStatus == CompetitionConstants.EQUIPMENT_STATUS_IN_USE
                        && targetStatus == CompetitionConstants.EQUIPMENT_STATUS_RETURNED);
        if (!validTransition) {
            throw new BusinessException(ErrorCode.PARAM_VALIDATION_FAILED);
        }

        // 归还时恢复库存
        if (targetStatus == CompetitionConstants.EQUIPMENT_STATUS_RETURNED) {
            int quantity = record.getQuantity() != null ? record.getQuantity() : 0;
            if (quantity > 0) {
                equipmentDAO.restoreAvailable(record.getEquipmentId(), quantity);
            }
        }

        record.setStatus(targetStatus);
        competitionEquipmentDAO.update(record);

        // 驱逐秩序册缓存
        competitionDAO.evictAllBooklets();

        log.info("更新器材状态: recordId={}, competitionId={}, {} -> {}",
                recordId, competitionId, currentStatus, targetStatus);
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
}
