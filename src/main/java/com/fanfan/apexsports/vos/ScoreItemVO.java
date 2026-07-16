package com.fanfan.apexsports.vos;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 成绩排名项 VO
 * <p>
 * 用于 {@link CompetitionScoreVO#getRankings()} 列表项，承载单个参赛者/团体在某比赛场次中的排名快照。
 * 无 {@code createdAt}，因排名列表展示场景不关心录入时间（如需时间用 {@link ScoreVO}/{@link ScoreDetailVO}）。
 * 使用 {@code @Getter} 保证不可变性，不允许外部修改。
 * </p>
 *
 * @author fanfan187
 * @version v1.0.0
 * @since v1.0.0
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ScoreItemVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 成绩ID */
    private Long id;

    /** 个人参赛记录ID（null 为团体） */
    private Long participationId;

    /** 团体参赛记录ID（null 为个人） */
    private Long teamParticipationId;

    /** 运动员姓名（个人项目，团体为 null） */
    private String athleteName;

    /** 学院名称 */
    private String collegeName;

    /** 排名 */
    private Integer rank;

    /** 积分 */
    private Integer points;

    /** 成绩值 */
    private BigDecimal scoreValue;

    /** 成绩显示 */
    private String scoreDisplay;

    /** 是否破纪录：0-否, 1-是 */
    private Integer isRecordBreaking;
}
