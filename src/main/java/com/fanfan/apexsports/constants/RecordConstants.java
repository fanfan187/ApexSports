package com.fanfan.apexsports.constants;

/**
 * 纪录相关常量
 * <p>
 * 定义纪录类型（校/市/省级）及纪录状态（失效/生效）等常量
 * </p>
 *
 * @author fanfan187
 * @version v1.0.0
 * @since v1.0.0
 */
public final class RecordConstants {

    private RecordConstants() {}

    /** 纪录类型: 校记录 */
    public static final int RECORD_TYPE_SCHOOL = 1;

    /** 纪录类型: 市记录 */
    public static final int RECORD_TYPE_CITY = 2;

    /** 纪录类型: 省记录 */
    public static final int RECORD_TYPE_PROVINCE = 3;

    /** 状态: 失效 */
    public static final int STATUS_INVALID = 0;

    /** 状态: 生效 */
    public static final int STATUS_ACTIVE = 1;
}
