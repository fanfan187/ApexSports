package com.fanfan.apexsports.dtos;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 修改成绩请求 DTO
 * <p>
 * 对应 {@code PUT /api/scores/{id}}。id 通常取自路径参数，此处一并承载便于 Service 层统一封装。
 * 未提供的字段（null）表示不修改；Logic 层修改后自动触发破纪录回滚与重判、积分重算。
 * </p>
 *
 * @author fanfan187
 * @version v1.0.0
 * @since v1.0.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ScoreUpdateDTO {

    /** 成绩ID */
    @NotNull(message = "成绩ID不能为空")
    private Long id;

    /** 排名（可选，null 表示不修改） */
    @Positive(message = "排名必须为正整数")
    private Integer rank;

    /** 积分（可选，null 表示由 Logic 层按 rank 自动重算） */
    private Integer points;

    /** 成绩值（可选，null 表示不修改） */
    private BigDecimal scoreValue;

    /** 成绩显示（可选，null 表示不修改） */
    private String scoreDisplay;
}
