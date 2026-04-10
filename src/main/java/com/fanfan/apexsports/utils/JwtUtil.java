package com.fanfan.apexsports.utils;

import com.fanfan.apexsports.exception.BusinessException;
import com.fanfan.apexsports.exception.ErrorCode;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Date;

/**
 * JWT 工具类
 * <p>
 * 提供 JWT Token 的生成、解析和 Claims 信息提取功能，支持 Token 过期检测和剩余有效期计算。
 * 所有方法均为静态方法，通过 {@link #init(String, long)} 初始化签名密钥和过期时间
 * </p>
 *
 * @author fanfan187
 * @version v1.0.0
 * @since v1.0.0
 */
public final class JwtUtil {

    private static SecretKey signingKey;
    private static long expiration;

    private JwtUtil() {}

    /**
     * 初始化 JWT 工具类
     * <p>
     * 应在应用启动时调用，设置签名密钥和 Token 有效期
     * </p>
     *
     * @param secret      签名密钥字符串
     * @param expirationMs Token 有效期（毫秒）
     */
    public static void init(String secret, long expirationMs) {
        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        JwtUtil.signingKey = Keys.hmacShaKeyFor(keyBytes);
        JwtUtil.expiration = expirationMs;
    }

    /**
     * 生成 JWT Token
     *
     * @param userId 用户ID
     * @param role   角色
     * @return Token 字符串
     */
    public static String generateToken(Long userId, Integer role) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expiration);

        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim("userId", userId)
                .claim("role", role)
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(signingKey)
                .compact();
    }

    /**
     * 解析 Token，返回 Claims
     * <p>
     * 如果 Token 已过期或无效，抛出 {@link BusinessException}
     * </p>
     *
     * @param token JWT Token 字符串
     * @return Claims 声明信息
     */
    public static Claims parseToken(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(signingKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (ExpiredJwtException e) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "Token 已过期");
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "Token 无效");
        }
    }

    /**
     * 从 Token 中获取用户ID
     *
     * @param token JWT Token 字符串
     * @return 用户ID
     */
    public static Long getUserId(String token) {
        Claims claims = parseToken(token);
        return claims.get("userId", Long.class);
    }

    /**
     * 从 Token 中获取角色
     *
     * @param token JWT Token 字符串
     * @return 角色编码
     */
    public static Integer getRole(String token) {
        Claims claims = parseToken(token);
        return claims.get("role", Integer.class);
    }

    /**
     * 获取 Token 剩余有效时间
     *
     * @param token JWT Token 字符串
     * @return 剩余时间，如果已过期返回 Duration.ZERO
     */
    public static Duration getRemainingExpiration(String token) {
        try {
            Claims claims = parseToken(token);
            Date exp = claims.getExpiration();
            long remaining = exp.getTime() - System.currentTimeMillis();
            return remaining > 0 ? Duration.ofMillis(remaining) : Duration.ZERO;
        } catch (BusinessException e) {
            return Duration.ZERO;
        }
    }
}
