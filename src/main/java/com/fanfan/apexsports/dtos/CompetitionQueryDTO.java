package com.fanfan.apexsports.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 赛程分页查询请求 DTO
 * <p>
 * 封装赛程列表查询条件，所有字段可选
 * </p>
 *
 * @author fanfan187
 * @version v1.0.0
 * @since v1.0.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CompetitionQueryDTO {

    /** 当前页（可选） */
    private Integer current;

    /** 每页大小（可选） */
    private Integer size;

    /** 比赛日期（可选） */
    private String date;

    /** 场地ID（可选） */
    private Long venueId;

    /** 项目ID（可选） */
    private Long eventId;

    /** 赛程状态（可选） */
    private Integer status;

    /** 轮次（可选） */
    private Integer round;
}
