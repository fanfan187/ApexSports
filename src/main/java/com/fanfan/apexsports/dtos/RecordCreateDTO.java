package com.fanfan.apexsports.dtos;

import java.math.BigDecimal;
import java.time.LocalDate;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 新增记录请求 DTO
 * <p>
 * 封装创建记录（校记录/市记录/省记录）所需的入参，支持 JSR-303 参数校验。
 * 注意：不包含 status 字段，状态默认在 DAO 层设为 STATUS_ACTIVE(1)；
 * 不包含 id/createdAt/updatedAt/deleted 字段，由数据库自动生成。
 * 个人/团体项目的互斥校验（athleteId 与 collegeId 二选一）由 Logic 层负责。
 * </p>
 *
 * @author fanfan187
 * @version v1.0.0
 * @since v1.0.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RecordCreateDTO {

    @NotNull(message = "项目ID不能为空")
    private Long eventId;

    @NotNull(message = "记录类型不能为空")
    @Min(value = 1, message = "记录类型无效")
    @Max(value = 3, message = "记录类型无效")
    private Integer recordType;

    private Long athleteId;

    private Long collegeId;

    private String athleteName;

    private String collegeName;

    @NotNull(message = "成绩值不能为空")
    @DecimalMax(value = "9999999.999", message = "成绩值超出允许范围")
    @Digits(integer = 7, fraction = 3, message = "成绩值整数位最多7位，小数位最多3位")
    private BigDecimal score;

    @NotBlank(message = "成绩显示不能为空")
    private String scoreDisplay;

    @NotNull(message = "创造记录日期不能为空")
    private LocalDate recordDate;

    private String competitionName;
}
