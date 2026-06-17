package com.fanfan.apexsports.dtos;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 修改运动员信息请求 DTO
 * <p>
 * 封装修改运动员信息所需的字段，所有字段可选，支持部分更新语义。
 * 包含 status 字段以支持禁赛/启用操作
 * </p>
 *
 * @author fanfan187
 * @version v1.0.0
 * @since v1.0.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AthleteUpdateDTO {

    private Long collegeId;

    @Size(max = 50, message = "姓名最多50个字符")
    private String name;

    @Size(max = 30, message = "学号最多30个字符")
    private String studentNo;

    private Integer gender;

    @Size(max = 20, message = "年级最多20个字符")
    private String grade;

    @Size(max = 20, message = "联系电话最多20个字符")
    private String phone;

    private Integer status;
}
