package com.fanfan.apexsports.vos;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 学院积分明细项 VO
 * <p>
 * 返回给前端的单条成绩积分明细视图对象，用于 {@link CollegeScoreDetailVO#getEventScoreDetails()} 列表项，
 * 对应接口83（{@code GET /api/rankings/colleges/{collegeId}}）明细数组。
 * 数据来源于 {@code as_score} 五表 LEFT JOIN（score→competition→event 取项目信息；score→participation→athlete 取运动员姓名）。
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
public class EventScoreDetailVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 项目ID */
    private Long eventId;

    /** 项目名称 */
    private String eventName;

    /** 项目类型："个人" / "团体" */
    private String eventType;

    /** 运动员姓名（团体项目为 null） */
    private String athleteName;

    /** 名次（对应 as_score.rank） */
    private Integer rank;

    /** 成绩显示文本（对应 as_score.score_display） */
    private String score;

    /** 该项目获得积分 */
    private BigDecimal points;

    /** 奖牌类型：GOLD/SILVER/BRONZE/NONE，由 Logic 层用 ScoreConstants.getMedal(rank) 填充，VO 自身无逻辑 */
    private String medalType;
}