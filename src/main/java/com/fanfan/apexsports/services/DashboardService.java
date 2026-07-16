package com.fanfan.apexsports.services;

import com.fanfan.apexsports.vos.BreakingRecordsVO;
import com.fanfan.apexsports.vos.DashboardOverviewVO;
import com.fanfan.apexsports.vos.TodayScheduleVO;

/**
 * 首页看板服务接口
 * <p>
 * 只定义"做什么"，不写"怎么做"。由 {@code DashboardLogic} 提供具体实现。
 * 对应 Docs/13-统计与看板模块.md §85/§86/§87 三个只读 GET 接口。
 * </p>
 *
 * @author fanfan187
 * @version v1.0.0
 * @since v1.0.0
 */
public interface DashboardService {

    /**
     * 获取首页概览聚合数据（KPI 卡片所需）。
     *
     * @return 概览 VO
     */
    DashboardOverviewVO getOverview();

    /**
     * 获取今日赛程聚合数据。
     *
     * @return 今日赛程 VO
     */
    TodayScheduleVO getTodaySchedule();

    /**
     * 获取破纪录汇总聚合数据。
     *
     * @return 破纪录汇总 VO
     */
    BreakingRecordsVO getBreakingRecords();
}