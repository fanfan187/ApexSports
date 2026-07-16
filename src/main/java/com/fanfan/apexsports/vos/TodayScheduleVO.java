package com.fanfan.apexsports.vos;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

/**
 * 今日赛程 VO
 * <p>
 * 严格映射 Docs/13-统计与看板模块.md §86 的响应字段（camelCase 契约）。
 * {@code date} 为服务器今日日期格式化字符串 "yyyy-MM-dd"，{@code competitions}
 * 为今日按 startTime 升序排列的场次列表（排除 CANCELLED=3）。
 * </p>
 * <p>
 * 使用 {@code @Getter} 保证不可变性，无 {@code @Setter}，仅通过
 * {@code @AllArgsConstructor} 一次性构造赋值。
 * </p>
 *
 * @author fanfan187
 * @version v1.0.0
 * @since v1.0.0
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class TodayScheduleVO implements Serializable {

    /** 今日日期，格式 "yyyy-MM-dd" */
    private String date;

    /** 今日比赛场次总数 */
    private Integer totalCompetitions;

    /** 今日赛程项列表（按 startTime 升序） */
    private List<TodayScheduleItemVO> competitions;
}