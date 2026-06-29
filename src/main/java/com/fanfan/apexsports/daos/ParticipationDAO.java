package com.fanfan.apexsports.daos;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fanfan.apexsports.entities.Competition;
import com.fanfan.apexsports.entities.Participation;
import com.fanfan.apexsports.mappers.CompetitionMapper;
import com.fanfan.apexsports.mappers.ParticipationMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Collection;

/**
 * 参赛记录数据调度层
 * <p>
 * 协调 {@link ParticipationMapper} 与 {@link CompetitionMapper}，负责参赛记录的存储、查询和调度。
 * 作为普通 Spring Bean，不继承 ServiceImpl，内部自动填充创建时间、更新时间等字段。
 * </p>
 * <p>
 * 设计说明：Participation 表不含 collegeId 字段，按学院筛选时由 Logic 层先查询
 * AthleteDAO 获取该学院的运动员ID集合，再通过 {@link #page} 的 {@code athleteIds} 参数传入筛选
 * </p>
 *
 * @author fanfan187
 * @version v1.0.0
 * @since v1.0.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ParticipationDAO {

    private final ParticipationMapper participationMapper;
    private final CompetitionMapper competitionMapper;

    // ==================== 查询操作 ====================

    /**
     * 分页查询参赛记录（支持多条件筛选）
     * <p>
     * 注：collegeId 筛选由 Logic 层转换为 athleteIds 后传入；athleteIds 为空集合或 null 时不筛选
     * </p>
     *
     * @param pageNum       当前页码
     * @param pageSize      每页大小
     * @param eventId       项目ID筛选（可选）
     * @param athleteIds    运动员ID集合筛选（可选，来源于 collegeId → athleteDAO 的转换）
     * @param status        状态筛选（可选）
     * @param competitionId 比赛场次ID筛选（可选）
     * @return 分页结果
     */
    public Page<Participation> page(int pageNum, int pageSize, Long eventId,
                                    Collection<Long> athleteIds, Integer status, Long competitionId) {
        LambdaQueryWrapper<Participation> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(eventId != null, Participation::getEventId, eventId)
                .in(athleteIds != null && !athleteIds.isEmpty(), Participation::getAthleteId, athleteIds)
                .eq(status != null, Participation::getStatus, status)
                .eq(competitionId != null, Participation::getCompetitionId, competitionId)
                .orderByDesc(Participation::getCreatedAt);
        return participationMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
    }

    /**
     * 根据 ID 查询参赛记录
     *
     * @param id 参赛记录ID
     * @return 参赛记录实体，不存在返回 null
     */
    public Participation getById(Long id) {
        return participationMapper.selectById(id);
    }

    /**
     * 统计该运动员未删除的个人报名数（@TableLogic 自动过滤 deleted=1）
     *
     * @param athleteId 运动员ID
     * @return 该运动员的有效报名记录数
     */
    public Long countByAthleteId(Long athleteId) {
        return participationMapper.selectCount(
                new LambdaQueryWrapper<Participation>().eq(Participation::getAthleteId, athleteId)
        );
    }

    /**
     * 查询当前有效的同 athlete+event 报名记录
     * <p>
     * 用于唯一性校验：同一运动员对同一项目仅允许一条有效报名记录。
     * {@code @TableLogic} 会自动追加 deleted=0 条件
     * </p>
     *
     * @param athleteId 运动员ID
     * @param eventId   项目ID
     * @return 已存在的报名记录，不存在返回 null
     */
    public Participation findByAthleteIdAndEventId(Long athleteId, Long eventId) {
        return participationMapper.selectOne(
                new LambdaQueryWrapper<Participation>()
                        .eq(Participation::getAthleteId, athleteId)
                        .eq(Participation::getEventId, eventId)
        );
    }

    // ==================== 写入操作 ====================

    /**
     * 插入参赛记录（自动填充创建时间、更新时间）
     *
     * @param participation 参赛记录实体（不含主键和审计字段）
     * @return 插入后的参赛记录实体（含自动生成的主键）
     */
    public Participation insert(Participation participation) {
        participation.setCreatedAt(LocalDateTime.now());
        participation.setUpdatedAt(LocalDateTime.now());
        participationMapper.insert(participation);
        // 从数据库重新提取，确保字段完整
        return participationMapper.selectById(participation.getId());
    }

    /**
     * 更新参赛记录（自动更新修改时间）
     *
     * @param participation 参赛记录实体
     * @return 更新后的参赛记录实体
     */
    public Participation update(Participation participation) {
        participation.setUpdatedAt(LocalDateTime.now());
        participationMapper.updateById(participation);
        return participationMapper.selectById(participation.getId());
    }

    /**
     * 逻辑删除参赛记录（MyBatis-Plus 自动处理 @TableLogic）
     *
     * @param id 参赛记录ID
     */
    public void deleteById(Long id) {
        participationMapper.deleteById(id);
    }

    // ==================== 代理查询 ====================

    /**
     * 代理查询比赛场次（供 Logic 层做检录校验使用）
     * <p>
     * 检录时需校验比赛场次的 status 是否处于允许检录的状态，避免 Logic 层直接调用 Mapper
     * </p>
     *
     * @param competitionId 比赛场次ID
     * @return 比赛场次实体，不存在返回 null
     */
    public Competition getCompetitionById(Long competitionId) {
        return competitionMapper.selectById(competitionId);
    }
}
