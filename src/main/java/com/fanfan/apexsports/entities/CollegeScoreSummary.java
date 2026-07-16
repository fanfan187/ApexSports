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
import java.time.LocalDateTime;

/**
 * 学院积分汇总实体
 * <p>
 * 对应数据库 as_college_score_summary 表，按学院汇总当前运动会的总积分、
 * 金银铜牌数、个人/团体项目积分等，用于学院排名展示
 * </p>
 *
 * @author fanfan187
 * @version v1.0.0
 * @since v1.0.0
 */
@Data
@TableName("as_college_score_summary")
@Accessors(chain = true)
@NoArgsConstructor
public class CollegeScoreSummary implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 主键ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 学院ID（唯一） */
    private Long collegeId;

    /** 总积分 */
    private BigDecimal totalPoints;

    /** 金牌数 */
    private Integer goldCount;

    /** 银牌数 */
    private Integer silverCount;

    /** 铜牌数 */
    private Integer bronzeCount;

    /** 个人项目积分 */
    private BigDecimal individualPoints;

    /** 团体项目积分 */
    private BigDecimal teamPoints;

    /** 创建时间 */
    private LocalDateTime createdAt;

    /** 更新时间 */
    private LocalDateTime updatedAt;

    /** 逻辑删除: 0-未删除, 1-已删除 */
    @TableLogic
    private Integer deleted;
}
