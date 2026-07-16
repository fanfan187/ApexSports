package com.fanfan.apexsports.entities;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 成绩实体
 * <p>
 * 对应数据库 as_score 表，记录运动员/团体在某场比赛某项目中的成绩。
 * 个人成绩与团体成绩通过 participationId / teamParticipationId 互斥 nullable 区分：
 *   - participationId 非空 且 teamParticipationId 为空 → 个人成绩
 *   - teamParticipationId 非空 且 participationId 为空 → 团体成绩
 * 支持逻辑删除
 * </p>
 *
 * @author fanfan187
 * @version v1.0.0
 * @since v1.0.0
 */
@Data
@TableName("as_score")
@Accessors(chain = true)
@NoArgsConstructor
public class Score implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 成绩ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 个人参赛ID(nullable；与 teamParticipationId 互斥，非空表示个人成绩) */
    private Long participationId;

    /** 团体参赛ID(nullable；与 participationId 互斥，非空表示团体成绩) */
    private Long teamParticipationId;

    /** 运动员ID */
    private Long athleteId;

    /** 学院ID */
    private Long collegeId;

    /** 比赛场次ID */
    private Long competitionId;

    /** 名次 */
    private Integer rank;

    /** 得分(积分) */
    private Integer points;

    /** 成绩值(数值型,便于排序/比较；对应数据库 as_score.score 列) */
    private BigDecimal score;

    /** 成绩显示(如 "12.34s" / "5.60m",前端直接展示) */
    private String scoreDisplay;

    /** 是否破纪录: 0-否, 1-是 */
    private Integer isRecordBreaking;

    /** 备注(nullable) */
    private String remark;

    /** 创建时间 */
    private LocalDateTime createdAt;

    /** 更新时间 */
    private LocalDateTime updatedAt;

    /** 逻辑删除: 0-未删除, 1-已删除 */
    @TableLogic
    private Integer deleted;
}
