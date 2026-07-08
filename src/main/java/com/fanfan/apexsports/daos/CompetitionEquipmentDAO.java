package com.fanfan.apexsports.daos;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.fanfan.apexsports.entities.CompetitionEquipment;
import com.fanfan.apexsports.mappers.CompetitionEquipmentMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 比赛器材分配数据调度层
 * <p>
 * 协调 {@link CompetitionEquipmentMapper}，负责某场比赛器材分配记录的存储、查询和调度。
 * 作为普通 Spring Bean，不继承 ServiceImpl，内部自动填充状态、创建时间、更新时间等字段。
 * </p>
 * <p>
 * 设计说明：本 DAO 只管 as_competition_equipment 表；库存扣减/恢复由 Logic 层调用 EquipmentDAO 完成（跨模块协作），
 * 本层不出现任何库存相关逻辑，也不引入 Cache。
 * </p>
 *
 * @author fanfan187
 * @version v1.0.0
 * @since v1.0.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CompetitionEquipmentDAO {

    /** 器材分配状态：已分配 */
    private static final int STATUS_ASSIGNED = 0;
    /** 器材分配状态：已归还 */
    private static final int STATUS_RETURNED = 2;

    private final CompetitionEquipmentMapper competitionEquipmentMapper;

    // ==================== 查询操作 ====================

    /**
     * 查询某场比赛的所有器材分配记录（按创建时间倒序）
     * <p>
     * {@code @TableLogic} 自动过滤已删除记录
     * </p>
     *
     * @param competitionId 比赛场次ID
     * @return 该场次的器材分配记录列表，无记录返回空集合
     */
    public List<CompetitionEquipment> findByCompetitionId(Long competitionId) {
        return competitionEquipmentMapper.selectList(
                new LambdaQueryWrapper<CompetitionEquipment>()
                        .eq(CompetitionEquipment::getCompetitionId, competitionId)
                        .orderByDesc(CompetitionEquipment::getCreatedAt)
        );
    }

    /**
     * 根据 ID 查询器材分配记录
     *
     * @param id 分配记录ID
     * @return 分配记录实体，不存在返回 null
     */
    public CompetitionEquipment getById(Long id) {
        return competitionEquipmentMapper.selectById(id);
    }

    // ==================== 写入操作 ====================

    /**
     * 插入器材分配记录（自动填充 status=0 已分配、创建时间、更新时间）
     *
     * @param equipment 器材分配记录实体（不含主键和审计字段）
     * @return 插入后的器材分配记录实体（含自动生成的主键）
     */
    public CompetitionEquipment insert(CompetitionEquipment equipment) {
        equipment.setStatus(STATUS_ASSIGNED);
        equipment.setCreatedAt(LocalDateTime.now());
        equipment.setUpdatedAt(LocalDateTime.now());
        competitionEquipmentMapper.insert(equipment);
        // 从数据库重新提取，确保字段完整
        return competitionEquipmentMapper.selectById(equipment.getId());
    }

    /**
     * 更新器材分配记录（自动更新修改时间）
     *
     * @param equipment 器材分配记录实体
     * @return 更新后的器材分配记录实体
     */
    public CompetitionEquipment update(CompetitionEquipment equipment) {
        equipment.setUpdatedAt(LocalDateTime.now());
        competitionEquipmentMapper.updateById(equipment);
        return competitionEquipmentMapper.selectById(equipment.getId());
    }

    /**
     * 逻辑删除某场比赛的所有器材分配记录（MyBatis-Plus 自动处理 @TableLogic）
     * <p>
     * 用于场次取消/删除时清理该场次的器材分配记录。库存恢复由 Logic 层调用 EquipmentDAO 完成。
     * </p>
     *
     * @param competitionId 比赛场次ID
     */
    public void deleteByCompetitionId(Long competitionId) {
        competitionEquipmentMapper.delete(
                new LambdaQueryWrapper<CompetitionEquipment>()
                        .eq(CompetitionEquipment::getCompetitionId, competitionId)
        );
    }

    /**
     * 批量将某场比赛的器材分配记录状态改为已归还(2)
     * <p>
     * 用于场次结束后批量归还器材。库存恢复由 Logic 层调用 EquipmentDAO 完成。
     * </p>
     *
     * @param competitionId 比赛场次ID
     */
    public void updateStatusToReturnedByCompetitionId(Long competitionId) {
        competitionEquipmentMapper.update(null,
                new LambdaUpdateWrapper<CompetitionEquipment>()
                        .eq(CompetitionEquipment::getCompetitionId, competitionId)
                        .set(CompetitionEquipment::getStatus, STATUS_RETURNED)
                        .set(CompetitionEquipment::getUpdatedAt, LocalDateTime.now())
        );
    }
}
