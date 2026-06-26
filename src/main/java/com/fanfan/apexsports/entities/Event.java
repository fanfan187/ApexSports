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
 * 比赛项目实体
 * <p>
 * 对应数据库 event 表，包含比赛项目基本信息和审计字段，支持逻辑删除
 * </p>
 *
 * @author fanfan187
 * @version v1.0.0
 * @since v1.0.0
 */
@Data
@TableName("as_event")
@Accessors(chain = true)
@NoArgsConstructor
public class Event implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 项目ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 项目名称 */
    private String name;

    /** 项目代码 */
    private String code;

    /** 类型: 1-个人项目, 2-集体项目 */
    private Integer type;

    /** 类别: 1-径赛, 2-田赛 */
    private Integer category;

    /** 性别限制: 0-女子, 1-男子, 2-混合 */
    private Integer gender;

    /** 成绩单位(秒/米/分等) */
    private String unit;

    /** 集体项目最大参赛人数 */
    private Integer maxParticipants;

    /** 个人最多报名该项目次数 */
    private Integer maxIndividualEvents;

    /** 项目描述 */
    private String description;

    /** 状态: 0-停用, 1-启用 */
    private Integer status;

    /** 创建时间 */
    private LocalDateTime createdAt;

    /** 更新时间 */
    private LocalDateTime updatedAt;

    /** 逻辑删除: 0-未删除, 1-已删除 */
    @TableLogic
    private Integer deleted;
}
