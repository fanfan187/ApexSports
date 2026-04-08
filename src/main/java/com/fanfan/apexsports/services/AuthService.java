package com.fanfan.apexsports.services;

import com.fanfan.apexsports.dtos.LoginDTO;
import com.fanfan.apexsports.vos.LoginVO;

/**
 * 认证服务接口
 * <p>
 * 定义用户登录和登出的业务方法
 * </p>
 *
 * @author fanfan187
 * @version v1.0.0
 * @since v1.0.0
 */
public interface AuthService {

    /**
     * 用户登录
     *
     * @param dto 登录请求
     * @return 登录响应（Token + 用户信息）
     */
    LoginVO login(LoginDTO dto);

    /**
     * 用户登出（将 Token 加入黑名单）
     *
     * @param token 原始 JWT Token
     */
    void logout(String token);
}
