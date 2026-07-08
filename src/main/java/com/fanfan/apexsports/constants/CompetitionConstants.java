package com.fanfan.apexsports.constants;

/**
 * 赛程模块相关常量
 * <p>
 * 定义比赛场次状态、轮次、裁判角色、器材使用状态以及赛程相关默认值和缓存TTL等常量
 * </p>
 *
 * @author fanfan187
 * @version v1.0.0
 * @since v1.0.0
 */
public final class CompetitionConstants {

    private CompetitionConstants() {}

    // ===== 比赛场次状态 =====

    /** 比赛状态: 未开始 */
    public static final int STATUS_NOT_STARTED = 0;

    /** 比赛状态: 进行中 */
    public static final int STATUS_IN_PROGRESS = 1;

    /** 比赛状态: 已结束 */
    public static final int STATUS_FINISHED = 2;

    /** 比赛状态: 已取消 */
    public static final int STATUS_CANCELLED = 3;

    // ===== 轮次 =====

    /** 轮次: 预赛 */
    public static final int ROUND_PRELIMINARY = 1;

    /** 轮次: 决赛 */
    public static final int ROUND_FINAL = 2;

    // ===== 默认值 =====

    /** 默认组号 */
    public static final int DEFAULT_GROUP_NO = 1;

    /** 默认状态（未开始） */
    public static final int DEFAULT_STATUS = 0;

    // ===== 裁判角色 =====

    /** 裁判角色: 主裁判 */
    public static final int REFEREE_ROLE_CHIEF = 1;

    /** 裁判角色: 副裁判 */
    public static final int REFEREE_ROLE_DEPUTY = 2;

    /** 裁判角色: 计时员 */
    public static final int REFEREE_ROLE_TIMER = 3;

    /** 裁判角色: 记录员 */
    public static final int REFEREE_ROLE_RECORDER = 4;

    // ===== 器材使用状态 =====

    /** 器材状态: 已分配 */
    public static final int EQUIPMENT_STATUS_ASSIGNED = 0;

    /** 器材状态: 使用中 */
    public static final int EQUIPMENT_STATUS_IN_USE = 1;

    /** 器材状态: 已归还 */
    public static final int EQUIPMENT_STATUS_RETURNED = 2;

    // ===== 缓存 =====

    /** 秩序册缓存TTL（秒） */
    public static final int BOOKLET_CACHE_TTL = 60;
}
