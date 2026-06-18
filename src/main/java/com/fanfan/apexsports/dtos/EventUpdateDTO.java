package com.fanfan.apexsports.dtos;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 修改体育项目信息请求 DTO
 * <p>
 * 封装修改体育项目所需的字段，所有字段可选，支持部分更新语义
 * </p>
 *
 * @author fanfan187
 * @version v1.0.0
 * @since v1.0.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EventUpdateDTO {

    @Size(max = 100, message = "项目名称最多100个字符")
    private String name;

    @Size(max = 20, message = "项目代码最多20个字符")
    private String code;

    private Integer type;

    private Integer category;

    private Integer gender;

    @Size(max = 20, message = "成绩单位最多20个字符")
    private String unit;

    /**
     * 集体项目最大参赛人数（可选，仅集体项目使用）
     */
    private Integer maxParticipants;

    /**
     * 个人最多报名该项目次数（可选）
     */
    private Integer maxIndividualEvents;

    @Size(max = 500, message = "项目描述最多500个字符")
    private String description;

    /**
     * 项目状态（可选，允许通过更新接口修改状态）
     */
    private Integer status;
}
