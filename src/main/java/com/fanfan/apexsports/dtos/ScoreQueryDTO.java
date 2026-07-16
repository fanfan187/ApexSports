package com.fanfan.apexsports.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 成绩分页查询请求 DTO
 * <p>
 * 对应 {@code GET /api/scores}。competitionId / eventId 均为可选过滤条件，
 * 全部为空时返回全量分页。page / size 缺省由 Logic 层补齐（默认 1 / 10）。
 * </p>
 *
 * @author fanfan187
 * @version v1.0.0
 * @since v1.0.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ScoreQueryDTO {

    /** 比赛场次ID（可选过滤） */
    private Long competitionId;

    /** 项目ID（可选过滤） */
    private Long eventId;

    /** 页码（可选，缺省 1） */
    private Integer page;

    /** 每页条数（可选，缺省 10，最大 100） */
    private Integer size;
}
