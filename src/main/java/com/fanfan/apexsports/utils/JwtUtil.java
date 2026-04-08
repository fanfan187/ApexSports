package com.fanfan.apexsports.utils;

import com.fanfan.apexsports.config.JwtProperties;
import com.fanfan.apexsports.exception.BusinessException;
import com.fanfan.apexsports.exception.ErrorCode;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Date;

/**
 * JWT 工具类
 * <p>
 * 提供 JWT Token 的生成、解析和 Claims 信息提取功能，支持 Token 过期检测和剩余有效期计算
 * </p>
 *
 * @author fanfan187
 * @version v1.0.0
 * @since v1.0.0
 */
@Component
@RequiredArgsConstructor
public class JwtUtil {

    private final JwtProperties jwtProperties;

    /**
     * 获取 HMAC 签名密钥
     *
     * @return 签名密钥
     */
    private SecretKey getSigningKey() {
        byte[] keyBytes = jwtProperties.getSecret().getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    /**
     * 生成 JWT Token
     *
     * @param userId 用户ID
     * @param role   角色
     * @return Token 字符串
     */
    public String generateToken(Long userId, Integer role) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtProperties.getExpiration());

        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim("userId", userId)
                .claim("role", role)
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(getSigningKey())
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
    public Claims parseToken(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(getSigningKey())
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
    public Long getUserId(String token) {
        Claims claims = parseToken(token);
        return claims.get("userId", Long.class);
    }

    /**
     * 从 Token 中获取角色
     *
     * @param token JWT Token 字符串
     * @return 角色编码
     */
    public Integer getRole(String token) {
        Claims claims = parseToken(token);
        return claims.get("role", Integer.class);
    }

    /**
     * 获取 Token 剩余有效时间
     *
     * @return 剩余时间，如果已过期返回 Duration.ZERO
     */
    public Duration getRemainingExpiration(String token) {
        try {
            Claims claims = parseToken(token);
            Date expiration = claims.getExpiration();
            long remaining = expiration.getTime() - System.currentTimeMillis();
            return remaining > 0 ? Duration.ofMillis(remaining) : Duration.ZERO;
        } catch (BusinessException e) {
            return Duration.ZERO;
        }
    }
}
