package com.fanfan.apexsports.constants;

/**
 * 场地相关常量
 * <p>
 * 定义场地类型编码（径赛/田赛/综合）和状态编码（不可用/可用）等常量
 * </p>
 *
 * @author fanfan187
 * @version v1.0.0
 * @since v1.0.0
 */
public final class VenueConstants {

    private VenueConstants() {}

    /** 场地类型: 径赛场地 */
    public static final int TYPE_TRACK = 1;

    /** 场地类型: 田赛场地 */
    public static final int TYPE_FIELD = 2;

    /** 场地类型: 综合场地 */
    public static final int TYPE_COMPOSITE = 3;

    /** 状态: 不可用 */
    public static final int STATUS_UNAVAILABLE = 0;

    /** 状态: 可用 */
    public static final int STATUS_AVAILABLE = 1;
}
