package com.fanfan.apexsports.daos;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fanfan.apexsports.cache.CollegeScoreSummaryCache;
import com.fanfan.apexsports.entities.CollegeScoreSummary;
import com.fanfan.apexsports.mappers.CollegeScoreSummaryMapper;
import com.fanfan.apexsports.utils.CacheEvictionUtil;
import com.fanfan.apexsports.vos.CollegeRankingVO;
import com.fanfan.apexsports.vos.EventScoreDetailVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 学院积分汇总数据调度层
 * <p>
 * 协调 {@link CollegeScoreSummaryMapper} 与 {@link CollegeScoreSummaryCache}，负责 as_college_score_summary 表的
 * 查询、增量更新/回滚、全量重算与排名缓存调度。积分 delta 由成绩模块 Logic 层算好传入，
 * DAO 只负责 apply 到汇总行；全量重算走「物理清空 + 聚合 INSERT」原子流程。
 * </p>
 * <p>
 * <b>方法语义：</b>
 * <ul>
 *   <li>{@link #upsertIncrement}：不存在则插入全0 + delta 的合并结果；存在则按字段累加。
 *       写入后驱逐全部排名缓存（单学院积分变更会影响总榜名次排序）。</li>
 *   <li>{@link #rollbackIncrement}：仅对已存在记录按 delta 反向减回；不存在则记 warn 跳过。
 *       写入后驱逐全部排名缓存。</li>
 *   <li>{@link #findAllRankings} / {@link #findRankingByCollegeId}：cache-aside 读路径，未命中回源查
 *       {@code v_college_ranking} 视图并回填缓存。</li>
 *   <li>{@link #findEventScoreDetails}：单学院成绩明细，无缓存（明细数据量小且变更频繁，直接查库）。</li>
 *   <li>{@link #fullRecalculate}：@Transactional 原子流程，物理清空 + 聚合 INSERT + 统计，
 *       返回 {@link FullRecalcResult} 供 Logic 组装刷新结果 VO。</li>
 * </ul>
 * 增量写路径的缓存驱逐经 {@link CacheEvictionUtil#evictAfterCommit} 推迟到事务提交后执行，
 * 避免「事务未提交先驱逐 → 并发读回源旧数据回填缓存」的竞态
 * （增量写由成绩模块 Logic 在类级事务内调用，驱逐必须对齐事务提交边界）。
 * </p>
 * <p>
 * <b>红线遵守：</b>DAO 层不直接操作底层缓存中间件（通过 {@link CollegeScoreSummaryCache}）、
 * 不出现底层存储相关字样、不做业务校验（如 delta 正负、是否存在汇总行由 Logic 保证）、
 * 不做 DO→VO 转换（VO 由 Mapper 直接返回，DAO 透传）。
 * </p>
 *
 * @author fanfan187
 * @version v1.1.0
 * @since v1.0.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CollegeScoreSummaryDAO {

    // ==================== 依赖注入 ====================

    private final CollegeScoreSummaryMapper collegeScoreSummaryMapper;
    private final CollegeScoreSummaryCache cache;

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
        } else {
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
        // 写操作：驱逐全部排名缓存（列表 + 单学院行——单学院积分变更会影响总榜名次排序），
        // 事务提交后执行，避免并发读在提交前回填旧排名
        CacheEvictionUtil.evictAfterCommit(cache::evictAll);
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
        // 写操作：驱逐全部排名缓存（事务提交后执行，避免并发读在提交前回填旧排名）
        CacheEvictionUtil.evictAfterCommit(cache::evictAll);
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

    // ==================== 排名查询（cache-aside） ====================

    /**
     * 查询全部学院总积分排名列表（接口82 列表）。
     * <p>
     * cache-aside 读路径：先查列表缓存，未命中回源查 {@code v_college_ranking} 视图并回填缓存。
     * 排名属于「低写高读」数据，列表不分页，整列表作为单 key 缓存。
     * </p>
     *
     * @return 全部学院排名列表，按 ranking 升序
     */
    public List<CollegeRankingVO> findAllRankings() {
        List<CollegeRankingVO> cached = cache.getList();
        if (cached != null) {
            return cached;
        }
        List<CollegeRankingVO> data = collegeScoreSummaryMapper.selectAllRankings();
        cache.cacheList(data);
        return data;
    }

    /**
     * 查询单个学院的排名行（接口83 头部数据）。
     * <p>
     * cache-aside 读路径：先查单学院缓存，未命中回源查 {@code v_college_ranking} 视图并回填缓存。
     * </p>
     *
     * @param collegeId 学院ID
     * @return 该学院排名行；不存在返回 {@code null}（由 Logic 层判空抛 BusinessException）
     */
    public CollegeRankingVO findRankingByCollegeId(Long collegeId) {
        CollegeRankingVO cached = cache.getSingle(collegeId);
        if (cached != null) {
            return cached;
        }
        CollegeRankingVO data = collegeScoreSummaryMapper.selectRankingByCollegeId(collegeId);
        cache.cacheSingle(collegeId, data);
        return data;
    }

    // ==================== 单学院成绩明细（无缓存，直接查库） ====================

    /**
     * 查询单学院的成绩明细列表（接口83 明细数组）。
     * <p>
     * 明细数据量小且变更频繁（任意成绩录入/修改都会影响），不做缓存，直接查库。
     * 返回的 {@link EventScoreDetailVO} 不含 medalType 字段，由 Logic 层用
     * {@code ScoreConstants.getMedal(rank)} 填充。
     * </p>
     *
     * @param collegeId 学院ID
     * @return 该学院的全部成绩明细，按 points DESC 排序
     */
    public List<EventScoreDetailVO> findEventScoreDetails(Long collegeId) {
        return collegeScoreSummaryMapper.selectEventScoreDetails(collegeId);
    }

    // ==================== 全量重算（@Transactional 原子流程） ====================

    /**
     * 全量重算学院积分汇总：物理清空 + 聚合 INSERT + 统计，原子事务。
     * <p>
     * <b>流程：</b>
     * <ol>
     *   <li>{@link CollegeScoreSummaryMapper#physicalDeleteAll} 物理清空 as_college_score_summary 表
     *       （DELETE 而非 TRUNCATE，保证与 {@code @Transactional} 配合可回滚）；</li>
     *   <li>{@link CollegeScoreSummaryMapper#insertAggregatedFromScores} 聚合 as_score 表按学院维度
     *       统计积分/金银铜牌数/个人团体积分，直接 INSERT INTO as_college_score_summary；</li>
     *   <li>{@link CollegeScoreSummaryMapper#countDistinctEvents} 统计参与排名的不同项目数。</li>
     * </ol>
     * </p>
     * <p>
     * <b>事务：</b>{@code @Transactional(rollbackFor = Exception.class)} 保证清空与插入原子性，
     * 任意步骤抛异常则全部回滚（避免清空后未插入导致排名数据丢失）。
     * </p>
     * <p>
     * <b>缓存驱逐与分布式锁：</b>本方法只负责数据重算，不驱逐缓存、不获取刷新锁；
     * 由调用方 Logic 层在 {@code finally} 中调用 {@link CollegeScoreSummaryCache#evictAll}
     * 驱逐缓存，并用 {@link CollegeScoreSummaryCache#tryAcquireRefreshLock} /
     * {@link CollegeScoreSummaryCache#releaseRefreshLock} 保证刷新互斥。
     * </p>
     *
     * @return 重算结果（受影响学院数 + 参与排名的不同项目数），供 Logic 组装刷新结果 VO
     */
    @Transactional(rollbackFor = Exception.class)
    public FullRecalcResult fullRecalculate() {
        collegeScoreSummaryMapper.physicalDeleteAll();
        int affectedColleges = collegeScoreSummaryMapper.insertAggregatedFromScores();
        int totalEvents = collegeScoreSummaryMapper.countDistinctEvents();
        return new FullRecalcResult(affectedColleges, totalEvents);
    }

    /**
     * 全量重算结果，供 Logic 层组装 {@code ScoreRefreshResultVO}。
     *
     * @param affectedColleges 受影响学院数（聚合 INSERT 影响行数）
     * @param totalEvents      参与排名的不同项目数
     */
    public record FullRecalcResult(int affectedColleges, int totalEvents) {
    }
}
