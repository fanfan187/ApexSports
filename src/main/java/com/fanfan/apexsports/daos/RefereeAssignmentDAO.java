package com.fanfan.apexsports.daos;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fanfan.apexsports.entities.RefereeAssignment;
import com.fanfan.apexsports.mappers.RefereeAssignmentMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 裁判分配数据调度层
 * <p>
 * 协调 {@link RefereeAssignmentMapper}，负责裁判分配记录的存储、查询和调度。
 * 作为普通 Spring Bean，不继承 ServiceImpl，内部自动填充创建时间、更新时间、状态等字段。
 * </p>
 * <p>
 * 设计说明：裁判分配不加缓存（实时性要求高、低频读），全部走 DB；
 * 裁判自身状态校验（在职/离职等）由 Logic 层调用 RefereeDAO 负责，DAO 不越权
 * </p>
 *
 * @author fanfan187
 * @version v1.0.0
 * @since v1.0.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RefereeAssignmentDAO {

    private final RefereeAssignmentMapper refereeAssignmentMapper;

    // ==================== 查询操作 ====================

    /**
     * 查询某场比赛的全部裁判分配记录（按角色升序）
     * <p>
     * 排序规则：role 升序（主裁判 → 副裁判 → 计时员 → 记录员），
     * 便于 Logic 层组装 VO 时按角色权重展示
     * </p>
     *
     * @param competitionId 比赛场次ID
     * @return 该场次的裁判分配列表，按 role 升序；无记录返回空集合
     */
    public List<RefereeAssignment> findByCompetitionId(Long competitionId) {
        return refereeAssignmentMapper.selectList(
                new LambdaQueryWrapper<RefereeAssignment>()
                        .eq(RefereeAssignment::getCompetitionId, competitionId)
                        .orderByAsc(RefereeAssignment::getRole)
        );
    }

    /**
     * 根据 ID 查询裁判分配记录
     *
     * @param id 分配记录ID
     * @return 分配记录实体，不存在返回 null
     */
    public RefereeAssignment getById(Long id) {
        return refereeAssignmentMapper.selectById(id);
    }

    /**
     * 查询同一场次同一裁判的已存在分配记录
     * <p>
     * 用于唯一性校验：同一场次同一裁判仅允许一条有效分配记录。
     * {@code @TableLogic} 会自动追加 deleted=0 条件
     * </p>
     *
     * @param competitionId 比赛场次ID
     * @param refereeId     裁判ID
     * @return 已存在的分配记录，不存在返回 null
     */
    public RefereeAssignment findByCompetitionIdAndRefereeId(Long competitionId, Long refereeId) {
        return refereeAssignmentMapper.selectOne(
                new LambdaQueryWrapper<RefereeAssignment>()
                        .eq(RefereeAssignment::getCompetitionId, competitionId)
                        .eq(RefereeAssignment::getRefereeId, refereeId)
        );
    }

    /**
     * 统计某场比赛已分配的裁判数量（@TableLogic 自动过滤 deleted=1）
     *
     * @param competitionId 比赛场次ID
     * @return 该场次的有效裁判分配数
     */
    public Long countByCompetitionId(Long competitionId) {
        return refereeAssignmentMapper.selectCount(
                new LambdaQueryWrapper<RefereeAssignment>()
                        .eq(RefereeAssignment::getCompetitionId, competitionId)
        );
    }

    // ==================== 写入操作 ====================

    /**
     * 插入裁判分配记录（自动填充状态、创建时间、更新时间）
     * <p>
     * 默认 status=1（已分配）。插入后重新查询数据库，确保所有字段（含主键、审计字段）完整。
     * </p>
     *
     * @param assignment 裁判分配实体（不含主键和审计字段）
     * @return 插入后的裁判分配实体（含自动生成的主键）
     */
    public RefereeAssignment insert(RefereeAssignment assignment) {
        assignment.setStatus(1);
        assignment.setCreatedAt(LocalDateTime.now());
        assignment.setUpdatedAt(LocalDateTime.now());
        refereeAssignmentMapper.insert(assignment);
        // 从数据库重新提取，确保字段完整
        return refereeAssignmentMapper.selectById(assignment.getId());
    }

    /**
     * 逻辑删除裁判分配记录（MyBatis-Plus 自动处理 @TableLogic）
     *
     * @param id 分配记录ID
     */
    public void deleteById(Long id) {
        refereeAssignmentMapper.deleteById(id);
    }
}
