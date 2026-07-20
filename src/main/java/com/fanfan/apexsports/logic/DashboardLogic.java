package com.fanfan.apexsports.logic;

import com.fanfan.apexsports.daos.DashboardDAO;
import com.fanfan.apexsports.exception.BusinessException;
import com.fanfan.apexsports.exception.ErrorCode;
import com.fanfan.apexsports.services.DashboardService;
import com.fanfan.apexsports.vos.BreakingRecordsVO;
import com.fanfan.apexsports.vos.DashboardOverviewVO;
import com.fanfan.apexsports.vos.TodayScheduleVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

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
        DashboardOverviewVO vo = dashboardDAO.getOverviewData();
        if (vo == null) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "看板数据查询失败");
        }

        // 计算 scheduleProgress = completed / total * 100%（HALF_UP 一位小数，total=0 时记 "0.0%"）
        int total = vo.getTotalCompetitions() == null ? 0 : vo.getTotalCompetitions();
        int completed = vo.getCompletedCompetitions() == null ? 0 : vo.getCompletedCompetitions();
        String scheduleProgress;
        if (total == 0) {
            scheduleProgress = "0.0%";
        } else {
            BigDecimal percent = BigDecimal.valueOf(completed * 100L)
                    .divide(BigDecimal.valueOf(total), 1, RoundingMode.HALF_UP);
            scheduleProgress = percent.toString() + "%";
        }

        // VO 用 @Getter 无 @Setter，无法 setScheduleProgress；
        // 重新构造一个 VO，沿用原 8 字段并填入计算后的 scheduleProgress。
        return new DashboardOverviewVO(
                vo.getTotalAthletes(),
                vo.getTotalColleges(),
                vo.getTotalEvents(),
                vo.getTotalRegistrations(),
                vo.getCompletedCompetitions(),
                vo.getTotalCompetitions(),
                scheduleProgress,
                vo.getRecordBreakCount(),
                vo.getTodayCompetitions()
        );
    }

    @Override
    public TodayScheduleVO getTodaySchedule() {
        TodayScheduleVO vo = dashboardDAO.getTodayScheduleData(LocalDate.now());
        if (vo == null) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "今日赛程查询失败");
        }
        // VO 已由 DAO 完成 DO→VO 组装（cache-aside 聚合结果），Logic 层无需转换直接返回
        return vo;
    }

    @Override
    public BreakingRecordsVO getBreakingRecords() {
        BreakingRecordsVO vo = dashboardDAO.getBreakingRecordsData();
        if (vo == null) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "破纪录汇总查询失败");
        }
        return vo;
    }
}