package com.fanfan.apexsports.vos;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 今日赛程单项 VO
 * <p>
 * 严格映射 Docs/13-统计与看板模块.md §86 的响应字段（camelCase 契约），
 * 为 {@link TodayScheduleVO#getCompetitions()} 列表中每个场次项的字段定义。
 * {@code status} 取值固定为 {@code PENDING / IN_PROGRESS / COMPLETED}，
 * 不展示 CANCELLED（看板侧已排除）。
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
public class TodayScheduleItemVO implements Serializable {

    /** 比赛场次ID */
    private Long competitionId;

    /** 项目名称 */
    private String eventName;

    /** 场地名称 */
    private String venueName;

    /** 开始时间，格式 "HH:mm" */
    private String startTime;

    /** 结束时间，格式 "HH:mm"；未结束为 null */
    private String endTime;

    /** 参赛人数（个人 + 团体合计） */
    private Integer participantCount;

    /** 主裁判姓名；未指派主裁判为 null */
    private String chiefRefereeName;

    /** 场次状态文案：PENDING / IN_PROGRESS / COMPLETED */
    private String status;

    /** 组别编号 */
    private Integer groupNumber;
}