package com.fanfan.apexsports.vos;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 成绩响应 VO
 * <p>
 * 返回给前端的成绩视图对象（列表场景），聚合运动员/学院/项目/比赛场次关联字段。
 * 使用 {@code @Getter} 保证不可变性，不允许外部修改。
 * isRecordBreaking 在列表响应中为 {@code Integer}（0/1），区别于录入响应 {@link ScoreEntryResultVO} 的 {@code Boolean}。
 * </p>
 *
 * @author fanfan187
 * @version v1.0.0
 * @since v1.0.0
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ScoreVO implements Serializable {

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

    /** 项目名称 */
    private String eventName;

    /** 比赛场次名称（关联字段，外部传入） */
    private String competitionName;

    /** 排名 */
    private Integer rank;

    /** 积分 */
    private Integer points;

    /** 成绩值 */
    private BigDecimal scoreValue;

    /** 成绩显示 */
    private String scoreDisplay;

    /** 是否破纪录：0-否, 1-是（列表响应用 Integer） */
    private Integer isRecordBreaking;

    /** 录入时间 */
    private LocalDateTime createdAt;
}
