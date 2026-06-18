package com.fanfan.apexsports.vos;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 场地赛程响应 VO
 * <p>
 * 用于 GET /venues/{id}/schedule 接口，返回该场地相关的赛程列表。
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
public class VenueScheduleVO implements Serializable {

    /**
     * 比赛ID
     */
    private Long id;

    /**
     * 项目名称
     */
    private String eventName;

    /**
     * 比赛开始时间
     */
    private LocalDateTime startTime;

    /**
     * 比赛结束时间
     */
    private LocalDateTime endTime;

    /**
     * 比赛状态
     */
    private Integer status;
}
