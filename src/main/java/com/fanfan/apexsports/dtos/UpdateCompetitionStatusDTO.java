package com.fanfan.apexsports.dtos;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 更新赛程状态请求 DTO
 * <p>
 * 封装更新赛程状态所需的字段，支持 JSR-303 参数校验
 * </p>
 *
 * @author fanfan187
 * @version v1.0.0
 * @since v1.0.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateCompetitionStatusDTO {

    /** 赛程状态（0-3） */
    @NotNull(message = "赛程状态不能为空")
    @Min(value = 0, message = "赛程状态最小为0")
    @Max(value = 3, message = "赛程状态最大为3")
    private Integer status;
}
