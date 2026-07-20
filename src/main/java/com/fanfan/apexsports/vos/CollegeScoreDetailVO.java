package com.fanfan.apexsports.vos;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

/**
 * 学院积分详情 VO
 * <p>
 * 返回给前端的学院积分详情视图对象，对应接口83（{@code GET /api/rankings/colleges/{collegeId}}）。
 * 在 {@link CollegeRankingVO} 字段基础上扩展 {@code eventScoreDetails} 明细数组，
 * 字段独立声明（不继承），保持不可变 VO 的封装简单性。
 * 使用 {@code @Getter} 保证不可变性，不允许外部修改。
 * 字段参考 Docs/12-学院积分与排名模块.md 接口83 契约。
 * </p>
 *
 * @author fanfan187
 * @version v1.0.0
 * @since v1.0.0
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class CollegeScoreDetailVO implements Serializable {

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

    /** 排名（学院无成绩时为 null，由 Logic 兜底） */
    private Integer ranking;

    /** 明细列表：该学院所有成绩的逐项明细（medalType 已由 Logic 填充；按 points 降序） */
    private List<EventScoreDetailVO> eventScoreDetails;
}