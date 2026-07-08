package com.fanfan.apexsports.dtos;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 分配裁判请求 DTO
 * <p>
 * 封装为赛程分配裁判所需的字段，支持 JSR-303 参数校验
 * </p>
 *
 * @author fanfan187
 * @version v1.0.0
 * @since v1.0.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AssignRefereeDTO {

    /** 裁判ID */
    @NotNull(message = "裁判ID不能为空")
    private Long refereeId;

    /** 裁判角色（1-4） */
    @NotNull(message = "裁判角色不能为空")
    @Min(value = 1, message = "裁判角色最小为1")
    @Max(value = 4, message = "裁判角色最大为4")
    private Integer role;
}
