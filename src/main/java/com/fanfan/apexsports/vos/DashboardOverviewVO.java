package com.fanfan.apexsports.vos;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 首页概览 VO
 * <p>
 * 严格映射 Docs/13-统计与看板模块.md §85 的响应字段（camelCase 契约），
 * 包含看板顶部 KPI 卡片所需的全部聚合指标。{@code scheduleProgress} 为 Logic 层
 * 计算 {@code completed/total*100%}（HALF_UP 一位小数）后写入的字符串，例如 "75.0%"。
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
public class DashboardOverviewVO implements Serializable {

    /** 运动员总数 */
    private Integer totalAthletes;

    /** 学院总数 */
    private Integer totalColleges;

    /** 项目总数 */
    private Integer totalEvents;

    /** 报名总数（含已取消，Docs §85 规则#4） */
    private Integer totalRegistrations;

    /** 已完成比赛场次（status=2） */
    private Integer completedCompetitions;

    /** 比赛总场次（排除 CANCELLED=3） */
    private Integer totalCompetitions;

    /** 赛程进度百分比字符串，如 "75.0%"，Logic 层计算后写入 */
    private String scheduleProgress;

    /** 破纪录数（status=1 的有效记录条数） */
    private Integer recordBreakCount;

    /** 今日比赛场次（startTime 落在今日，且未取消） */
    private Integer todayCompetitions;
}