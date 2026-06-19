package com.fanfan.apexsports.vos;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 裁判赛程响应 VO
 * <p>
 * 用于 GET /referees/{id}/schedule 接口，返回该裁判相关的赛程列表。
 * 此 VO 是 join 查询的产物，不对应单一 Entity，通过 {@code @AllArgsConstructor}
 * 构造或由 Mapper 的结果集映射直接填充
 * </p>
 *
 * @author fanfan187
 * @version v1.0.0
 * @since v1.0.0
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class RefereeScheduleVO implements Serializable {

    /**
     * 比赛ID
     */
    private Long competitionId;

    /**
     * 项目名称
     */
    private String eventName;

    /**
     * 安排时间（startTime 别名，兼容）
     */
    private LocalDateTime scheduleTime;

    /**
     * 比赛开始时间
     */
    private LocalDateTime startTime;

    /**
     * 比赛结束时间
     */
    private LocalDateTime endTime;

    /**
     * 场地名称
     */
    private String venueName;

    /**
     * 裁判职务: 1-主裁判, 2-副裁判, 3-计时员, 4-记录员
     */
    private Integer role;

    /**
     * 比赛状态: 0-未开始, 1-进行中, 2-已结束, 3-已取消
     */
    private Integer competitionStatus;
}
