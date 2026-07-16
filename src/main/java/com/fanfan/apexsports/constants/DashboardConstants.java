package com.fanfan.apexsports.constants;

import java.time.format.DateTimeFormatter;
import java.util.Map;

/**
 * 首页看板模块相关常量
 * <p>
 * 定义看板缓存 TTL、赛事状态文案、记录类型中文映射以及时间格式化器等常量，
 * 供 DashboardCache / DashboardDAO / DashboardLogic 复用，避免魔法数字与字符串散落。
 * </p>
 * <p>
 * 设计决策：
 * <ul>
 *   <li>{@link #CACHE_TTL_SECONDS} 取 60 秒，对齐 {@link CompetitionConstants#BOOKLET_CACHE_TTL}，
 *       看板为高读低写聚合数据，60 秒新鲜度足够。</li>
 *   <li>{@link #COMPETITION_STATUS_TEXT} 不包含 CANCELLED(3) —— 看板侧将已取消场次在网络层
 *       即排除，前端无需展示该状态文案。</li>
 *   <li>{@link #RECORD_TYPE_CN} 与 {@link RecordConstants#RECORD_TYPE_SCHOOL/CITY/PROVINCE}
 *       的 int 值一一对应，foreign key 由 Logic 层读取后映射为展示文案。</li>
 * </ul>
 * </p>
 *
 * @author fanfan187
 * @version v1.0.0
 * @since v1.0.0
 */
public final class DashboardConstants {

    private DashboardConstants() {}

    // ===== 缓存 =====

    /** 看板缓存过期时间（秒），cache-aside 命中后由 DAO 回填 */
    public static final int CACHE_TTL_SECONDS = 60;

    // ===== 赛事状态文案（看板侧） =====

    /** 赛事状态：未开始（对应 Competition.status=0） */
    public static final String STATUS_PENDING = "PENDING";

    /** 赛事状态：进行中（对应 Competition.status=1） */
    public static final String STATUS_IN_PROGRESS = "IN_PROGRESS";

    /** 赛事状态：已结束（对应 Competition.status=2） */
    public static final String STATUS_COMPLETED = "COMPLETED";

    /**
     * 赛事状态码 → 文案映射（不可变）。
     * <p>
     * 0→PENDING / 1→IN_PROGRESS / 2→COMPLETED，<b>不包含 3(CANCELLED)</b>，
     * 看板侧统一排除已取消场次。
     * </p>
     */
    public static final Map<Integer, String> COMPETITION_STATUS_TEXT = Map.of(
            0, STATUS_PENDING,
            1, STATUS_IN_PROGRESS,
            2, STATUS_COMPLETED
    );

    /**
     * 记录类型码 → 中文展示文案映射（不可变）。
     * <p>
     * 1→校记录 / 2→市记录 / 3→省记录，与
     * {@link RecordConstants#RECORD_TYPE_SCHOOL}/{@link RecordConstants#RECORD_TYPE_CITY}/
     * {@link RecordConstants#RECORD_TYPE_PROVINCE} 对齐。
     * </p>
     */
    public static final Map<Integer, String> RECORD_TYPE_CN = Map.of(
            1, "校记录",
            2, "市记录",
            3, "省记录"
    );

    // ===== 时间格式化器 =====

    /** 时分格式化器，用于今日赛程的 startTime/endTime 字段展示 */
    public static final DateTimeFormatter HH_MM = DateTimeFormatter.ofPattern("HH:mm");

    /** 日期格式化器，用于今日赛程的 date 字段与破纪录的 breakDate 字段展示 */
    public static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
}