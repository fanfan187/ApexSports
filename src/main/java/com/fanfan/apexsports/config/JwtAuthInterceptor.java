package com.fanfan.apexsports.config;

import com.fanfan.apexsports.cache.AuthCache;
import com.fanfan.apexsports.constants.JwtConstants;
import com.fanfan.apexsports.exception.BusinessException;
import com.fanfan.apexsports.exception.ErrorCode;
import com.fanfan.apexsports.utils.JwtUtil;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * JWT 认证拦截器
 * <p>
 * 拦截请求并校验 JWT Token 的有效性，通过 AuthCache 检查黑名单，
 * 解析 Token 后将用户信息注入 {@link UserContext}，请求结束后自动清理 ThreadLocal
 * </p>
 *
 * @author fanfan187
 * @version v1.0.0
 * @since v1.0.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthInterceptor implements HandlerInterceptor {

    private final AuthCache authCache;

    /**
     * 请求预处理
     * <p>
     * OPTIONS 预检请求直接放行；提取并校验 JWT Token，通过 AuthCache 检查黑名单，
     * 解析后将用户信息注入 {@link UserContext}
     * </p>
     *
     * @param request  HTTP 请求
     * @param response HTTP 响应
     * @param handler  处理器
     * @return 是否继续执行后续拦截器和处理器
     */
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // OPTIONS 预检请求直接放行
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        // 提取 Token
        String token = extractToken(request);
        if (!StringUtils.hasText(token)) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "缺少认证凭证");
        }

        // 通过 AuthCache 检查黑名单
        if (authCache.isTokenBlacklisted(token)) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "凭证已失效，请重新登录");
        }

        // 解析 Token 并填充上下文（JwtUtil 为静态工具类）
        Claims claims = JwtUtil.parseToken(token);
        Long userId = claims.get("userId", Long.class);
        Integer role = claims.get("role", Integer.class);
        UserContext.set(userId, role);

        // 将原始 token 存入 request attribute，供 logout 使用
        request.setAttribute("rawToken", token);
        return true;
    }

    /**
     * 请求完成后清理 ThreadLocal
     *
     * @param request  HTTP 请求
     * @param response HTTP 响应
     * @param handler  处理器
     * @param ex       异常（可为 null）
     */
    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        UserContext.clear();
    }

    /**
     * 从请求头中提取 Bearer Token
     *
     * @param request HTTP 请求
     * @return Token 字符串，不存在则返回 null
     */
    private String extractToken(HttpServletRequest request) {
        String bearerToken = request.getHeader(JwtConstants.TOKEN_HEADER);
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith(JwtConstants.TOKEN_PREFIX)) {
            return bearerToken.substring(JwtConstants.TOKEN_PREFIX.length());
        }
        return null;
    }
}
