package com.fanfan.apexsports.daos;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.fanfan.apexsports.entities.CompetitionEquipment;
import com.fanfan.apexsports.entities.Equipment;
import com.fanfan.apexsports.mappers.CompetitionEquipmentMapper;
import com.fanfan.apexsports.mappers.EquipmentMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 器材数据调度层
 * <p>
 * 协调 {@link EquipmentMapper} 与 {@link CompetitionEquipmentMapper}，负责器材数据的存储、查询和调度。
 * 作为普通 Spring Bean（{@code @Component}），不继承 ServiceImpl，
 * 内部手动填充 createdAt / updatedAt（项目无 MetaObjectHandler），
 * 统一通过 LambdaQueryWrapper 构建条件查询。
 * </p>
 * <p>
 * 除常规 CRUD 外，额外提供：
 * <ul>
 *   <li>{@link #countByCode} / {@link #countByCodeExcludeId}：用于 code 唯一性校验</li>
 *   <li>{@link #updateTotalQuantity}：库存联动原子更新（{@code setSql} + {@code apply} 条件防超卖）</li>
 *   <li>{@link #countUnreturnedByEquipmentId}：删除前占用检查（MBP selectCount，跨表查询 CompetitionEquipmentMapper）</li>
 *   <li>{@link #deductAvailable} / {@link #restoreAvailable}：赛程模块跨模块调用的库存扣减/恢复（条件 SQL 防超卖）</li>
 * </ul>
 * </p>
 *
 * @author fanfan187
 * @version v1.0.0
 * @since v1.0.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EquipmentDAO {

    private final EquipmentMapper equipmentMapper;
    private final CompetitionEquipmentMapper competitionEquipmentMapper;

    // ==================== 查询操作 ====================

    /**
     * 全量查询器材列表（不分页），支持 type / status 可选筛选。
     * <p>
     * 两个筛选参数均为可选（null 表示不过滤），结果按 {@code createdAt DESC} 排序。
     * 不做业务校验，不转 VO，直接返回 DO 列表，由 Logic 层负责 DO→VO 转换。
     * </p>
     *
     * @param type   器材类型筛选（可选，字符串类型）
     * @param status 状态筛选（可选）：0-停用 / 1-可用
     * @return 器材实体列表（按创建时间倒序）
     */
    public List<Equipment> findAll(String type, Integer status) {
        LambdaQueryWrapper<Equipment> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(type != null, Equipment::getType, type)
                .eq(status != null, Equipment::getStatus, status)
                .orderByDesc(Equipment::getCreatedAt);
        return equipmentMapper.selectList(wrapper);
    }

    /**
     * 根据 ID 查询器材
     *
     * @param id 器材ID
     * @return 器材实体，不存在返回 null
     */
    public Equipment getById(Long id) {
        return equipmentMapper.selectById(id);
    }

    /**
     * 统计指定 code 的未删除记录数（用于新增时唯一性校验）
     * <p>
     * MBP {@code @TableLogic} 会自动追加 {@code deleted = 0} 条件，
     * 因此只统计未逻辑删除的记录。
     * </p>
     *
     * @param code 器材代码
     * @return 记录数
     */
    public Long countByCode(String code) {
        return equipmentMapper.selectCount(
                new LambdaQueryWrapper<Equipment>().eq(Equipment::getCode, code)
        );
    }

    /**
     * 统计指定 code 的记录数，排除指定 ID（用于更新时唯一性校验）
     * <p>
     * 排除当前 ID 自身，避免更新时误判 code 与自身冲突。
     * </p>
     *
     * @param code       器材代码
     * @param excludeId  需排除的器材ID
     * @return 记录数
     */
    public Long countByCodeExcludeId(String code, Long excludeId) {
        return equipmentMapper.selectCount(
                new LambdaQueryWrapper<Equipment>()
                        .eq(Equipment::getCode, code)
                        .ne(Equipment::getId, excludeId)
        );
    }

    /**
     * 检查器材是否存在未归还的使用记录。
     * <p>
     * 通过 {@link CompetitionEquipmentMapper} 跨表查询 as_competition_equipment，
     * 统计指定器材的未归还（status != 2）记录数。
     * MBP {@code @TableLogic} 会自动追加 {@code deleted = 0} 条件，
     * 因此无需在 wrapper 中显式指定。
     * </p>
     *
     * @param equipmentId 器材ID
     * @return 未归还（status != 2）的记录数
     */
    public Long countUnreturnedByEquipmentId(Long equipmentId) {
        return competitionEquipmentMapper.selectCount(
                new LambdaQueryWrapper<CompetitionEquipment>()
                        .eq(CompetitionEquipment::getEquipmentId, equipmentId)
                        .ne(CompetitionEquipment::getStatus, 2)
        );
    }

    // ==================== 写入操作 ====================

    /**
     * 插入器材（手动填充 createdAt / updatedAt）
     * <p>
     * 项目未配置 MyBatis-Plus 的 MetaObjectHandler，需在此手动填充审计字段。
     * Equipment 实体使用 {@code @Accessors(chain = true)}，支持链式 setter。
     * insert 后重新查询以确保返回 DB 自动生成字段（主键等）的完整对象。
     * </p>
     *
     * @param equipment 器材实体（不含主键和审计字段）
     * @return 插入后的器材实体（含自动生成的主键），若插入失败可能返回 null
     */
    public Equipment insert(Equipment equipment) {
        LocalDateTime now = LocalDateTime.now();
        equipment.setCreatedAt(now).setUpdatedAt(now);
        equipmentMapper.insert(equipment);
        // 从数据库重新提取，确保字段完整
        return equipmentMapper.selectById(equipment.getId());
    }

    /**
     * 更新器材总数量，同步调整可用数量（{@code setSql} + {@code apply} 条件防超卖）。
     * <p>
     * 使用 {@link LambdaUpdateWrapper#setSql} 拼接原子 SET 子句，配合
     * {@link LambdaUpdateWrapper#apply} 在 WHERE 中追加 {@code available_quantity + diff >= 0}
     * 条件，若扣减后为负则 0 行受影响，返回 false，实现乐观并发下的防超卖语义。
     * </p>
     * <p>
     * <b>重要约束：</b>本方法只处理"管理员编辑器材"场景。
     * 赛程模块的器材借还（扣减/恢复 available_quantity）由本 DAO 的
     * {@link #deductAvailable} / {@link #restoreAvailable} 提供。
     * </p>
     *
     * @param id       器材ID
     * @param newTotal 新的总数量
     * @param oldTotal 旧的总数量（用于计算差值）
     * @return true=更新成功，false=库存不足（available + diff &lt; 0）
     */
    public boolean updateTotalQuantity(Long id, int newTotal, int oldTotal) {
        int diff = newTotal - oldTotal;
        LambdaUpdateWrapper<Equipment> wrapper = new LambdaUpdateWrapper<Equipment>()
                .eq(Equipment::getId, id)
                .setSql("total_quantity = " + newTotal)
                .setSql("available_quantity = available_quantity + " + diff)
                .setSql("updated_at = NOW()")
                .apply("available_quantity + {0} >= 0", diff);
        int rows = equipmentMapper.update(null, wrapper);
        return rows > 0;
    }

    /**
     * 更新器材基础字段（name / code / type / unit / status），自动填充 updatedAt。
     * <p>
     * 仅刷新这些基础字段，<b>不更新</b> totalQuantity / availableQuantity，
     * 库存数量由 {@link #updateTotalQuantity} 单独原子处理。
     * </p>
     *
     * @param equipment 器材实体（必须含主键及需更新的基础字段）
     */
    public void updateBasicFields(Equipment equipment) {
        equipment.setUpdatedAt(LocalDateTime.now());
        equipmentMapper.updateById(equipment);
    }

    /**
     * 逻辑删除器材（MyBatis-Plus 自动处理 {@code @TableLogic}）
     *
     * @param id 器材ID
     */
    public void deleteById(Long id) {
        equipmentMapper.deleteById(id);
    }

    /**
     * 扣减器材可用库存（赛程模块分配器材时调用，{@code setSql} + {@code apply} 条件防超卖）。
     * <p>
     * 使用 {@link LambdaUpdateWrapper#setSql} 拼接原子 SET 子句，配合
     * {@link LambdaUpdateWrapper#apply} 在 WHERE 中追加 {@code available_quantity - quantity >= 0}
     * 条件，仅在扣减后非负时成功；若库存不足则 0 行受影响，返回 false。
     * 供赛程模块 {@code CompetitionEquipmentLogic} 跨模块调用，确保分配记录插入与库存扣减
     * 在同一事务内原子完成。
     * </p>
     *
     * @param id       器材ID
     * @param quantity 扣减数量（必须 > 0）
     * @return true=扣减成功，false=库存不足
     */
    public boolean deductAvailable(Long id, int quantity) {
        LambdaUpdateWrapper<Equipment> wrapper = new LambdaUpdateWrapper<Equipment>()
                .eq(Equipment::getId, id)
                .setSql("available_quantity = available_quantity - " + quantity)
                .setSql("updated_at = NOW()")
                .apply("available_quantity - {0} >= 0", quantity);
        int rows = equipmentMapper.update(null, wrapper);
        return rows > 0;
    }

    /**
     * 恢复器材可用库存（赛程模块归还器材时调用）。
     * <p>
     * 使用 {@link LambdaUpdateWrapper#setSql} 拼接原子 SET 子句，执行
     * {@code available_quantity = available_quantity + quantity}。恢复操作无库存上限约束，
     * 故不需要 {@code apply} 条件。供赛程模块 {@code CompetitionEquipmentLogic} 跨模块调用，
     * 确保状态更新与库存恢复在同一事务内原子完成。
     * </p>
     *
     * @param id       器材ID
     * @param quantity 恢复数量（必须 > 0）
     */
    public void restoreAvailable(Long id, int quantity) {
        LambdaUpdateWrapper<Equipment> wrapper = new LambdaUpdateWrapper<Equipment>()
                .eq(Equipment::getId, id)
                .setSql("available_quantity = available_quantity + " + quantity)
                .setSql("updated_at = NOW()");
        equipmentMapper.update(null, wrapper);
    }
}
