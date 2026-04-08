package com.fanfan.apexsports.config;

/**
 * 用户上下文 - 基于 ThreadLocal 持有当前登录用户信息
 * <p>
 * 在 JWT 拦截器中设置当前请求的用户 ID 和角色，在请求结束后自动清理，避免内存泄漏
 * </p>
 *
 * @author fanfan187
 * @version v1.0.0
 * @since v1.0.0
 */
public final class UserContext {

    private static final ThreadLocal<Long> USER_ID = new ThreadLocal<>();
    private static final ThreadLocal<Integer> ROLE = new ThreadLocal<>();

    private UserContext() {}

    /**
     * 设置当前登录用户信息
     *
     * @param userId 用户ID
     * @param role   角色编码
     */
    public static void set(Long userId, Integer role) {
        USER_ID.set(userId);
        ROLE.set(role);
    }

    /**
     * 获取当前登录用户ID
     *
     * @return 用户ID
     */
    public static Long getUserId() {
        return USER_ID.get();
    }

    /**
     * 获取当前登录用户角色
     *
     * @return 角色编码
     */
    public static Integer getRole() {
        return ROLE.get();
    }

    /**
     * 清理当前线程的用户信息
     * <p>
     * 必须在请求结束后调用，防止 ThreadLocal 内存泄漏
     * </p>
     */
    public static void clear() {
        USER_ID.remove();
        ROLE.remove();
    }
}
