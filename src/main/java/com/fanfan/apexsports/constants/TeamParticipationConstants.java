package com.fanfan.apexsports.constants;

/**
 * 团体参赛记录相关常量
 * <p>
 * 定义参赛状态编码：0-已报名, 1-检录通过, 2-已参赛, 3-弃权, 4-取消资格
 * </p>
 *
 * @author fanfan187
 * @version v1.0.0
 * @since v1.0.0
 */
public final class TeamParticipationConstants {

    private TeamParticipationConstants() {}

    /** 状态: 已报名 */
    public static final int STATUS_REGISTERED = 0;

    /** 状态: 检录通过 */
    public static final int STATUS_CHECKED_IN = 1;

    /** 状态: 已参赛 */
    public static final int STATUS_PARTICIPATED = 2;

    /** 状态: 弃权 */
    public static final int STATUS_WITHDRAWN = 3;

    /** 状态: 取消资格 */
    public static final int STATUS_DISQUALIFIED = 4;
}
