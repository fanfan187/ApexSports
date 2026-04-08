package com.fanfan.apexsports.dtos;

import lombok.Data;

/**
 * 修改用户信息请求 DTO
 * <p>
 * 封装修改用户信息所需的字段，不包含用户名和密码，所有字段可选
 * </p>
 *
 * @author fanfan187
 * @version v1.0.0
 * @since v1.0.0
 */
@Data
public class UserUpdateDTO {

    private String realName;

    /** 角色: 0-工作人员, 1-管理员 */
    private Integer role;

    private String phone;

    private String email;

    /** 状态: 0-禁用, 1-启用 */
    private Integer status;
}
