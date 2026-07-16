package com.fanfan.apexsports.vos;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 团体成绩响应 VO
 * <p>
 * 返回给前端的团体成绩视图对象。团体成绩无运动员维度，仅保留学院/项目/比赛场次信息。
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
public class TeamScoreVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 成绩ID */
    private Long id;

    /** 团体参赛记录ID */
    private Long teamParticipationId;

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

    /** 录入时间 */
    private LocalDateTime createdAt;
}
