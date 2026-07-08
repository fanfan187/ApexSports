package com.fanfan.apexsports.dtos;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 更新器材状态请求 DTO
 * <p>
 * 封装更新赛程器材使用状态所需的字段，支持 JSR-303 参数校验
 * </p>
 *
 * @author fanfan187
 * @version v1.0.0
 * @since v1.0.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateEquipmentStatusDTO {

    /** 器材使用状态（1-2） */
    @NotNull(message = "器材状态不能为空")
    @Min(value = 1, message = "器材状态最小为1")
    @Max(value = 2, message = "器材状态最大为2")
    private Integer status;
}
