package com.fanfan.apexsports.logic;

import com.fanfan.apexsports.daos.DashboardDAO;
import com.fanfan.apexsports.services.DashboardService;
import com.fanfan.apexsports.vos.BreakingRecordsVO;
import com.fanfan.apexsports.vos.DashboardOverviewVO;
import com.fanfan.apexsports.vos.TodayScheduleVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 首页看板业务实现
 * <p>
 * 实现 {@link DashboardService} 接口，负责 DO→VO 转换、判空防御、状态/记录类型文案映射、
 * scheduleProgress 百分比计算等业务逻辑，依赖 {@link DashboardDAO} 完成数据调度。
 * </p>
 * <p>
 * <b>骨架阶段（T1）：</b>三个方法仅声明并抛出 {@link UnsupportedOperationException}，
 * 全链路实现留给 T2/T3/T4。
 * </p>
 * <p>
 * <b>红线遵守：</b>Logic 层不直接操作缓存（由 DAO 调用 {@code DashboardCache}），
 * 不直接调用 Mapper（通过 DAO），不出现缓存与底层存储相关字样。
 * </p>
 *
 * @author fanfan187
 * @version v1.0.0
 * @since v1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DashboardLogic implements DashboardService {

    private final DashboardDAO dashboardDAO;

    @Override
    public DashboardOverviewVO getOverview() {
        throw new UnsupportedOperationException("Not yet implemented — see T2/T3/T4");
    }

    @Override
    public TodayScheduleVO getTodaySchedule() {
        throw new UnsupportedOperationException("Not yet implemented — see T2/T3/T4");
    }

    @Override
    public BreakingRecordsVO getBreakingRecords() {
        throw new UnsupportedOperationException("Not yet implemented — see T2/T3/T4");
    }
}