package com.fanfan.apexsports.cache;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * 认证缓存管理
 * <p>
 * 管理 JWT Token 黑名单的缓存生命周期，内部私有化 Redis Key 格式和 TTL 策略，
 * 对外只暴露"加入黑名单"和"是否已拉黑"两个业务语义方法
 * </p>
 *
 * @author fanfan187
 * @version v1.0.0
 * @since v1.0.0
 */
@Component
@RequiredArgsConstructor
public class AuthCache {

    /** Token 黑名单 Key 前缀（私有化，不对外暴露） */
    private static final String TOKEN_BLACKLIST_PREFIX = "token:blacklist:";

    private final StringRedisTemplate stringRedisTemplate;

    /**
     * 将 Token 加入黑名单
     *
     * @param token 原始 JWT Token
     * @param ttl   缓存过期时间（与 Token 剩余有效期一致）
     */
    public void addToBlacklist(String token, Duration ttl) {
        stringRedisTemplate.opsForValue().set(TOKEN_BLACKLIST_PREFIX + token, "1", ttl);
    }

    /**
     * 检查 Token 是否已被拉黑
     *
     * @param token 原始 JWT Token
     * @return true 表示已被拉黑
     */
    public boolean isTokenBlacklisted(String token) {
        return Boolean.TRUE.equals(
                stringRedisTemplate.hasKey(TOKEN_BLACKLIST_PREFIX + token)
        );
    }
}
