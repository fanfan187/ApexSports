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
 * 运动员实体
 * <p>
 * 对应数据库 athlete 表，包含运动员基本信息和审计字段，支持逻辑删除
 * </p>
 *
 * @author fanfan187
 * @version v1.0.0
 * @since v1.0.0
 */
@Data
@TableName("athlete")
@Accessors(chain = true)
@NoArgsConstructor
public class Athlete implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 运动员ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 所属学院ID */
    private Long collegeId;

    /** 运动员姓名 */
    private String name;

    /** 学号 */
    private String studentNo;

    /** 性别: 0-女, 1-男 */
    private Integer gender;

    /** 年级 */
    private String grade;

    /** 联系电话 */
    private String phone;

    /** 状态: 0-禁赛, 1-正常 */
    private Integer status;

    /** 创建时间 */
    private LocalDateTime createdAt;

    /** 更新时间 */
    private LocalDateTime updatedAt;

    /** 逻辑删除: 0-未删除, 1-已删除 */
    @TableLogic
    private Integer deleted;
}
