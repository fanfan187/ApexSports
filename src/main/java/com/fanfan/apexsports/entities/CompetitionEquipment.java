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
 * 比赛器材分配实体
 * <p>
 * 对应数据库 as_competition_equipment 表，记录某场比赛与器材的分配关系、数量及归还状态，
 * 支持逻辑删除
 * </p>
 *
 * @author fanfan187
 * @version v1.0.0
 * @since v1.0.0
 */
@Data
@TableName("as_competition_equipment")
@Accessors(chain = true)
@NoArgsConstructor
public class CompetitionEquipment implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 分配记录ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 比赛场次ID */
    private Long competitionId;

    /** 器材ID */
    private Long equipmentId;

    /** 分配数量 */
    private Integer quantity;

    /** 状态: 0-已分配, 1-使用中, 2-已归还 */
    private Integer status;

    /** 备注 */
    private String remark;

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
