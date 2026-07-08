package com.fanfan.apexsports.dtos;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 批量安排个人参赛者请求 DTO
 * <p>
 * 封装批量安排个人参赛者到赛程所需的列表数据，支持 JSR-303 嵌套校验
 * </p>
 *
 * @author fanfan187
 * @version v1.0.0
 * @since v1.0.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BatchArrangeDTO {

    /** 参赛安排列表 */
    @Valid
    @NotEmpty(message = "参赛安排列表不能为空")
    private List<ArrangeItem> participants;

    /**
     * 单个个人参赛安排项
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ArrangeItem {

        /** 参赛记录ID */
        @NotNull(message = "参赛记录ID不能为空")
        private Long participationId;

        /** 道次号（可选） */
        private Integer laneNo;

        /** 号码布（可选） */
        private String bibNumber;
    }
}
