package com.fanfan.apexsports.dtos;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 新增赛程请求 DTO
 * <p>
 * 封装创建赛程所需的字段，支持 JSR-303 参数校验。
 * 注意：startTime/endTime 使用 ISO datetime 字符串，由 Logic 层解析
 * </p>
 *
 * @author fanfan187
 * @version v1.0.0
 * @since v1.0.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CompetitionCreateDTO {

    /** 所属项目ID */
    @NotNull(message = "项目ID不能为空")
    private Long eventId;

    /** 场地ID */
    @NotNull(message = "场地ID不能为空")
    private Long venueId;

    /** 轮次 */
    @NotNull(message = "轮次不能为空")
    @Min(value = 1, message = "轮次最小为1")
    private Integer round;

    /** 组别（可选） */
    private Integer groupNo;

    /** 开始时间（ISO datetime 字符串） */
    @NotBlank(message = "开始时间不能为空")
    private String startTime;

    /** 结束时间（ISO datetime 字符串） */
    @NotBlank(message = "结束时间不能为空")
    private String endTime;

    /** 备注（可选） */
    @Size(max = 500, message = "备注最多500个字符")
    private String remark;
}
