package com.fanfan.apexsports.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 新增运动员请求 DTO
 * <p>
 * 封装创建运动员所需的基本信息，支持 JSR-303 参数校验。
 * 注意：不包含 status 字段，状态默认在 Logic 层设为 1（正常）
 * </p>
 *
 * @author fanfan187
 * @version v1.0.0
 * @since v1.0.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AthleteCreateDTO {

    @NotNull(message = "所属学院不能为空")
    private Long collegeId;

    @NotBlank(message = "姓名不能为空")
    @Size(max = 50, message = "姓名最多50个字符")
    private String name;

    @NotBlank(message = "学号不能为空")
    @Size(max = 30, message = "学号最多30个字符")
    private String studentNo;

    @NotNull(message = "性别不能为空")
    private Integer gender;

    @Size(max = 20, message = "年级最多20个字符")
    private String grade;

    @Size(max = 20, message = "联系电话最多20个字符")
    private String phone;
}
