package com.fanfan.apexsports.logic;

import com.fanfan.apexsports.config.UserContext;
import com.fanfan.apexsports.constants.EquipmentConstants;
import com.fanfan.apexsports.constants.UserConstants;
import com.fanfan.apexsports.daos.EquipmentDAO;
import com.fanfan.apexsports.dtos.EquipmentCreateDTO;
import com.fanfan.apexsports.dtos.EquipmentUpdateDTO;
import com.fanfan.apexsports.entities.Equipment;
import com.fanfan.apexsports.exception.BusinessException;
import com.fanfan.apexsports.exception.ErrorCode;
import com.fanfan.apexsports.services.EquipmentService;
import com.fanfan.apexsports.vos.EquipmentVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 器材业务实现
 * <p>
 * 实现器材的列表查询、详情查询、创建（含 code 唯一性校验、availableQuantity = totalQuantity 初始化、状态默认可用）、
 * 修改（部分更新语义、code 修改排除自身、totalQuantity 修改通过原子条件 SQL 联动调整 availableQuantity，防并发超卖）、
 * 删除（含未归还记录关联校验）等业务逻辑。
 * 所有写操作均需要管理员权限，读操作对所有角色开放
 * </p>
 *
 * @author fanfan187
 * @version v1.0.0
 * @since v1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EquipmentLogic implements EquipmentService {

    private final EquipmentDAO equipmentDAO;

    /**
     * 全量查询器材列表（不分页）
     * <p>
     * 所有角色均可查询，无需管理员权限。直接将 DO 列表转换为 VO 列表
     * </p>
     *
     * @param type   器材类型筛选（可选，字符串类型）
     * @param status 状态筛选（可选）
     * @return 器材 VO 列表
     */
    @Override
    public List<EquipmentVO> list(String type, Integer status) {
        return equipmentDAO.findAll(type, status).stream()
                .map(EquipmentVO::from)
                .toList();
    }

    /**
     * 根据 ID 查询器材详情
     * <p>
     * 所有角色均可查询
     * </p>
     *
     * @param id 器材ID
     * @return 器材 VO
     */
    @Override
    public EquipmentVO getById(Long id) {
        Equipment equipment = getEquipmentOrThrow(id);
        return EquipmentVO.from(equipment);
    }

    /**
     * 新增器材
     * <p>
     * 需要管理员权限，校验 code 唯一性。
     * 新增时 availableQuantity 初始化等于 totalQuantity，状态默认为可用（1）
     * </p>
     *
     * @param dto 新增器材请求数据
     * @return 新创建的器材 VO
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public EquipmentVO create(EquipmentCreateDTO dto) {
        checkAdmin();

        // 校验 code 唯一性
        Long count = equipmentDAO.countByCode(dto.getCode());
        if (count > 0) {
            throw new BusinessException(ErrorCode.EQUIPMENT_CODE_EXISTS);
        }

        // 构建实体：availableQuantity = totalQuantity（初始可用 = 总数量），status 默认可用
        Equipment equipment = new Equipment()
                .setName(dto.getName())
                .setCode(dto.getCode())
                .setType(dto.getType())
                .setUnit(dto.getUnit())
                .setTotalQuantity(dto.getTotalQuantity())
                .setAvailableQuantity(dto.getTotalQuantity());
        equipment.setStatus(EquipmentConstants.STATUS_AVAILABLE);

        Equipment inserted = equipmentDAO.insert(equipment);
        log.info("创建器材: id={}, code={}", inserted.getId(), inserted.getCode());
        return EquipmentVO.from(inserted);
    }

    /**
     * 修改器材信息
     * <p>
     * 需要管理员权限，支持部分更新语义。修改 code 时排除自身进行唯一性校验，
     * 修改 totalQuantity 时通过原子条件 SQL 同步调整 availableQuantity（差额更新，防并发超卖），
     * 其他字段（name / type / unit / status）通过 {@code updateBasicFields} 更新
     * </p>
     *
     * @param id  器材ID
     * @param dto 修改器材请求数据
     * @return 修改后的器材 VO
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public EquipmentVO update(Long id, EquipmentUpdateDTO dto) {
        checkAdmin();

        Equipment equipment = getEquipmentOrThrow(id);

        // 部分更新：仅更新非空字段
        if (StringUtils.hasText(dto.getName())) {
            equipment.setName(dto.getName());
        }
        if (StringUtils.hasText(dto.getType())) {
            equipment.setType(dto.getType());
        }
        if (StringUtils.hasText(dto.getUnit())) {
            equipment.setUnit(dto.getUnit());
        }
        if (dto.getStatus() != null) {
            equipment.setStatus(dto.getStatus());
        }
        if (StringUtils.hasText(dto.getCode())) {
            // 校验 code 唯一性（排除自身）
            Long count = equipmentDAO.countByCodeExcludeId(dto.getCode(), id);
            if (count > 0) {
                throw new BusinessException(ErrorCode.EQUIPMENT_CODE_EXISTS);
            }
            equipment.setCode(dto.getCode());
        }

        // ⭐ 库存联动：仅当 totalQuantity 被修改时触发原子条件 SQL 更新（防并发超卖）
        if (dto.getTotalQuantity() != null) {
            int oldTotal = equipment.getTotalQuantity();
            int newTotal = dto.getTotalQuantity();
            // 条件 SQL：available_quantity + diff >= 0 才更新，否则库存不足
            boolean success = equipmentDAO.updateTotalQuantity(id, newTotal, oldTotal);
            if (!success) {
                // available + diff < 0，库存不足
                throw new BusinessException(ErrorCode.EQUIPMENT_QUANTITY_INVALID);
            }
            // 同步内存对象（DB 已更新）—— 必须同时更新 availableQuantity，
            // 否则后续 updateBasicFields→updateById 会用旧值覆盖 DB 中已正确同步的 available_quantity
            int diff = newTotal - oldTotal;
            equipment.setTotalQuantity(newTotal);
            equipment.setAvailableQuantity(equipment.getAvailableQuantity() + diff);
        }

        // 更新基础字段（不含 totalQuantity / availableQuantity，这两个已在上面单独处理）
        equipmentDAO.updateBasicFields(equipment);
        log.info("修改器材: id={}", id);
        // 重新查询，返回最新数据
        return EquipmentVO.from(equipmentDAO.getById(id));
    }

    /**
     * 删除器材（逻辑删除）
     * <p>
     * 需要管理员权限，若该器材存在未归还的使用记录
     * （as_competition_equipment 中 status != 2）则禁止删除
     * </p>
     *
     * @param id 器材ID
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteById(Long id) {
        checkAdmin();

        getEquipmentOrThrow(id);

        // 引用完整性：检查 as_competition_equipment 表未归还记录
        Long usageCount = equipmentDAO.countUnreturnedByEquipmentId(id);
        if (usageCount > 0) {
            throw new BusinessException(ErrorCode.EQUIPMENT_IN_USE);
        }

        equipmentDAO.deleteById(id);
        log.info("删除器材: id={}", id);
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
     * 获取器材，不存在则抛异常
     *
     * @param id 器材ID
     * @return 器材实体
     */
    private Equipment getEquipmentOrThrow(Long id) {
        Equipment equipment = equipmentDAO.getById(id);
        if (equipment == null) {
            throw new BusinessException(ErrorCode.EQUIPMENT_NOT_FOUND);
        }
        return equipment;
    }
}
