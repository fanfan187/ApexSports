package com.fanfan.apexsports.vos;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 学院总积分排名 VO
 * <p>
 * 返回给前端的学院总积分排名视图对象，对应接口82（{@code GET /api/rankings/colleges}）列表项。
 * 数据来源于视图 {@code v_college_ranking}（按 total_points/gold_count/silver_count/bronze_count 四级排序）。
 * 使用 {@code @Getter} 保证不可变性，不允许外部修改。
 * 字段参考 Docs/12-学院积分与排名模块.md 接口82 契约。
 * </p>
 *
 * @author fanfan187
 * @version v1.0.0
 * @since v1.0.0
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class CollegeRankingVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 学院ID */
    private Long collegeId;

    /** 学院名称 */
    private String collegeName;

    /** 学院代码 */
    private String collegeCode;

    /** 总积分 */
    private BigDecimal totalPoints;

    /** 金牌数 */
    private Integer goldCount;

    /** 银牌数 */
    private Integer silverCount;

    /** 铜牌数 */
    private Integer bronzeCount;

    /** 个人项目积分 */
    private BigDecimal individualPoints;

    /** 团体项目积分 */
    private BigDecimal teamPoints;

    /** 排名（视图中 RANK() 计算结果，遵循文档契约字段名 ranking 而非 rank） */
    private Integer ranking;
}