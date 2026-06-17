package com.fanfan.apexsports.dtos;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 修改学院信息请求 DTO
 * <p>
 * 封装修改学院信息所需的字段，所有字段可选，支持部分更新语义
 * </p>
 *
 * @author fanfan187
 * @version v1.0.0
 * @since v1.0.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CollegeUpdateDTO {

    @Size(max = 100, message = "学院名称最多100个字符")
    private String name;

    @Size(max = 20, message = "学院代码最多20个字符")
    private String code;

    @Size(max = 500, message = "学院描述最多500个字符")
    private String description;
}
