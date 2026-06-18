package com.fanfan.apexsports.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 新增场地请求 DTO
 * <p>
 * 封装创建场地所需的基本信息，支持 JSR-303 参数校验。
 * 注意：不包含 status 字段，状态默认在 Logic 层设为 1（可用）；
 * type 取值范围（1/2/3）由 Logic 层校验，不使用自定义注解
 * </p>
 *
 * @author fanfan187
 * @version v1.0.0
 * @since v1.0.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class VenueCreateDTO {

    @NotBlank(message = "场地名称不能为空")
    @Size(min = 1, max = 100, message = "场地名称长度必须在1~100个字符之间")
    private String name;

    @NotBlank(message = "场地代码不能为空")
    @Size(min = 1, max = 20, message = "场地代码长度必须在1~20个字符之间")
    private String code;

    @NotNull(message = "场地类型不能为空")
    private Integer type;

    @Size(max = 200, message = "位置描述最多200个字符")
    private String location;

    private Integer capacity;
}
