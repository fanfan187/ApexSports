package com.fanfan.apexsports.constants;

/**
 * JWT 相关常量
 * <p>
 * 定义 Authorization 请求头名称和 Token 前缀等常量。
 * Token 黑名单前缀已迁移至 {@link com.fanfan.apexsports.cache.AuthCache} 内部管理
 * </p>
 *
 * @author fanfan187
 * @version v1.0.0
 * @since v1.0.0
 */
public final class JwtConstants {

    private JwtConstants() {}

    /** Authorization 请求头名称 */
    public static final String TOKEN_HEADER = "Authorization";

    /** Token 前缀 */
    public static final String TOKEN_PREFIX = "Bearer ";
}
