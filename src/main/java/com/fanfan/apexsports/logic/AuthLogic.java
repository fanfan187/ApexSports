package com.fanfan.apexsports.logic;

import cn.hutool.crypto.digest.BCrypt;
import com.fanfan.apexsports.config.UserContext;
import com.fanfan.apexsports.constants.UserConstants;
import com.fanfan.apexsports.daos.UserDAO;
import com.fanfan.apexsports.dtos.LoginDTO;
import com.fanfan.apexsports.entities.User;
import com.fanfan.apexsports.exception.BusinessException;
import com.fanfan.apexsports.exception.ErrorCode;
import com.fanfan.apexsports.services.AuthService;
import com.fanfan.apexsports.utils.JwtUtil;
import com.fanfan.apexsports.vos.LoginVO;
import com.fanfan.apexsports.vos.UserVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 认证业务实现
 * <p>
 * 实现用户登录验证（BCrypt 密码校验、账号状态检查）和登出（将 Token 加入黑名单）
 * </p>
 *
 * @author fanfan187
 * @version v1.0.0
 * @since v1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthLogic implements AuthService {

    private final UserDAO userDAO;

    /**
     * 用户登录
     * <p>
     * 验证用户名密码（BCrypt），检查账号状态，生成 JWT Token
     * </p>
     *
     * @param dto 登录请求
     * @return 登录响应（Token + 用户信息）
     */
    @Override
    public LoginVO login(LoginDTO dto) {
        // 通过 DAO 查询用户
        User user = userDAO.getByUsername(dto.getUsername());
        if (user == null) {
            throw new BusinessException(ErrorCode.USERNAME_OR_PASSWORD_ERROR);
        }

        // 校验密码
        if (!BCrypt.checkpw(dto.getPassword(), user.getPassword())) {
            throw new BusinessException(ErrorCode.USERNAME_OR_PASSWORD_ERROR);
        }

        // 检查状态
        if (UserConstants.STATUS_DISABLED == user.getStatus()) {
            throw new BusinessException(ErrorCode.ACCOUNT_DISABLED);
        }

        // 生成 Token（JwtUtil 为静态工具类）
        String token = JwtUtil.generateToken(user.getId(), user.getRole());

        log.info("用户登录成功: userId={}, username={}", user.getId(), user.getUsername());
        return new LoginVO(token, UserVO.from(user));
    }

    /**
     * 用户登出
     * <p>
     * 将 Token 加入黑名单，过期时间与 Token 剩余有效期一致
     * </p>
     *
     * @param token 原始 JWT Token
     */
    @Override
    public void logout(String token) {
        java.time.Duration remaining = JwtUtil.getRemainingExpiration(token);
        userDAO.blacklistToken(token, remaining);
        log.info("用户登出: userId={}", UserContext.getUserId());
    }
}
