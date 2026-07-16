package com.fanfan.apexsports.dtos;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 修改记录信息请求 DTO
 * <p>
 * 封装 PUT /api/records/{id} 接口的请求体字段，全部字段可选，支持部分更新语义：
 * <b>null 表示不修改该字段</b>，仅传入的字段才会覆盖既有记录值。
 * </p>
 * <p>
 * 按 API 业务规则 5，eventId / athleteId / collegeId 三个字段为不可变属性，
 * 不在本 DTO 中暴露 —— 更新接口无法修改这三项。
 * </p>
 *
 * @author fanfan187
 * @version v1.0.0
 * @since v1.0.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RecordUpdateDTO {

    @Min(value = 1, message = "记录类型只能为 1-校记录、2-市记录、3-省记录")
    @Max(value = 3, message = "记录类型只能为 1-校记录、2-市记录、3-省记录")
    private Integer recordType;

    private String athleteName;

    private String collegeName;

    @DecimalMax(value = "9999999.999", message = "成绩值整数位最多7位、小数位最多3位")
    @Digits(integer = 7, fraction = 3, message = "成绩值整数位最多7位、小数位最多3位")
    private BigDecimal score;

    private String scoreDisplay;

    private LocalDate recordDate;

    private String competitionName;

    @Min(value = 0, message = "状态只能为 0-已作废、1-有效")
    @Max(value = 1, message = "状态只能为 0-已作废、1-有效")
    private Integer status;
}
