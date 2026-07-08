package com.fanfan.apexsports.dtos;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 分配器材请求 DTO
 * <p>
 * 封装为赛程分配器材所需的字段，支持 JSR-303 参数校验
 * </p>
 *
 * @author fanfan187
 * @version v1.0.0
 * @since v1.0.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AssignEquipmentDTO {

    /** 器材ID */
    @NotNull(message = "器材ID不能为空")
    private Long equipmentId;

    /** 数量 */
    @NotNull(message = "数量不能为空")
    @Min(value = 1, message = "数量最小为1")
    private Integer quantity;

    /** 备注（可选） */
    private String remark;
}
