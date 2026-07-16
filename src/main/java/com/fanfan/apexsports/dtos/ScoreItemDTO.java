package com.fanfan.apexsports.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 单条个人成绩条目 DTO（批量录入的内嵌元素）
 * <p>
 * 仅承载成绩录入所需的最小字段：参赛记录ID、排名、成绩值与成绩显示。
 * 不包含运动员姓名/学院名称等展示字段（由 VO 层在连表查询时填充）。
 * </p>
 *
 * @author fanfan187
 * @version v1.0.0
 * @since v1.0.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ScoreItemDTO {

    /** 参赛记录ID */
    @NotNull(message = "参赛记录ID不能为空")
    private Long participationId;

    /** 排名（1~N） */
    @NotNull(message = "排名不能为空")
    @Positive(message = "排名必须为正整数")
    private Integer rank;

    /** 成绩值（Decimal，精确到3位小数） */
    @NotNull(message = "成绩值不能为空")
    private BigDecimal scoreValue;

    /** 成绩显示（如"10.58秒"、"1.85米"） */
    @NotBlank(message = "成绩显示不能为空")
    private String scoreDisplay;
}
