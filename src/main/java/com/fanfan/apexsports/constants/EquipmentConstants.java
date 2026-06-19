package com.fanfan.apexsports.constants;

/**
 * 器材相关常量
 * <p>
 * 定义器材状态编码（停用/可用）等常量。
 * 注意：角色相关常量（如 ROLE_ADMIN）统一在 UserConstants 中定义，全局共用，此处不重复定义
 * </p>
 *
 * @author fanfan187
 * @version v1.0.0
 * @since v1.0.0
 */
public final class EquipmentConstants {

    private EquipmentConstants() {}

    /** 状态: 停用 */
    public static final int STATUS_DISABLED = 0;

    /** 状态: 可用 */
    public static final int STATUS_AVAILABLE = 1;
}
