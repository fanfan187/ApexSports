package com.fanfan.apexsports.constants;

/**
 * 裁判模块常量定义
 * <p>
 * 定义性别编码（女/男）和状态编码（休息/可用）等常量
 * </p>
 *
 * @author fanfan187
 * @version v1.0.0
 * @since v1.0.0
 */
public final class RefereeConstants {

    private RefereeConstants() {}

    /** 状态: 休息 */
    public static final int STATUS_REST = 0;

    /** 状态: 可用 */
    public static final int STATUS_AVAILABLE = 1;

    /** 性别: 女 */
    public static final int GENDER_FEMALE = 0;

    /** 性别: 男 */
    public static final int GENDER_MALE = 1;
}
