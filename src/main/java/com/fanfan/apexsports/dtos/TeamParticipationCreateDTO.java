package com.fanfan.apexsports.dtos;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

/**
 * 新增团体参赛记录请求 DTO
 * <p>
 * 封装创建团体参赛记录所需的学院ID、项目ID与实际参赛人数，支持 JSR-303 参数校验。
 * 注意：不包含 status / createdAt / updatedAt 字段，状态默认在 Logic 层设为 0（已报名），时间由 DAO 填充
 * </p>
 *
 * @author fanfan187
 * @version v1.0.0
 * @since v1.0.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TeamParticipationCreateDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 学院ID */
    @NotNull(message = "学院ID不能为空")
    private Long collegeId;

    /** 项目ID */
    @NotNull(message = "项目ID不能为空")
    private Long eventId;

    /** 实际参赛人数 */
    @NotNull(message = "参赛人数不能为空")
    @Min(value = 1, message = "参赛人数至少为1")
    private Integer participantCount;
}
