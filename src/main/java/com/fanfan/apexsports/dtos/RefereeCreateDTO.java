package com.fanfan.apexsports.dtos;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 新增裁判请求 DTO
 * <p>
 * 封装创建裁判所需的基本信息，支持 JSR-303 参数校验。
 * 注意：不包含 status 字段，状态默认在 Logic 层设为 1（正常）
 * </p>
 *
 * @author fanfan187
 * @version v1.0.0
 * @since v1.0.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RefereeCreateDTO {

    @NotBlank(message = "姓名不能为空")
    @Size(min = 1, max = 50, message = "姓名长度必须在1-50个字符之间")
    private String name;

    @NotNull(message = "性别不能为空")
    @Min(value = 0, message = "性别值非法")
    @Max(value = 1, message = "性别值非法")
    private Integer gender;

    @Size(max = 20, message = "电话长度不能超过20个字符")
    private String phone;

    @Size(max = 30, message = "裁判等级长度不能超过30个字符")
    private String level;
}
