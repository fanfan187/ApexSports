package com.fanfan.apexsports.constants;

/**
 * 用户相关常量
 * <p>
 * 定义默认密码、角色编码（工作人员/管理员）和状态编码（禁用/启用）等常量
 * </p>
 *
 * @author fanfan187
 * @version v1.0.0
 * @since v1.0.0
 */
public final class UserConstants {

    private UserConstants() {}

    /** 默认密码 */
    public static final String DEFAULT_PASSWORD = "123456";

    /** 角色: 工作人员 */
    public static final int ROLE_STAFF = 0;

    /** 角色: 管理员 */
    public static final int ROLE_ADMIN = 1;

    /** 状态: 禁用 */
    public static final int STATUS_DISABLED = 0;

    /** 状态: 启用 */
    public static final int STATUS_ENABLED = 1;
}
