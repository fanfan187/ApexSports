package com.fanfan.apexsports.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 单条个人成绩录入请求 DTO
 * <p>
 * 对应 {@code POST /api/scores}，适用于补录/修正场景。
 * points 允许调用方显式传入（覆盖自动计算），未传时由 Logic 层按 rank 映射自动计算。
 * </p>
 *
 * @author fanfan187
 * @version v1.0.0
 * @since v1.0.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SingleScoreDTO {

    /** 参赛记录ID */
    @NotNull(message = "参赛记录ID不能为空")
    private Long participationId;

    /** 排名（1~N） */
    @NotNull(message = "排名不能为空")
    @Positive(message = "排名必须为正整数")
    private Integer rank;

    /** 积分（可选，null 时由 Logic 层按 rank 自动计算） */
    private Integer points;

    /** 成绩值（Decimal，精确到3位小数） */
    @NotNull(message = "成绩值不能为空")
    private BigDecimal scoreValue;

    /** 成绩显示（如"10.58秒"、"1.85米"） */
    @NotBlank(message = "成绩显示不能为空")
    private String scoreDisplay;
}
