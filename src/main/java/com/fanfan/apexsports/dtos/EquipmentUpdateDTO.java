package com.fanfan.apexsports.dtos;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 修改器材信息请求 DTO
 * <p>
 * 封装修改器材信息所需的字段，所有字段可选，支持部分更新语义。
 * 不包含 availableQuantity 字段，可用数量只能通过 totalQuantity 联动控制，由后端业务规则维护
 * </p>
 *
 * @author fanfan187
 * @version v1.0.0
 * @since v1.0.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EquipmentUpdateDTO {

    @Size(max = 100, message = "器材名称最多100个字符")
    private String name;

    @Size(max = 20, message = "器材代码最多20个字符")
    private String code;

    @Size(max = 50, message = "器材类型最多50个字符")
    private String type;

    @Size(max = 20, message = "计量单位最多20个字符")
    private String unit;

    @PositiveOrZero(message = "总数量必须大于等于0")
    @Max(value = 100000, message = "总数量不能超过100000")
    private Integer totalQuantity;

    private Integer status;
}
