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
 * 比赛场次实体
 * <p>
 * 对应数据库 competition 表，记录单场比赛的场次信息（赛事/场地/轮次/组别/状态等），
 * 支持逻辑删除
 * </p>
 *
 * @author fanfan187
 * @version v1.0.0
 * @since v1.0.0
 */
@Data
@TableName("as_competition")
@Accessors(chain = true)
@NoArgsConstructor
public class Competition implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 比赛场次ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 所属赛事ID */
    private Long eventId;

    /** 比赛场地ID */
    private Long venueId;

    /** 轮次（默认 1） */
    private Integer round;

    /** 组别编号（默认 1） */
    private Integer groupNo;

    /** 开始时间 */
    private LocalDateTime startTime;

    /** 结束时间 */
    private LocalDateTime endTime;

    /** 场次状态: 0-未开始, 其他状态由业务定义 */
    private Integer status;

    /** 备注 */
    private String remark;

    /** 创建时间 */
    private LocalDateTime createdAt;

    /** 更新时间 */
    private LocalDateTime updatedAt;

    /** 逻辑删除: 0-未删除, 1-已删除 */
    @TableLogic
    private Integer deleted;
}
