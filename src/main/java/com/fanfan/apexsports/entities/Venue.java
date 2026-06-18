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
 * 场地实体
 * <p>
 * 对应数据库 as_venue 表，包含场地基本信息和审计字段，支持逻辑删除
 * </p>
 *
 * @author fanfan187
 * @version v1.0.0
 * @since v1.0.0
 */
@Data
@TableName("as_venue")
@Accessors(chain = true)
@NoArgsConstructor
public class Venue implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 场地ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 场地名称 */
    private String name;

    /** 场地代码 */
    private String code;

    /** 类型: 1-径赛场地, 2-田赛场地, 3-综合场地 */
    private Integer type;

    /** 位置描述 */
    private String location;

    /** 容纳人数 */
    private Integer capacity;

    /** 状态: 0-不可用, 1-可用 */
    private Integer status;

    /** 创建时间 */
    private LocalDateTime createdAt;

    /** 更新时间 */
    private LocalDateTime updatedAt;

    /** 逻辑删除: 0-未删除, 1-已删除 */
    @TableLogic
    private Integer deleted;
}
