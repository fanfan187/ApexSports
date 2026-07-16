package com.fanfan.apexsports.controllers;

import com.fanfan.apexsports.common.result.Result;
import com.fanfan.apexsports.services.DashboardService;
import com.fanfan.apexsports.vos.BreakingRecordsVO;
import com.fanfan.apexsports.vos.DashboardOverviewVO;
import com.fanfan.apexsports.vos.TodayScheduleVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 首页看板控制器
 * <p>
 * 提供首页看板所需的三个只读 GET 接口，对应 Docs/13-统计与看板模块.md §85/§86/§87。
 * 所有接口对登录用户开放（无 checkAdmin / 角色守卫），JWT 拦截器已保证调用方已登录；
 * 不返回任何 DO/Entity，仅返回 VO。
 * </p>
 *
 * @author fanfan187
 * @version v1.0.0
 * @since v1.0.0
 */
@RestController
@RequestMapping("/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    /**
     * 获取首页概览聚合数据（KPI 卡片所需）。
     *
     * @return 概览 VO
     */
    @GetMapping("/overview")
    public Result<DashboardOverviewVO> getOverview() {
        return Result.success("查询成功", dashboardService.getOverview());
    }

    /**
     * 获取今日赛程聚合数据。
     *
     * @return 今日赛程 VO
     */
    @GetMapping("/today-schedule")
    public Result<TodayScheduleVO> getTodaySchedule() {
        return Result.success("查询成功", dashboardService.getTodaySchedule());
    }

    /**
     * 获取破纪录汇总聚合数据。
     *
     * @return 破纪录汇总 VO
     */
    @GetMapping("/breaking-records")
    public Result<BreakingRecordsVO> getBreakingRecords() {
        return Result.success("查询成功", dashboardService.getBreakingRecords());
    }
}