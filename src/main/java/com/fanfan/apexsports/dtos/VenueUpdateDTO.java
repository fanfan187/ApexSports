package com.fanfan.apexsports.dtos;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 修改场地信息请求 DTO
 * <p>
 * 封装修改场地信息所需的字段，所有字段可选，支持部分更新语义。
 * 包含 status 字段以支持可用/不可用状态切换
 * </p>
 *
 * @author fanfan187
 * @version v1.0.0
 * @since v1.0.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class VenueUpdateDTO {

    @Size(max = 100, message = "场地名称最多100个字符")
    private String name;

    @Size(max = 20, message = "场地代码最多20个字符")
    private String code;

    private Integer type;

    @Size(max = 200, message = "位置描述最多200个字符")
    private String location;

    private Integer capacity;

    private Integer status;
}
