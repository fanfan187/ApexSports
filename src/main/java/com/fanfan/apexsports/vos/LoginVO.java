package com.fanfan.apexsports.vos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 登录响应 VO
 * <p>
 * 封装登录成功后返回的 JWT Token 和用户信息
 * </p>
 *
 * @author fanfan187
 * @version v1.0.0
 * @since v1.0.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoginVO {

    /** JWT Token */
    private String token;

    /** 用户信息 */
    private UserVO userInfo;
}
