package com.fanfan.apexsports.vos;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 成绩详情响应 VO
 * <p>
 * 返回给前端的成绩详情视图对象，在 {@link ScoreVO} 基础上扩展 {@code recordId}（破纪录记录链接）。
 * 适用于成绩详情、成绩修改后回显、分页列表（{@code ScoreMapper.findByPage} 返回类型）场景。
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
public class ScoreDetailVO implements Serializable {

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

    /** 是否破纪录：0-否, 1-是 */
    private Integer isRecordBreaking;

    /** 破纪录记录ID（nullable，链接 as_record） */
    private Long recordId;

    /** 录入时间 */
    private LocalDateTime createdAt;
}
