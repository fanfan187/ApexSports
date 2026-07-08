package com.fanfan.apexsports.dtos;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 修改赛程信息请求 DTO
 * <p>
 * 封装修改赛程所需的字段，所有字段可选，支持部分更新语义
 * </p>
 *
 * @author fanfan187
 * @version v1.0.0
 * @since v1.0.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CompetitionUpdateDTO {

    /** 所属项目ID（可选） */
    private Long eventId;

    /** 场地ID（可选） */
    private Long venueId;

    /** 轮次（可选） */
    @Min(value = 1, message = "轮次最小为1")
    private Integer round;

    /** 组别（可选） */
    private Integer groupNo;

    /** 开始时间（ISO datetime 字符串，可选） */
    private String startTime;

    /** 结束时间（ISO datetime 字符串，可选） */
    private String endTime;

    /** 备注（可选） */
    @Size(max = 500, message = "备注最多500个字符")
    private String remark;
}
