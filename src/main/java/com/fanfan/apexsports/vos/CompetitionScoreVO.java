package com.fanfan.apexsports.vos;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * 比赛场次成绩汇总响应 VO
 * <p>
 * 返回给前端的比赛场次成绩汇总视图对象，聚合比赛场次/项目信息及其排名列表。
 * 使用 {@code @Getter} 保证不可变性，不允许外部修改。
 * 字段参考 Docs/10-成绩与积分模块.md（380-460 排名汇总场景）。
 * </p>
 *
 * @author fanfan187
 * @version v1.0.0
 * @since v1.0.0
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class CompetitionScoreVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 比赛场次ID */
    private Long competitionId;

    /** 比赛场次名称（关联字段，外部传入） */
    private String competitionName;

    /** 项目名称（关联字段，外部传入） */
    private String eventName;

    /** 排名列表（按 rank 升序） */
    private List<ScoreItemVO> rankings;

    /** 该场次破纪录总数（汇总用） */
    private Integer totalRecords;
}
