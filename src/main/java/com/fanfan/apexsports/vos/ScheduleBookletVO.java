package com.fanfan.apexsports.vos;

import com.fanfan.apexsports.entities.Competition;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 秩序册条目响应 VO
 * <p>
 * 返回给前端的秩序册视图对象，用于生成赛程秩序册（汇总每场比赛的关键信息 +
 * 参赛人数 + 主裁判）。提供从 Entity 转换的静态工厂方法。
 * 使用 {@code @Getter} 保证不可变性，不允许外部修改
 * </p>
 *
 * @author fanfan187
 * @version v1.0.0
 * @since v1.0.0
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ScheduleBookletVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 比赛场次ID */
    private Long id;

    /** 赛事名称（关联字段，外部传入） */
    private String eventName;

    /** 项目类型: 1-个人项目, 2-集体项目（关联字段，外部传入） */
    private Integer eventType;

    /** 项目类别: 1-径赛, 2-田赛（关联字段，外部传入） */
    private Integer eventCategory;

    /** 场地名称（关联字段，外部传入） */
    private String venueName;

    /** 轮次 */
    private Integer round;

    /** 组别编号 */
    private Integer groupNo;

    /** 开始时间 */
    private LocalDateTime startTime;

    /** 结束时间 */
    private LocalDateTime endTime;

    /** 场次状态: 0-未开始, 1-进行中, 2-已结束, 3-已取消 */
    private Integer status;

    /** 参赛人数（外部聚合传入） */
    private Integer participantCount;

    /** 主裁判姓名（外部传入，无主裁判时为 null） */
    private String chiefReferee;

    /**
     * 从 Entity 转换为 VO
     * <p>
     * 通过外部传入的关联字段（赛事信息、场地名称、参赛人数、主裁判）组装 VO，
     * 适用于秩序册批量生成场景
     * </p>
     *
     * @param competition      比赛场次实体
     * @param eventName        赛事名称
     * @param eventType        项目类型
     * @param eventCategory    项目类别
     * @param venueName        场地名称
     * @param participantCount 参赛人数
     * @param chiefReferee     主裁判姓名
     * @return 秩序册条目 VO，如果实体为 null 则返回 null
     */
    public static ScheduleBookletVO from(Competition competition, String eventName, Integer eventType,
                                         Integer eventCategory, String venueName, int participantCount,
                                         String chiefReferee) {
        if (competition == null) {
            return null;
        }
        return new ScheduleBookletVO(
                competition.getId(),
                eventName,
                eventType,
                eventCategory,
                venueName,
                competition.getRound(),
                competition.getGroupNo(),
                competition.getStartTime(),
                competition.getEndTime(),
                competition.getStatus(),
                participantCount,
                chiefReferee
        );
    }
}
