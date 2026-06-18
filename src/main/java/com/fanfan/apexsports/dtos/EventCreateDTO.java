package com.fanfan.apexsports.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 新增体育项目请求 DTO
 * <p>
 * 封装创建体育项目所需的字段，支持 JSR-303 参数校验
 * </p>
 *
 * @author fanfan187
 * @version v1.0.0
 * @since v1.0.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EventCreateDTO {

    @NotBlank(message = "项目名称不能为空")
    @Size(max = 100, message = "项目名称最多100个字符")
    private String name;

    @NotBlank(message = "项目代码不能为空")
    @Size(max = 20, message = "项目代码最多20个字符")
    private String code;

    @NotNull(message = "项目类型不能为空")
    private Integer type;

    @NotNull(message = "项目类别不能为空")
    private Integer category;

    @NotNull(message = "性别限制不能为空")
    private Integer gender;

    @NotBlank(message = "成绩单位不能为空")
    @Size(max = 20, message = "成绩单位最多20个字符")
    private String unit;

    /**
     * 集体项目最大参赛人数（可选，仅集体项目使用）
     */
    private Integer maxParticipants;

    /**
     * 个人最多报名该项目次数，默认 2（可选）
     */
    private Integer maxIndividualEvents;

    @Size(max = 500, message = "项目描述最多500个字符")
    private String description;
}
