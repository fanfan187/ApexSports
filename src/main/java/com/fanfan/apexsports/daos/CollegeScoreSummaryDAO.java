package com.fanfan.apexsports.daos;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fanfan.apexsports.entities.CollegeScoreSummary;
import com.fanfan.apexsports.mappers.CollegeScoreSummaryMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 学院积分汇总数据调度层
 * <p>
 * 协调 {@link CollegeScoreSummaryMapper}，负责 as_college_score_summary 表的查询与
 * 增量更新/回滚。积分 delta 由成绩模块 Logic 层算好传入，DAO 只负责 apply 到汇总行，
 * 不做全量重算（全量重算由模块12 Logic 触发，本 DAO 不提供该能力）。
 * </p>
 * <p>
 * <b>方法语义：</b>
 * <ul>
 *   <li>{@link #upsertIncrement}：不存在则插入全0 + delta 的合并结果；存在则按字段累加。</li>
 *   <li>{@link #rollbackIncrement}：仅对已存在记录按 delta 反向减回；不存在则记 warn 跳过。</li>
 * </ul>
 * </p>
 * <p>
 * <b>红线遵守：</b>DAO 层不直接操作 Redis、不出现 redis/db/rdb 字样、不做业务校验
 * （如 delta 正负、是否存在汇总行由 Logic 保证）、不做 DO→VO 转换。
 * </p>
 *
 * @author fanfan187
 * @version v1.0.0
 * @since v1.0.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CollegeScoreSummaryDAO {

    // ==================== 依赖注入 ====================

    private final CollegeScoreSummaryMapper collegeScoreSummaryMapper;

    // ==================== 基础查询 ====================

    /**
     * 根据学院ID查询积分汇总记录。
     * <p>
     * MyBatis-Plus 的 {@code @TableLogic} 会自动追加 {@code deleted = 0} 条件。
     * collegeId 在表上为唯一约束，{@code selectOne} 最多返回一条。
     * </p>
     *
     * @param collegeId 学院ID
     * @return 积分汇总实体，不存在返回 null
     */
    public CollegeScoreSummary getByCollegeId(Long collegeId) {
        LambdaQueryWrapper<CollegeScoreSummary> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CollegeScoreSummary::getCollegeId, collegeId);
        return collegeScoreSummaryMapper.selectOne(wrapper);
    }

    // ==================== 增量更新 ====================

    /**
     * 增量 upsert：将 Logic 算好的 delta 应用到指定学院的积分汇总行。
     * <p>
     * <b>不存在分支：</b>插入全0 + delta 的合并结果（等价于先 insert 全0再增量，一步到位）。
     * </p>
     * <p>
     * <b>存在分支：</b>
     * <ul>
     *   <li>{@code totalPoints} += {@code deltaPoints}</li>
     *   <li>{@code individualPoints} += {@code deltaPoints}（仅当 {@code isTeam=false}）</li>
     *   <li>{@code teamPoints} += {@code deltaPoints}（仅当 {@code isTeam=true}）</li>
     *   <li>{@code goldCount/silverCount/bronzeCount} 分别 += 对应 delta</li>
     *   <li>{@code updatedAt} = now</li>
     * </ul>
     * </p>
     *
     * @param collegeId    学院ID
     * @param deltaPoints  积分增量（BigDecimal，可为负表示减）
     * @param deltaGold    金牌数增量
     * @param deltaSilver  银牌数增量
     * @param deltaBronze  铜牌数增量
     * @param isTeam       true 表示团体项目积分增量，false 表示个人项目积分增量
     */
    public void upsertIncrement(Long collegeId, BigDecimal deltaPoints, int deltaGold,
                                int deltaSilver, int deltaBronze, boolean isTeam) {
        BigDecimal safeDelta = zeroIfNull(deltaPoints);
        LocalDateTime now = LocalDateTime.now();

        CollegeScoreSummary existing = getByCollegeId(collegeId);
        if (existing == null) {
            // 不存在：插入全0 + delta 的合并结果
            CollegeScoreSummary fresh = new CollegeScoreSummary()
                    .setCollegeId(collegeId)
                    .setTotalPoints(safeDelta)
                    .setGoldCount(deltaGold)
                    .setSilverCount(deltaSilver)
                    .setBronzeCount(deltaBronze)
                    .setIndividualPoints(isTeam ? BigDecimal.ZERO : safeDelta)
                    .setTeamPoints(isTeam ? safeDelta : BigDecimal.ZERO)
                    .setDeleted(0)
                    .setCreatedAt(now)
                    .setUpdatedAt(now);
            collegeScoreSummaryMapper.insert(fresh);
            return;
        }

        // 存在：按字段累加
        existing.setTotalPoints(zeroIfNull(existing.getTotalPoints()).add(safeDelta))
                .setIndividualPoints(isTeam
                        ? zeroIfNull(existing.getIndividualPoints())
                        : zeroIfNull(existing.getIndividualPoints()).add(safeDelta))
                .setTeamPoints(isTeam
                        ? zeroIfNull(existing.getTeamPoints()).add(safeDelta)
                        : zeroIfNull(existing.getTeamPoints()))
                .setGoldCount(zeroIfNullInt(existing.getGoldCount()) + deltaGold)
                .setSilverCount(zeroIfNullInt(existing.getSilverCount()) + deltaSilver)
                .setBronzeCount(zeroIfNullInt(existing.getBronzeCount()) + deltaBronze)
                .setUpdatedAt(now);
        collegeScoreSummaryMapper.updateById(existing);
    }

    /**
     * 回滚增量：将此前 {@link #upsertIncrement} 已应用的 delta 反向减回。
     * <p>
     * 仅对已存在的汇总行生效；不存在则记 warn 跳过（视为无操作可回滚，避免抛异常
     * 中断 Logic 的补偿流程）。{@code updatedAt} = now。
     * </p>
     *
     * @param collegeId    学院ID
     * @param deltaPoints  积分增量（与 upsertIncrement 同值）
     * @param deltaGold    金牌数增量
     * @param deltaSilver  银牌数增量
     * @param deltaBronze  铜牌数增量
     * @param isTeam       与 upsertIncrement 一致的 isTeam 标记
     */
    public void rollbackIncrement(Long collegeId, BigDecimal deltaPoints, int deltaGold,
                                  int deltaSilver, int deltaBronze, boolean isTeam) {
        CollegeScoreSummary existing = getByCollegeId(collegeId);
        if (existing == null) {
            log.warn("rollbackIncrement: collegeId={} 不存在积分汇总记录，跳过回滚", collegeId);
            return;
        }
        BigDecimal safeDelta = zeroIfNull(deltaPoints);
        LocalDateTime now = LocalDateTime.now();

        existing.setTotalPoints(zeroIfNull(existing.getTotalPoints()).subtract(safeDelta))
                .setIndividualPoints(isTeam
                        ? zeroIfNull(existing.getIndividualPoints())
                        : zeroIfNull(existing.getIndividualPoints()).subtract(safeDelta))
                .setTeamPoints(isTeam
                        ? zeroIfNull(existing.getTeamPoints()).subtract(safeDelta)
                        : zeroIfNull(existing.getTeamPoints()))
                .setGoldCount(zeroIfNullInt(existing.getGoldCount()) - deltaGold)
                .setSilverCount(zeroIfNullInt(existing.getSilverCount()) - deltaSilver)
                .setBronzeCount(zeroIfNullInt(existing.getBronzeCount()) - deltaBronze)
                .setUpdatedAt(now);
        collegeScoreSummaryMapper.updateById(existing);
    }

    // ==================== 内部工具 ====================

    /**
     * {@code null} BigDecimal 兜底为 {@link BigDecimal#ZERO}。
     * <p>
     * 兼容历史汇总行可能存在的 null 字段（如旧数据缺 individualPoints），
     * 避免累加时 NPE。
     * </p>
     */
    private static BigDecimal zeroIfNull(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }

    /**
     * {@code null} Integer 兜底为 0。
     */
    private static int zeroIfNullInt(Integer v) {
        return v == null ? 0 : v;
    }
}
