package com.fanfan.apexsports.mappers;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fanfan.apexsports.entities.CollegeScoreSummary;
import com.fanfan.apexsports.vos.CollegeRankingVO;
import com.fanfan.apexsports.vos.EventScoreDetailVO;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 学院积分汇总 Mapper
 * <p>
 * 继承 MyBatis-Plus BaseMapper，提供 as_college_score_summary 表的 CRUD 基础操作。
 * 在此之上扩展排名模块所需的 6 个自定义查询方法：
 * <ul>
 *   <li>前 3 个由 {@code resources/mapper/CollegeScoreSummaryMapper.xml} 实现，查 {@code v_college_ranking} 视图与 5 表 LEFT JOIN 明细；</li>
 *   <li>后 3 个由注解实现，用于全量重算流程的物理清空、聚合插入与参与项目数统计。</li>
 * </ul>
 * </p>
 *
 * @author fanfan187
 * @version v1.0.0
 * @since v1.0.0
 */
@Mapper
public interface CollegeScoreSummaryMapper extends BaseMapper<CollegeScoreSummary> {

    /**
     * 查询全部学院总积分排名（接口82 列表）。
     * <p>
     * 数据来源：{@code v_college_ranking} 视图。视图列名为 snake_case，由 XML 中 {@code AS camelCase} 别名对齐 VO 字段。
     * 结果按 {@code ranking ASC} 排序（视图内置 ORDER BY 不保证顺序，故 SQL 显式排序）。
     * </p>
     *
     * @return 全部学院排名列表，按 ranking 升序
     */
    List<CollegeRankingVO> selectAllRankings();

    /**
     * 查询单个学院的总积分排名行（接口83 头部数据，cache-aside 命中失败时回源）。
     * <p>
     * 数据来源：{@code v_college_ranking} 视图，按 {@code college_id} 过滤。
     * </p>
     *
     * @param collegeId 学院ID
     * @return 学院排名行；不存在时返回 {@code null}（由 Logic 层判空）
     */
    CollegeRankingVO selectRankingByCollegeId(@Param("collegeId") Long collegeId);

    /**
     * 查询单个学院的积分明细列表（接口83 明细数组）。
     * <p>
     * 数据来源：{@code as_score} 5 表 LEFT JOIN（score→competition→event 取项目信息；
     * score→participation→athlete 取运动员姓名；团体成绩 athleteName 自然为 null）。
     * 学院过滤覆盖个人（as_athlete.college_id）与团体（as_score.college_id）两种成绩归属路径。
     * medalType 不在 SQL 中计算，返回 null，由 Logic 层用 {@code ScoreConstants.getMedal(rank)} 填充。
     * </p>
     *
     * @param collegeId 学院ID
     * @return 该学院的全部成绩明细，按 points DESC 排序
     */
    List<EventScoreDetailVO> selectEventScoreDetails(@Param("collegeId") Long collegeId);

    /**
     * 物理清空 as_college_score_summary 表（绕过 @TableLogic 逻辑删除）。
     * <p>
     * 用于全量重算流程的"先清后插"前置步骤。注意：原生 TRUNCATE 无法回滚，
     * 这里使用 DELETE 以保证与 {@code @Transactional} 配合可回滚。
     * </p>
     *
     * @return 受影响行数
     */
    @Delete("DELETE FROM as_college_score_summary")
    int physicalDeleteAll();

    /**
     * 统计当前有效成绩涉及的不同项目数（用于全量重算结果 VO 的 totalEvents 字段）。
     * <p>
     * 数据来源：{@code as_score} JOIN {@code as_competition}，按 {@code event_id} 去重计数。
     * </p>
     *
     * @return 不同项目数
     */
    @Select("SELECT COUNT(DISTINCT cp.event_id) FROM as_score s " +
            "JOIN as_competition cp ON s.competition_id = cp.id AND cp.deleted = 0 " +
            "WHERE s.deleted = 0")
    int countDistinctEvents();

    /**
     * 全量重算：聚合 as_score 表，按学院维度统计积分/金 银 铜牌数/个人团体积分，
     * 直接 INSERT INTO as_college_score_summary。
     * <p>
     * 由 {@code fullRecalculate} 在 {@code physicalDeleteAll} 之后调用，遵循「先物理清空后聚合插入」
     * 顺序。{@code sports_meet_id} 填 NULL（本系统按单运动会设计，实体不映射该列，列虽为 VARCHAR
     * 但 NULL 与任何类型兼容）。{@code rank} 列不预填，由 {@code v_college_ranking} 视图实时计算。
     * </p>
     * <p>
     * 个人/团体积分区分依据：{@code team_participation_id IS NULL} 视为个人项目积分，
     * 否则视为团体项目积分（与实体设计一致：个人成绩只填 participation_id，
     * 团体成绩只填 team_participation_id）。
     * </p>
     *
     * @return 受影响行数（即参与排名的学院数）
     */
    @Insert("INSERT INTO as_college_score_summary " +
            "(college_id, sports_meet_id, total_points, gold_count, silver_count, bronze_count, " +
            "individual_points, team_points, created_at, updated_at, deleted) " +
            "SELECT " +
            "s.college_id, NULL, " +
            "SUM(s.points), " +
            "SUM(CASE WHEN s.`rank` = 1 THEN 1 ELSE 0 END), " +
            "SUM(CASE WHEN s.`rank` = 2 THEN 1 ELSE 0 END), " +
            "SUM(CASE WHEN s.`rank` = 3 THEN 1 ELSE 0 END), " +
            "SUM(CASE WHEN s.team_participation_id IS NULL THEN s.points ELSE 0 END), " +
            "SUM(CASE WHEN s.team_participation_id IS NOT NULL THEN s.points ELSE 0 END), " +
            "NOW(), NOW(), 0 " +
            "FROM as_score s " +
            "WHERE s.deleted = 0 " +
            "GROUP BY s.college_id")
    int insertAggregatedFromScores();
}
