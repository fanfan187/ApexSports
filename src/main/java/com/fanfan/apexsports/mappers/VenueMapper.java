package com.fanfan.apexsports.mappers;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fanfan.apexsports.entities.Venue;
import com.fanfan.apexsports.vos.VenueScheduleVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.util.List;

/**
 * 场地 Mapper
 * <p>
 * 继承 MyBatis-Plus BaseMapper，提供场地表的 CRUD 基础操作
 * 并扩展跨表赛程查询（competition JOIN event）
 * </p>
 *
 * <p><b>依赖说明：</b>{@link VenueScheduleVO} 由场地模块 Task 2 创建；
 * 若该类缺失，编译将失败，请先执行 Task 2。</p>
 *
 * @author fanfan187
 * @version v1.0.0
 * @since v1.0.0
 */
@Mapper
public interface VenueMapper extends BaseMapper<Venue> {

    /**
     * 查询指定场地在指定日期的赛程列表
     * <p>
     * 跨表 join：as_competition LEFT JOIN as_event，按 venue_id 过滤，
     * 可选按 date（LocalDate）转换为当日 00:00:00 ~ 次日 00:00:00 范围过滤 start_time，
     * 结果按 start_time ASC 升序排列。
     * </p>
     *
     * @param venueId 场地ID（必填）
     * @param date    日期（可选，为 null 时不按日期过滤）
     * @return 赛程列表（按开始时间升序）
     */
    List<VenueScheduleVO> selectVenueSchedule(@Param("venueId") Long venueId,
                                              @Param("date") LocalDate date);
}
