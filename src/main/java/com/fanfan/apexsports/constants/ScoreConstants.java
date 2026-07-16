package com.fanfan.apexsports.constants;

import java.util.Map;

/**
 * 成绩模块相关常量
 * <p>
 * 定义名次积分、奖牌映射、纪录类型及成绩方向（径赛/田赛）等常量
 * </p>
 *
 * @author fanfan187
 * @version v1.0.0
 * @since v1.0.0
 */
public final class ScoreConstants {

    private ScoreConstants() {}

    // ===== 纪录类型 =====

    /** 纪录类型: 校记录 */
    public static final int RECORD_TYPE_SCHOOL = 1;

    // ===== 成绩方向 =====

    /** 成绩方向: 径赛（越小越优，如短跑用时） */
    public static final int SCORE_DIRECTION_TRACK = 1;

    /** 成绩方向: 田赛（越大越优，如跳远距离） */
    public static final int SCORE_DIRECTION_FIELD = 2;

    // ===== 名次积分 =====

    /** 名次->积分 不可变映射 */
    public static final Map<Integer, Integer> RANK_POINTS_MAP = Map.of(
            1, 10,
            2, 8,
            3, 6,
            4, 4,
            5, 2,
            6, 1
    );

    /**
     * 根据名次获取对应积分
     *
     * @param rank 名次
     * @return 积分；名次不在映射内返回 0
     */
    public static int getPoints(int rank) {
        return RANK_POINTS_MAP.getOrDefault(rank, 0);
    }

    // ===== 奖牌映射 =====

    /** 名次->奖牌 不可变映射（仅 1/2/3 名） */
    public static final Map<Integer, String> MEDAL_MAP = Map.of(
            1, "GOLD",
            2, "SILVER",
            3, "BRONZE"
    );

    /**
     * 根据名次获取奖牌标识
     *
     * @param rank 名次
     * @return 奖牌标识（GOLD/SILVER/BRONZE）；非前三名返回 "NONE"
     */
    public static String getMedal(int rank) {
        return MEDAL_MAP.getOrDefault(rank, "NONE");
    }
}
