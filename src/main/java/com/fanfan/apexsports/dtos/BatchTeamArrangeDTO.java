package com.fanfan.apexsports.dtos;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 批量安排集体项目参赛者请求 DTO
 * <p>
 * 封装批量安排集体项目（队伍）参赛者到赛程所需的列表数据，支持 JSR-303 嵌套校验
 * </p>
 *
 * @author fanfan187
 * @version v1.0.0
 * @since v1.0.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BatchTeamArrangeDTO {

    /** 集体项目参赛安排列表 */
    @Valid
    @NotEmpty(message = "集体项目参赛安排列表不能为空")
    private List<TeamArrangeItem> teamParticipants;

    /**
     * 单个集体项目参赛安排项
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TeamArrangeItem {

        /** 集体项目参赛记录ID */
        @NotNull(message = "集体项目参赛记录ID不能为空")
        private Long teamParticipationId;
    }
}
