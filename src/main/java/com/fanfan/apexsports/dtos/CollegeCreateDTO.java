package com.fanfan.apexsports.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 新增学院请求 DTO
 * <p>
 * 封装创建学院所需的名称、代码和描述，支持 JSR-303 参数校验
 * </p>
 *
 * @author fanfan187
 * @version v1.0.0
 * @since v1.0.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CollegeCreateDTO {

    @NotBlank(message = "学院名称不能为空")
    @Size(max = 100, message = "学院名称最多100个字符")
    private String name;

    @NotBlank(message = "学院代码不能为空")
    @Size(max = 20, message = "学院代码最多20个字符")
    private String code;

    @Size(max = 500, message = "学院描述最多500个字符")
    private String description;
}
