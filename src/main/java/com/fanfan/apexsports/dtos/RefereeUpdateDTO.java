package com.fanfan.apexsports.dtos;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 修改裁判信息请求 DTO
 * <p>
 * 封装修改裁判信息所需的字段，所有字段可选，支持部分更新语义。
 * 包含 status 字段以支持禁用/启用操作
 * </p>
 *
 * @author fanfan187
 * @version v1.0.0
 * @since v1.0.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RefereeUpdateDTO {

    @Size(max = 50, message = "姓名长度不能超过50个字符")
    private String name;

    @Min(value = 0, message = "性别值非法")
    @Max(value = 1, message = "性别值非法")
    private Integer gender;

    @Size(max = 20, message = "电话长度不能超过20个字符")
    private String phone;

    @Size(max = 30, message = "裁判等级长度不能超过30个字符")
    private String level;

    @Min(value = 0, message = "状态值非法")
    @Max(value = 1, message = "状态值非法")
    private Integer status;
}
