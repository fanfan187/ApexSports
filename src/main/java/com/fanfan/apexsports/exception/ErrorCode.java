package com.fanfan.apexsports.exception;

import lombok.Getter;

/**
 * 错误码枚举
 * <p>
 * 定义系统中所有业务错误码及对应消息，按 HTTP 状态码段分类：400xx 参数相关、401xx 认证相关、
 * 403xx 权限相关、404xx 资源不存在、409xx 冲突相关、500xx 系统错误
 * </p>
 *
 * @author fanfan187
 * @version v1.0.0
 * @since v1.0.0
 */
@Getter
public enum ErrorCode {

    // 400xx - 参数相关
    PARAM_VALIDATION_FAILED(40001, "参数校验失败"),

    // 401xx - 认证相关
    UNAUTHORIZED(40101, "未授权或凭证无效"),
    USERNAME_OR_PASSWORD_ERROR(40101, "用户名或密码错误"),

    // 403xx - 权限相关
    FORBIDDEN(40301, "权限不足"),

    // 404xx - 资源不存在
    USER_NOT_FOUND(40401, "用户不存在"),
    COLLEGE_NOT_FOUND(40402, "学院不存在"),
    ATHLETE_NOT_FOUND(40403, "运动员不存在"),
    EVENT_NOT_FOUND(40404, "项目不存在"),
    VENUE_NOT_FOUND(40406, "场地不存在"),
    EQUIPMENT_NOT_FOUND(40407, "器材不存在"),
    REFEREE_NOT_FOUND(40408, "裁判不存在"),
    PARTICIPATION_NOT_FOUND(40411, "参赛记录不存在"),
    TEAM_PARTICIPATION_NOT_FOUND(40412, "团体参赛记录不存在"),

    // 409xx - 冲突相关
    USERNAME_EXISTS(40901, "用户名已存在"),
    OLD_PASSWORD_WRONG(40901, "原密码错误"),
    NEW_PASSWORD_SAME_AS_OLD(40901, "新密码不能与旧密码相同"),

    // 409xx - 状态冲突
    ACCOUNT_DISABLED(40902, "账号已被禁用"),
    CANNOT_MODIFY_SELF(40902, "不能操作自己的账号"),
    COLLEGE_CODE_EXISTS(40903, "学院代码已存在"),
    COLLEGE_IN_USE(40904, "该学院下存在关联数据，不可删除"),
    STUDENT_NO_EXISTS(40905, "学号已存在"),
    EVENT_CODE_EXISTS(40906, "项目代码已存在"),
    EVENT_IN_USE(40907, "该项目存在关联报名记录或赛程安排，不可删除"),
    REFEREE_IN_USE(40908, "该裁判存在未完成的裁判分配，不可删除"),
    VENUE_CODE_EXISTS(40911, "场地代码已存在"),
    VENUE_IN_USE(40912, "该场地下存在关联赛程，不可删除"),
    EQUIPMENT_CODE_EXISTS(40913, "器材代码已存在"),
    EQUIPMENT_IN_USE(40914, "该器材存在未归还记录，不可删除"),
    EQUIPMENT_QUANTITY_INVALID(40915, "可用库存不足"),
    EQUIPMENT_DISABLED(40916, "器材已停用"),

    // 409xx - 报名模块冲突
    ATHLETE_NOT_REGISTERED(40921, "该运动员尚未被编排到场次"),
    TEAM_NOT_SCHEDULED(40922, "该团体尚未被编排到场次"),
    ATHLETE_EVENT_LIMIT_EXCEEDED(40923, "运动员已报满个人项目上限"),
    ATHLETE_EVENT_DUPLICATE(40924, "运动员已报名该项目"),
    EVENT_NOT_INDIVIDUAL(40925, "该项目不是个人项目"),
    EVENT_NOT_TEAM(40926, "该项目不是集体项目"),
    ATHLETE_BANNED(40927, "运动员已被禁赛"),
    EVENT_DISABLED(40928, "项目已停用"),
    PARTICIPATION_ALREADY_SCHEDULED(40929, "该报名已被编排到场次，请先移除编排"),
    COMPETITION_NOT_CHECKABLE(40930, "关联比赛未在可检录状态"),
    PARTICIPATION_ALREADY_CHECKED(40931, "该运动员已检录或已参赛"),
    TEAM_PARTICIPATION_ALREADY_CHECKED(40932, "该团体已检录或已参赛"),
    PARTICIPANT_COUNT_EXCEEDED(40933, "参赛人数超过项目上限"),
    COLLEGE_EVENT_DUPLICATE(40934, "该学院已报名该项目"),
    COMPETITION_STARTED(40935, "关联比赛已开始或已结束，不可取消"),

    // 404xx - 赛程模块资源不存在
    COMPETITION_NOT_FOUND(40413, "比赛场次不存在"),
    REFEREE_ASSIGNMENT_NOT_FOUND(40414, "裁判分配不存在"),
    COMPETITION_EQUIPMENT_NOT_FOUND(40415, "器材使用记录不存在"),

    // 409xx - 赛程模块冲突
    TRACK_TIME_CONFLICT(40941, "径赛时间冲突：该时间段内已有径赛进行"),
    VENUE_TIME_CONFLICT(40942, "场地时间冲突：该场地该时间段已被占用"),
    REFEREE_TIME_CONFLICT(40943, "裁判时间冲突：该裁判在该时间段已有其他场次"),
    REFEREE_ASSIGNMENT_DUPLICATE(40944, "该裁判已分配到此场次"),
    CHIEF_REFEREE_EXISTS(40945, "主裁判已存在：该场次已有主裁判"),
    ATHLETE_TIME_CONFLICT(40946, "运动员时间冲突：该运动员在该时间段已有其他场次"),
    COLLEGE_TIME_CONFLICT(40947, "学院集体项目时间冲突：该学院在该时间段已有其他集体项目比赛"),
    PARTICIPATION_EVENT_MISMATCH(40948, "参赛记录不属于该项目的运动员"),
    PARTICIPATION_ALREADY_ARRANGED(40949, "参赛记录已被编排到其他场次"),
    TEAM_ALREADY_ARRANGED(40950, "团体参赛记录已被编排到其他场次"),
    COMPETITION_FINISHED(40951, "比赛已结束或已取消，不可操作"),
    COMPETITION_IN_PROGRESS(40952, "比赛进行中，不可删除"),
    EQUIPMENT_RETURNED(40953, "器材已归还，不可重复操作"),
    REFEREE_UNAVAILABLE(40954, "裁判状态不可用"),
    INVALID_STATUS_TRANSITION(40955, "非法状态流转"),
    PARTICIPATION_NOT_IN_COMPETITION(40956, "该参赛记录未编排到此场次"),
    TEAM_NOT_IN_COMPETITION(40957, "该团体未编排到此场次"),

    // 500xx - 系统错误
    INTERNAL_ERROR(50000, "系统内部错误");

    /**
     * 错误码
     */
    private final int code;

    /**
     * 错误消息
     */
    private final String message;

    /**
     * 构造错误码枚举
     *
     * @param code    错误码
     * @param message 错误消息
     */
    ErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }
}
