package com.fanfan.apexsports.daos;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fanfan.apexsports.entities.Competition;
import com.fanfan.apexsports.entities.TeamParticipation;
import com.fanfan.apexsports.mappers.CompetitionMapper;
import com.fanfan.apexsports.mappers.TeamParticipationMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 团体参赛记录数据调度层
 * <p>
 * 协调 TeamParticipationMapper 与 CompetitionMapper，负责团体参赛记录的存储、查询和调度。
 * 作为普通 Spring Bean，不继承 ServiceImpl，内部自动填充创建时间、更新时间等字段。
 * </p>
 * <p>
 * 关键差异（vs ParticipationDAO）：team_participation 表自带 college_id 字段，
 * 因此 page 方法可直接按 collegeId 筛选，无需 Logic 层做学院→学院ID 的转换。
 * </p>
 *
 * @author fanfan187
 * @version v1.0.0
 * @since v1.0.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TeamParticipationDAO {

    private final TeamParticipationMapper teamParticipationMapper;
    private final CompetitionMapper competitionMapper;

    // ==================== 查询操作 ====================

    /**
     * 分页查询团体参赛记录（支持多条件筛选）
     * <p>
     * 由于 team_participation 表自带 college_id 字段，可直接按 collegeId 筛选。
     * 支持按项目ID、学院ID、状态、比赛场次ID过滤，结果按创建时间倒序排列。
     * </p>
     *
     * @param pageNum       当前页码
     * @param pageSize      每页大小
     * @param eventId       项目ID（可空）
     * @param collegeId     学院ID（可空）
     * @param status        状态：0-已报名, 1-检录通过, 2-已参赛, 3-弃权, 4-取消资格（可空）
     * @param competitionId 比赛场次ID（可空）
     * @return 分页结果（MyBatis-Plus Page 对象）
     */
    public Page<TeamParticipation> page(int pageNum, int pageSize, Long eventId, Long collegeId,
                                        Integer status, Long competitionId) {
        LambdaQueryWrapper<TeamParticipation> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(eventId != null, TeamParticipation::getEventId, eventId)
                .eq(collegeId != null, TeamParticipation::getCollegeId, collegeId)
                .eq(status != null, TeamParticipation::getStatus, status)
                .eq(competitionId != null, TeamParticipation::getCompetitionId, competitionId)
                .orderByDesc(TeamParticipation::getCreatedAt);
        return teamParticipationMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
    }

    /**
     * 根据 ID 查询团体参赛记录
     *
     * @param id 团体参赛记录ID
     * @return 团体参赛记录实体，不存在返回 null
     */
    public TeamParticipation getById(Long id) {
        return teamParticipationMapper.selectById(id);
    }

    /**
     * 查询当前有效的同学院+项目报名记录（用于唯一性校验）
     * <p>
     * 由于 @TableLogic 注解，查询自动追加 deleted=0 条件
     * </p>
     *
     * @param collegeId 学院ID
     * @param eventId   项目ID
     * @return 已存在的报名记录，不存在返回 null
     */
    public TeamParticipation findByCollegeIdAndEventId(Long collegeId, Long eventId) {
        return teamParticipationMapper.selectOne(
                new LambdaQueryWrapper<TeamParticipation>()
                        .eq(TeamParticipation::getCollegeId, collegeId)
                        .eq(TeamParticipation::getEventId, eventId)
        );
    }

    // ==================== 写入操作 ====================

    /**
     * 插入团体参赛记录（内部自动填充 createdAt/updatedAt）
     *
     * @param teamParticipation 团体参赛记录实体（不含审计字段）
     * @return 插入后的团体参赛记录实体（含自动生成的主键）
     */
    public TeamParticipation insert(TeamParticipation teamParticipation) {
        teamParticipation.setCreatedAt(LocalDateTime.now());
        teamParticipation.setUpdatedAt(LocalDateTime.now());
        teamParticipationMapper.insert(teamParticipation);
        // 从数据库重新提取，确保字段完整
        return teamParticipationMapper.selectById(teamParticipation.getId());
    }

    /**
     * 更新团体参赛记录（内部自动填充 updatedAt）
     *
     * @param teamParticipation 团体参赛记录实体
     * @return 更新后的团体参赛记录实体
     */
    public TeamParticipation update(TeamParticipation teamParticipation) {
        teamParticipation.setUpdatedAt(LocalDateTime.now());
        teamParticipationMapper.updateById(teamParticipation);
        return teamParticipationMapper.selectById(teamParticipation.getId());
    }

    /**
     * 逻辑删除团体参赛记录（MyBatis-Plus 自动处理 @TableLogic）
     *
     * @param id 团体参赛记录ID
     */
    public void deleteById(Long id) {
        teamParticipationMapper.deleteById(id);
    }

    /**
     * 清空团体参赛记录的场次编排信息（competitionId 置 null）
     * <p>
     * 使用 {@link LambdaUpdateWrapper#set} 显式置 null，绕开 MyBatis-Plus
     * {@code updateById} 默认 {@code FieldStrategy.NOT_NULL} 策略对 null 字段的跳过问题。
     * </p>
     *
     * @param teamParticipationId 团体参赛记录ID
     */
    public void clearCompetitionArrangement(Long teamParticipationId) {
        LambdaUpdateWrapper<TeamParticipation> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(TeamParticipation::getId, teamParticipationId)
                .set(TeamParticipation::getCompetitionId, null)
                .set(TeamParticipation::getUpdatedAt, LocalDateTime.now());
        teamParticipationMapper.update(null, wrapper);
    }

    // ==================== 代理查询（供 Logic 层使用） ====================

    /**
     * 根据比赛场次ID查询比赛场次信息（供 Logic 层做检录校验用）
     * <p>
     * 代理调用 CompetitionMapper，避免 Logic 直接依赖 Mapper。
     * </p>
     *
     * @param competitionId 比赛场次ID
     * @return 比赛场次实体，不存在返回 null
     */
    public Competition getCompetitionById(Long competitionId) {
        return competitionMapper.selectById(competitionId);
    }
}
