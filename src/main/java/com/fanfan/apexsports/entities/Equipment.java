package com.fanfan.apexsports.entities;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 器材实体
 * <p>
 * 对应数据库 as_equipment 表，包含器材基本信息、库存数量和审计字段，支持逻辑删除
 * </p>
 *
 * @author fanfan187
 * @version v1.0.0
 * @since v1.0.0
 */
@Data
@TableName("as_equipment")
@Accessors(chain = true)
@NoArgsConstructor
public class Equipment implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 器材ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 器材名称 */
    private String name;

    /** 器材代码 */
    private String code;

    /** 器材类型 */
    private String type;

    /** 计量单位 */
    private String unit;

    /** 总数量 */
    private Integer totalQuantity;

    /** 可用数量 */
    private Integer availableQuantity;

    /** 状态: 0-停用, 1-可用 */
    private Integer status;

    /** 创建时间 */
    private LocalDateTime createdAt;

    /** 更新时间 */
    private LocalDateTime updatedAt;

    /** 逻辑删除: 0-未删除, 1-已删除 */
    @TableLogic
    private Integer deleted;
}
