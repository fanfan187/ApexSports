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
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 记录实体
 * <p>
 * 对应数据库 record 表，用于 EventDAO 历史记录查询；包含校/市/省记录信息，支持逻辑删除
 * </p>
 *
 * @author fanfan187
 * @version v1.0.0
 * @since v1.0.0
 */
@Data
@TableName("as_record")
@Accessors(chain = true)
@NoArgsConstructor
public class Record implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 记录ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 项目ID */
    private Long eventId;

    /** 记录类型: 1-校记录, 2-市记录, 3-省记录 */
    private Integer recordType;

    /** 运动员ID(个人项目) */
    private Long athleteId;

    /** 学院ID(团体项目) */
    private Long collegeId;

    /** 运动员姓名(冗余) */
    private String athleteName;

    /** 学院名称(冗余) */
    private String collegeName;

    /** 成绩值 */
    private BigDecimal score;

    /** 成绩显示 */
    private String scoreDisplay;

    /** 创造记录日期 */
    private LocalDate recordDate;

    /** 创造记录的比赛名称 */
    private String competitionName;

    /** 状态: 0-已作废, 1-有效 */
    private Integer status;

    /** 创建时间 */
    private LocalDateTime createdAt;

    /** 更新时间 */
    private LocalDateTime updatedAt;

    /** 逻辑删除: 0-未删除, 1-已删除 */
    @TableLogic
    private Integer deleted;
}
