package com.fanfan.apexsports.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 新增用户请求 DTO
 * <p>
 * 封装创建用户所需的用户名、密码、真实姓名、角色、联系电话和邮箱，支持 JSR-303 参数校验
 * </p>
 *
 * @author fanfan187
 * @version v1.0.0
 * @since v1.0.0
 */
@Data
public class UserCreateDTO {

    @NotBlank(message = "用户名不能为空")
    @Size(min = 2, max = 50, message = "用户名长度2-50个字符")
    private String username;

    /** 密码，不传则默认 123456 */
    @Size(min = 6, max = 50, message = "密码长度6-50个字符")
    private String password;

    @NotBlank(message = "真实姓名不能为空")
    @Size(max = 50, message = "真实姓名最多50个字符")
    private String realName;

    /** 角色: 0-工作人员, 1-管理员 */
    private Integer role;

    @Size(max = 20, message = "联系电话最多20个字符")
    private String phone;

    @Size(max = 100, message = "邮箱最多100个字符")
    private String email;
}
