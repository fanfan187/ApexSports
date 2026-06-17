package com.fanfan.apexsports.constants;

/**
 * 运动员相关常量
 * <p>
 * 定义性别编码（女/男）和状态编码（禁赛/正常）等常量
 * </p>
 *
 * @author fanfan187
 * @version v1.0.0
 * @since v1.0.0
 */
public final class AthleteConstants {

    private AthleteConstants() {}

    /** 性别: 女 */
    public static final int GENDER_FEMALE = 0;

    /** 性别: 男 */
    public static final int GENDER_MALE = 1;

    /** 状态: 禁赛 */
    public static final int STATUS_SUSPENDED = 0;

    /** 状态: 正常 */
    public static final int STATUS_ACTIVE = 1;
}
