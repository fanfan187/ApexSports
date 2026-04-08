package com.fanfan.apexsports.entities;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 用户实体
 * <p>
 * 对应数据库 user 表，包含用户基本信息、角色、状态和审计字段，支持逻辑删除
 * </p>
 *
 * @author fanfan187
 * @version v1.0.0
 * @since v1.0.0
 */
@Data
@TableName("user")
public class User implements Serializable {

    /** 用户ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 用户名 */
    private String username;

    /** 密码(BCrypt加密) */
    @JsonIgnore
    private String password;

    /** 真实姓名 */
    private String realName;

    /** 角色: 0-工作人员, 1-管理员 */
    private Integer role;

    /** 联系电话 */
    private String phone;

    /** 邮箱 */
    private String email;

    /** 状态: 0-禁用, 1-启用 */
    private Integer status;

    /** 创建时间 */
    private LocalDateTime createdAt;

    /** 更新时间 */
    private LocalDateTime updatedAt;

    /** 逻辑删除: 0-未删除, 1-已删除 */
    @TableLogic
    private Integer deleted;
}
