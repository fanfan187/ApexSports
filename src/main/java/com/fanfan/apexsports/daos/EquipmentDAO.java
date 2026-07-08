package com.fanfan.apexsports.daos;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fanfan.apexsports.entities.Equipment;
import com.fanfan.apexsports.mappers.EquipmentMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 器材数据调度层
 * <p>
 * 协调 {@link EquipmentMapper} 与 {@link JdbcTemplate}，负责器材数据的存储、查询和调度。
 * 作为普通 Spring Bean（{@code @Component}），不继承 ServiceImpl，
 * 内部手动填充 createdAt / updatedAt（项目无 MetaObjectHandler），
 * 统一通过 LambdaQueryWrapper 构建条件查询。
 * </p>
 * <p>
 * 除常规 CRUD 外，额外提供：
 * <ul>
 *   <li>{@link #countByCode} / {@link #countByCodeExcludeId}：用于 code 唯一性校验</li>
 *   <li>{@link #updateTotalQuantity}：库存联动原子更新（条件 SQL 防超卖）</li>
 *   <li>{@link #countUnreturnedByEquipmentId}：删除前占用检查（JdbcTemplate 原生 SQL）</li>
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
    private final JdbcTemplate jdbcTemplate;

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
     * 注意：as_competition_equipment 表当前无 Entity/Mapper，使用 JdbcTemplate 原生 SQL。
     * 待赛程模块(09)开发时，该表将由赛程模块建立完整分层，本方法届时可重构。
     * </p>
     *
     * @param equipmentId 器材ID
     * @return 未归还（status != 2）的记录数
     */
    public Long countUnreturnedByEquipmentId(Long equipmentId) {
        return jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM as_competition_equipment WHERE equipment_id = ? AND status != 2 AND deleted = 0",
                Long.class, equipmentId
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
     * 更新器材总数量，同步调整可用数量（条件 SQL 防超卖）。
     * <p>
     * <b>重要约束：</b>本方法只处理"管理员编辑器材"场景。
     * 赛程模块的器材借还（扣减/恢复 available_quantity）应由赛程模块自己的
     * DAO 实现，使用类似的条件 SQL 模式，确保 available_quantity 不会为负。
     * </p>
     * <p>
     * TODO(赛程模块): 当赛程模块开发时，参考本方法的条件 SQL 模式，
     * 实现 CompetitionEquipmentDAO.deductAvailable() / restoreAvailable()。
     * </p>
     *
     * @param id       器材ID
     * @param newTotal 新的总数量
     * @param oldTotal 旧的总数量（用于计算差值）
     * @return true=更新成功，false=库存不足（available + diff &lt; 0）
     */
    public boolean updateTotalQuantity(Long id, int newTotal, int oldTotal) {
        int diff = newTotal - oldTotal;
        int rows = jdbcTemplate.update(
                "UPDATE as_equipment SET total_quantity = ?, available_quantity = available_quantity + ?, updated_at = NOW() " +
                        "WHERE id = ? AND deleted = 0 AND available_quantity + ? >= 0",
                newTotal, diff, id, diff
        );
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
     * 扣减器材可用库存（赛程模块分配器材时调用，条件 SQL 防超卖）。
     * <p>
     * 原子更新 {@code available_quantity = available_quantity - ?}，仅在扣减后非负时成功。
     * 供赛程模块 {@code CompetitionEquipmentLogic} 跨模块调用，确保分配记录插入与库存扣减在同一事务内原子完成。
     * </p>
     *
     * @param id       器材ID
     * @param quantity 扣减数量（必须 > 0）
     * @return true=扣减成功，false=库存不足
     */
    public boolean deductAvailable(Long id, int quantity) {
        int rows = jdbcTemplate.update(
                "UPDATE as_equipment SET available_quantity = available_quantity - ?, updated_at = NOW() " +
                        "WHERE id = ? AND deleted = 0 AND available_quantity - ? >= 0",
                quantity, id, quantity
        );
        return rows > 0;
    }

    /**
     * 恢复器材可用库存（赛程模块归还器材时调用）。
     * <p>
     * 原子更新 {@code available_quantity = available_quantity + ?}。供赛程模块
     * {@code CompetitionEquipmentLogic} 跨模块调用，确保状态更新与库存恢复在同一事务内原子完成。
     * </p>
     *
     * @param id       器材ID
     * @param quantity 恢复数量（必须 > 0）
     */
    public void restoreAvailable(Long id, int quantity) {
        jdbcTemplate.update(
                "UPDATE as_equipment SET available_quantity = available_quantity + ?, updated_at = NOW() " +
                        "WHERE id = ? AND deleted = 0",
                quantity, id
        );
    }
}
