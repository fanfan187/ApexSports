package com.fanfan.apexsports.entities;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
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
 * 裁判分配实体
 * <p>
 * 对应数据库 as_referee_assignment 表，记录某场比赛与裁判的分配关系及角色，
 * 支持逻辑删除
 * </p>
 *
 * @author fanfan187
 * @version v1.0.0
 * @since v1.0.0
 */
@Data
@TableName("as_referee_assignment")
@Accessors(chain = true)
@NoArgsConstructor
public class RefereeAssignment implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 分配记录ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 比赛场次ID */
    private Long competitionId;

    /** 裁判ID */
    private Long refereeId;

    /** 角色: 1-主裁判, 2-副裁判, 3-计时员, 4-记录员 */
    private Integer role;

    /** 状态: 1-已分配 */
    private Integer status;

    /** 创建时间 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    /** 更新时间 */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    /** 逻辑删除: 0-未删除, 1-已删除 */
    @TableLogic
    private Integer deleted;
}
