package com.fanfan.apexsports.daos;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fanfan.apexsports.cache.ScoreCache;
import com.fanfan.apexsports.dtos.ScoreQueryDTO;
import com.fanfan.apexsports.entities.Score;
import com.fanfan.apexsports.mappers.ScoreMapper;
import com.fanfan.apexsports.vos.ScoreDetailVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 成绩数据调度层
 * <p>
 * 协调 {@link ScoreMapper} 与 {@link ScoreCache}，负责 as_score 表的查询、写入与删除，
 * 以及连表分页查询（as_score JOIN as_participation/as_athlete/as_college/as_event/as_competition）。
 * 作为普通 Spring Bean，不继承 ServiceImpl，内部自动填充审计字段并做写后驱逐缓存。
 * </p>
 * <p>
 * <b>关键技术点：</b>
 * <ul>
 *   <li>{@link #findByPage} 委托 {@link ScoreMapper#findByPage} 执行 5 表 LEFT JOIN 分页，
 *       返回 {@link ScoreDetailVO}（含 athleteName/collegeName/eventName/competitionName 连表名称）。
 *       分页参数 page/size 缺省由本方法补齐为 1/10。</li>
 *   <li>{@link #findByCompetitionId} 用 {@link LambdaQueryWrapper} 单表查询指定场次下的成绩列表，
 *       返回 {@link Score} 实体（不做 DO→VO 转换，转换在 Logic 层）。</li>
 *   <li>{@link #existsByParticipationId} / {@link #existsByTeamParticipationId} 用于成绩录入前的
 *       唯一性校验（个人/团体成绩是否已录入），返回 count > 0。</li>
 *   <li>写操作（增/改/删）统一驱逐全部列表缓存与对应 competitionId 维度的聚合缓存，
 *       因为任意成绩变更都可能影响分页列表与该场次聚合结果。</li>
 *   <li>{@link #deleteById} 先 selectById 拿到 competitionId 再删除，避免驱逐维度缺失。</li>
 * </ul>
 * </p>
 * <p>
 * <b>红线遵守：</b>DAO 层不直接操作缓存（通过 {@link ScoreCache}）、不做业务校验
 * （如成绩合法性、破纪录判定在 Logic）、不做 DO→VO 转换、不出现底层存储相关字样。
 * </p>
 *
 * @author fanfan187
 * @version v1.0.0
 * @since v1.0.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ScoreDAO {

    // ==================== 默认分页参数 ====================

    /** 默认页码 */
    private static final int DEFAULT_PAGE = 1;

    /** 默认每页条数 */
    private static final int DEFAULT_SIZE = 10;

    // ==================== 依赖注入 ====================

    private final ScoreMapper scoreMapper;
    private final ScoreCache scoreCache;
    private final JdbcTemplate jdbcTemplate;

    // ==================== 基础查询 ====================

    /**
     * 根据 ID 查询成绩。
     *
     * @param id 成绩ID
     * @return 成绩实体，不存在返回 null
     */
    public Score getById(Long id) {
        return scoreMapper.selectById(id);
    }

    /**
     * 成绩明细连表分页查询（5 表 LEFT JOIN，返回 ScoreDetailVO）。
     * <p>
     * 委托 {@link ScoreMapper#findByPage} 执行分页，分页参数 page/size 缺省补齐为 1/10。
     * 可按 competitionId 过滤（as_score 表无 event_id 列，项目维度由 competitionId 隐含承载）。
     * </p>
     *
     * @param query 查询条件（competitionId/page/size 均可选）
     * @return 分页结果，每行为 {@link ScoreDetailVO}
     */
    public IPage<ScoreDetailVO> findByPage(ScoreQueryDTO query) {
        int pageNum = (query.getPage() == null || query.getPage() < 1) ? DEFAULT_PAGE : query.getPage();
        int pageSize = (query.getSize() == null || query.getSize() < 1) ? DEFAULT_SIZE : query.getSize();
        Page<ScoreDetailVO> page = new Page<>(pageNum, pageSize);
        return scoreMapper.findByPage(page, query.getCompetitionId());
    }

    /**
     * 查询指定场次下的全部成绩（按 id 升序）。
     * <p>
     * 返回 {@link Score} 实体列表，不做 DO→VO 转换（转换在 Logic 层）。
     * MyBatis-Plus 的 {@code @TableLogic} 会自动追加 {@code deleted = 0} 条件。
     * </p>
     *
     * @param competitionId 场次ID
     * @return 该场次下的成绩列表，无记录返回空列表
     */
    public List<Score> findByCompetitionId(Long competitionId) {
        LambdaQueryWrapper<Score> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Score::getCompetitionId, competitionId)
                .orderByAsc(Score::getId);
        return scoreMapper.selectList(wrapper);
    }

    /**
     * 判断指定个人参赛记录是否已有成绩（成绩录入唯一性校验）。
     *
     * @param participationId 个人参赛记录ID
     * @return 已存在成绩返回 true，否则 false
     */
    public boolean existsByParticipationId(Long participationId) {
        LambdaQueryWrapper<Score> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Score::getParticipationId, participationId);
        return scoreMapper.selectCount(wrapper) > 0;
    }

    /**
     * 判断指定团体参赛记录是否已有成绩（团体成绩录入唯一性校验）。
     *
     * @param teamParticipationId 团体参赛记录ID
     * @return 已存在成绩返回 true，否则 false
     */
    public boolean existsByTeamParticipationId(Long teamParticipationId) {
        LambdaQueryWrapper<Score> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Score::getTeamParticipationId, teamParticipationId);
        return scoreMapper.selectCount(wrapper) > 0;
    }

    // ==================== 写入操作 ====================

    /**
     * 插入成绩（自动填充审计字段与逻辑删除标记，写后驱逐缓存）。
     * <p>
     * 主键 id 由库自增（{@code @TableId(IdType.AUTO)}），不手动 set；
     * 写入后驱逐全部列表缓存与该 competitionId 维度的聚合缓存，重新查库返回完整字段。
     * </p>
     *
     * @param score 成绩实体（不含主键和审计字段）
     * @return 插入后的成绩实体（含自动生成的主键，重新查库保证字段完整）
     */
    public Score insert(Score score) {
        score.setDeleted(0);
        score.setCreatedAt(LocalDateTime.now());
        score.setUpdatedAt(LocalDateTime.now());
        scoreMapper.insert(score);
        Score inserted = scoreMapper.selectById(score.getId());
        // 写操作：驱逐全部列表缓存与该场次维度的聚合缓存
        scoreCache.evictList();
        if (score.getCompetitionId() != null) {
            scoreCache.evictCompetition(score.getCompetitionId());
        }
        return inserted;
    }

    /**
     * 更新成绩（自动更新修改时间，写后驱逐缓存）。
     *
     * @param score 成绩实体（含主键和待更新字段）
     * @return 更新后的成绩实体
     */
    public Score update(Score score) {
        score.setUpdatedAt(LocalDateTime.now());
        scoreMapper.updateById(score);
        Score updated = scoreMapper.selectById(score.getId());
        scoreCache.evictList();
        if (updated != null && updated.getCompetitionId() != null) {
            scoreCache.evictCompetition(updated.getCompetitionId());
        } else if (score.getCompetitionId() != null) {
            scoreCache.evictCompetition(score.getCompetitionId());
        }
        return updated;
    }

    /**
     * 逻辑删除成绩（MyBatis-Plus 自动处理 @TableLogic）。
     * <p>
     * 先查出该成绩以拿到 competitionId，再逻辑删除，写后驱逐对应维度缓存。
     * </p>
     *
     * @param id 成绩ID
     */
    public void deleteById(Long id) {
        Score existing = scoreMapper.selectById(id);
        if (existing == null) {
            log.warn("deleteById: score {} not found, skip", id);
            return;
        }
        scoreMapper.deleteById(id);
        scoreCache.evictList();
        if (existing.getCompetitionId() != null) {
            scoreCache.evictCompetition(existing.getCompetitionId());
        }
    }
}
