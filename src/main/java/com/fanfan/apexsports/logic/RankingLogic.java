package com.fanfan.apexsports.logic;

import com.fanfan.apexsports.config.UserContext;
import com.fanfan.apexsports.constants.ScoreConstants;
import com.fanfan.apexsports.constants.UserConstants;
import com.fanfan.apexsports.daos.CollegeDAO;
import com.fanfan.apexsports.daos.CollegeScoreSummaryDAO;
import com.fanfan.apexsports.exception.BusinessException;
import com.fanfan.apexsports.exception.ErrorCode;
import com.fanfan.apexsports.services.RankingService;
import com.fanfan.apexsports.vos.CollegeRankingVO;
import com.fanfan.apexsports.vos.CollegeScoreDetailVO;
import com.fanfan.apexsports.vos.EventScoreDetailVO;
import com.fanfan.apexsports.vos.ScoreRefreshResultVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * 排名模块业务实现
 * <p>
 * 实现学院总积分排名列表查询、单学院积分详情查询、手动全量重算积分三类业务逻辑。
 * 读接口（listRankings、getCollegeDetail）对所有角色开放；
 * refreshScores 需要管理员权限，并通过刷新锁保证并发互斥。
 * </p>
 * <p>
 * <b>架构红线遵守：</b>
 * <ul>
 *   <li>Logic 层不直接调用 Mapper，所有数据访问通过 {@link CollegeScoreSummaryDAO} / {@link CollegeDAO}；</li>
 *   <li>Logic 层不直接操作缓存，缓存生命周期（刷新锁 + 缓存驱逐）通过
 *       {@link CollegeScoreSummaryDAO} 的委托方法（{@code tryAcquireRefreshLock} /
 *       {@code releaseRefreshLock} / {@code evictAllRankings}）完成，
 *       保持 Logic → DAO → Cache 的统一链路；</li>
 *   <li>Logic 层不出现底层存储/缓存中间件相关字样；</li>
 *   <li>DO 实体不作为方法返回值，统一转换为 VO。</li>
 * </ul>
 * </p>
 *
 * @author fanfan187
 * @version v1.1.0
 * @since v1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RankingLogic implements RankingService {

    // ==================== 依赖注入 ====================

    private final CollegeScoreSummaryDAO collegeScoreSummaryDAO;
    private final CollegeDAO collegeDAO;

    // ==================== 接口82: 学院排名列表 ====================

    /**
     * {@inheritDoc}
     * <p>
     * 排名列表已由 DAO 层从 {@code v_college_ranking} 视图查询并完成 cache-aside 回填，
     * VO 在 Mapper 层组装完毕，Logic 层透传即可，不做二次组装。
     * </p>
     */
    @Override
    public List<CollegeRankingVO> listRankings() {
        return collegeScoreSummaryDAO.findAllRankings();
    }

    // ==================== 接口83: 学院积分详情 ====================

    /**
     * {@inheritDoc}
     * <p>
     * 校验学院存在 → 取排名行（学院无成绩时为 {@code null}，兜底各计数字段为 0）→
     * 取成绩明细列表 → 用全参构造器重建每条明细填充 {@code medalType}（VO 不可变，无 setter）→
     * 组装 {@link CollegeScoreDetailVO} 返回。
     * </p>
     * <p>
     * <b>medalType 填充：</b>SQL 返回的明细 {@code medalType} 字段为 {@code null}（视图/JOIN 不计算奖牌类型），
     * Logic 层用 {@link ScoreConstants#getMedal(int)} 按 {@code rank} 计算奖牌标识，
     * 通过 {@link EventScoreDetailVO} 全参构造器重建明细项以填入该字段。
     * </p>
     *
     * @param collegeId 学院ID
     * @return 学院积分详情 VO
     */
    @Override
    public CollegeScoreDetailVO getCollegeDetail(Long collegeId) {
        // 1. 校验学院存在（College 实体仅用于存在性判断，不暴露给上层）
        if (collegeDAO.getById(collegeId) == null) {
            throw new BusinessException(ErrorCode.COLLEGE_NOT_FOUND);
        }

        // 2. 取排名行（学院无成绩时 ranking 为 null，需兜底构造）
        CollegeRankingVO ranking = collegeScoreSummaryDAO.findRankingByCollegeId(collegeId);

        // 3. 取成绩明细（SQL 返回的 medalType 为 null，需 Logic 重建填充）
        List<EventScoreDetailVO> details = collegeScoreSummaryDAO.findEventScoreDetails(collegeId);

        // 4. 填充 medalType：VO 不可变（@Getter 无 setter），用全参构造器重建每个明细项
        List<EventScoreDetailVO> detailsWithMedal = details.stream()
                .map(d -> new EventScoreDetailVO(
                        d.getEventId(),
                        d.getEventName(),
                        d.getEventType(),
                        d.getAthleteName(),
                        d.getRank(),
                        d.getScore(),
                        d.getPoints(),
                        ScoreConstants.getMedal(d.getRank())))
                .toList();

        // 5. 组装详情 VO（ranking 为 null 时各计数字段兜底为 0）
        if (ranking == null) {
            return new CollegeScoreDetailVO(
                    collegeId,
                    null,
                    null,
                    BigDecimal.ZERO,
                    0,
                    0,
                    0,
                    BigDecimal.ZERO,
                    BigDecimal.ZERO,
                    null,
                    detailsWithMedal);
        }
        return new CollegeScoreDetailVO(
                ranking.getCollegeId(),
                ranking.getCollegeName(),
                ranking.getCollegeCode(),
                ranking.getTotalPoints(),
                ranking.getGoldCount(),
                ranking.getSilverCount(),
                ranking.getBronzeCount(),
                ranking.getIndividualPoints(),
                ranking.getTeamPoints(),
                ranking.getRanking(),
                detailsWithMedal);
    }

    // ==================== 接口84: 手动刷新积分汇总 ====================

    /**
     * {@inheritDoc}
     * <p>
     * 校验管理员权限 → 尝试获取刷新锁（不等待，拿不到抛
     * {@link ErrorCode#SCORE_REFRESH_IN_PROGRESS}）→ 调用 DAO 全量重算
     * （物理清空 + 聚合 INSERT，事务由 DAO 层 {@code @Transactional} 保证）→
     * 组装 {@link ScoreRefreshResultVO} → {@code finally} 中先驱逐全部排名缓存、
     * 再释放刷新锁（先驱逐后放锁，避免下一轮刷新在驱逐完成前启动；
     * 均通过 {@link CollegeScoreSummaryDAO} 委托方法，Logic 不直接依赖 Cache 层），
     * 保证下次查询回源取最新数据。
     * </p>
     *
     * @return 刷新结果 VO
     */
    @Override
    public ScoreRefreshResultVO refreshScores() {
        checkAdmin();

        boolean acquired = collegeScoreSummaryDAO.tryAcquireRefreshLock();
        if (!acquired) {
            throw new BusinessException(ErrorCode.SCORE_REFRESH_IN_PROGRESS);
        }
        try {
            CollegeScoreSummaryDAO.FullRecalcResult result =
                    collegeScoreSummaryDAO.fullRecalculate();
            String refreshTime = LocalDateTime.now()
                    .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            log.info("积分刷新完成: affectedColleges={}, totalEvents={}",
                    result.affectedColleges(), result.totalEvents());
            return new ScoreRefreshResultVO(
                    refreshTime,
                    result.affectedColleges(),
                    result.totalEvents());
        } finally {
            collegeScoreSummaryDAO.evictAllRankings();
            collegeScoreSummaryDAO.releaseRefreshLock();
        }
    }

    // ==================== 私有辅助方法 ====================

    /**
     * 检查当前用户是否为管理员。
     * <p>
     * 非管理员抛出 {@link BusinessException}（{@link ErrorCode#FORBIDDEN}）。
     * 实现与 {@code CollegeLogic}/{@code ScoreLogic} 保持一致。
     * </p>
     */
    private void checkAdmin() {
        Integer role = UserContext.getRole();
        if (role == null || UserConstants.ROLE_ADMIN != role) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
    }
}
