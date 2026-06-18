package com.fanfan.apexsports.constants;

/**
 * 项目（赛事项目）相关常量
 * <p>
 * 定义项目类型（个人/集体）、项目类别（径赛/田赛）、状态（停用/启用）以及默认报名限制等常量
 * </p>
 *
 * @author fanfan187
 * @version v1.0.0
 * @since v1.0.0
 */
public final class EventConstants {

    private EventConstants() {}

    /** 项目类型: 个人项目 */
    public static final int TYPE_INDIVIDUAL = 1;

    /** 项目类型: 集体项目 */
    public static final int TYPE_TEAM = 2;

    /** 项目类别: 径赛 */
    public static final int CATEGORY_TRACK = 1;

    /** 项目类别: 田赛 */
    public static final int CATEGORY_FIELD = 2;

    /** 状态: 停用 */
    public static final int STATUS_DISABLED = 0;

    /** 状态: 启用 */
    public static final int STATUS_ENABLED = 1;

    /** 默认个人最多报名项目次数 */
    public static final int DEFAULT_MAX_INDIVIDUAL_EVENTS = 2;
}
