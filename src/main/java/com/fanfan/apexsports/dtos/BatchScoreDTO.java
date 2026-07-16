package com.fanfan.apexsports.dtos;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 批量录入个人成绩请求 DTO
 * <p>
 * 对应 {@code POST /api/competitions/{id}/scores/batch}。
 * 携带比赛场次ID与多名选手的成绩条目，由 Logic 层联动触发破纪录判定与积分汇总。
 * </p>
 *
 * @author fanfan187
 * @version v1.0.0
 * @since v1.0.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BatchScoreDTO {

    /** 比赛场次ID（与路径参数一致，便于 Service 层统一校验） */
    @NotNull(message = "比赛场次ID不能为空")
    private Long competitionId;

    /** 成绩条目列表 */
    @NotEmpty(message = "成绩列表不能为空")
    @Valid
    private List<ScoreItemDTO> items;
}
