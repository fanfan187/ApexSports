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
 * 参赛记录实体
 * <p>
 * 对应数据库 participation 表，记录个人运动员在某项目/比赛场次的报名及参赛状态，支持逻辑删除
 * </p>
 *
 * @author fanfan187
 * @version v1.0.0
 * @since v1.0.0
 */
@Data
@TableName("as_participation")
@Accessors(chain = true)
@NoArgsConstructor
public class Participation implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 参赛记录ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 运动员ID */
    private Long athleteId;

    /** 项目ID */
    private Long eventId;

    /** 比赛场次ID */
    private Long competitionId;

    /** 跑道号/道次 */
    private Integer laneNo;

    /** 号码牌 */
    private String bibNumber;

    /** 状态: 0-已报名, 1-检录通过, 2-已参赛, 3-弃权, 4-取消资格 */
    private Integer status;

    /** 创建时间 */
    private LocalDateTime createdAt;

    /** 更新时间 */
    private LocalDateTime updatedAt;

    /** 逻辑删除: 0-未删除, 1-已删除 */
    @TableLogic
    private Integer deleted;
}
