package com.fanfan.apexsports.controllers;

import com.fanfan.apexsports.common.result.Result;
import com.fanfan.apexsports.dtos.LoginDTO;
import com.fanfan.apexsports.services.AuthService;
import com.fanfan.apexsports.vos.LoginVO;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 认证控制器
 * <p>
 * 提供用户登录和登出接口，登录接口在白名单中无需 Token 验证
 * </p>
 *
 * @author fanfan187
 * @version v1.0.0
 * @since v1.0.0
 */
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /**
     * 用户登录
     *
     * @param dto 登录请求（用户名 + 密码）
     * @return 登录响应（Token + 用户信息）
     */
    @PostMapping("/login")
    public Result<LoginVO> login(@RequestBody @Valid LoginDTO dto) {
        return Result.success("登录成功", authService.login(dto));
    }

    /**
     * 用户登出
     * <p>
     * 将当前 Token 加入 Redis 黑名单使其失效
     * </p>
     *
     * @param request HTTP 请求（从中获取 rawToken）
     * @return 操作结果
     */
    @PostMapping("/logout")
    public Result<Void> logout(HttpServletRequest request) {
        String rawToken = (String) request.getAttribute("rawToken");
        authService.logout(rawToken);
        return Result.success("登出成功", null);
    }
}
