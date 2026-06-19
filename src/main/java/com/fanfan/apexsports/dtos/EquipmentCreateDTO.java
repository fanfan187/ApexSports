package com.fanfan.apexsports.dtos;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 新增器材请求 DTO
 * <p>
 * 封装创建器材所需的基本信息，支持 JSR-303 参数校验。
 * 注意：不包含 status 字段，状态默认在 Logic 层设为 1（可用）；
 * 不包含 availableQuantity 字段，新增时可用数量 = 总数量，由后端业务规则控制
 * </p>
 *
 * @author fanfan187
 * @version v1.0.0
 * @since v1.0.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EquipmentCreateDTO {

    @NotBlank(message = "器材名称不能为空")
    @Size(min = 1, max = 100, message = "器材名称长度必须在1~100个字符之间")
    private String name;

    @NotBlank(message = "器材代码不能为空")
    @Size(min = 1, max = 20, message = "器材代码长度必须在1~20个字符之间")
    private String code;

    @Size(max = 50, message = "器材类型最多50个字符")
    private String type;

    @NotBlank(message = "计量单位不能为空")
    @Size(min = 1, max = 20, message = "计量单位长度必须在1~20个字符之间")
    private String unit;

    @NotNull(message = "总数量不能为空")
    @PositiveOrZero(message = "总数量必须大于等于0")
    @Max(value = 100000, message = "总数量不能超过100000")
    private Integer totalQuantity;
}
