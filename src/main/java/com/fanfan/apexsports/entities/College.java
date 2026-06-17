package com.fanfan.apexsports.entities;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 学院实体
 * <p>
 * 对应数据库 college 表，包含学院基本信息和审计字段，支持逻辑删除
 * </p>
 *
 * @author fanfan187
 * @version v1.0.0
 * @since v1.0.0
 */
@Data
@TableName("college")
@Accessors(chain = true)
@NoArgsConstructor
public class College implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 学院ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 学院名称 */
    private String name;

    /** 学院代码 */
    private String code;

    /** 学院描述 */
    private String description;

    /** 创建时间 */
    private LocalDateTime createdAt;

    /** 更新时间 */
    private LocalDateTime updatedAt;

    /** 逻辑删除: 0-未删除, 1-已删除 */
    @TableLogic
    private Integer deleted;
}
