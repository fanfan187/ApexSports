package com.fanfan.apexsports.vos;

import com.fanfan.apexsports.entities.Competition;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 比赛场次响应 VO
 * <p>
 * 返回给前端的比赛场次视图对象，提供从 Entity 转换的静态工厂方法。
 * 使用 {@code @Getter} 保证不可变性，不允许外部修改。
 * 同时作为列表查询的复用 VO（CompetitionListVO 直接复用本类，不单独创建）
 * </p>
 *
 * @author fanfan187
 * @version v1.0.0
 * @since v1.0.0
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class CompetitionVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 比赛场次ID */
    private Long id;

    /** 所属赛事ID */
    private Long eventId;

    /** 赛事名称（关联字段，外部传入） */
    private String eventName;

    /** 项目类型: 1-个人项目, 2-集体项目（关联字段，外部传入） */
    private Integer eventType;

    /** 项目类别: 1-径赛, 2-田赛（关联字段，外部传入） */
    private Integer eventCategory;

    /** 比赛场地ID */
    private Long venueId;

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

    /** 备注 */
    private String remark;

    /** 创建时间 */
    private LocalDateTime createdAt;

    /**
     * 从 Entity 转换为 VO
     * <p>
     * 通过外部传入的关联字段（赛事名称/类型/类别、场地名称）组装 VO，
     * 适用于列表与详情展示场景
     * </p>
     *
     * @param competition    比赛场次实体
     * @param eventName      赛事名称
     * @param eventType      项目类型
     * @param eventCategory  项目类别
     * @param venueName      场地名称
     * @return 比赛场次 VO，如果实体为 null 则返回 null
     */
    public static CompetitionVO from(Competition competition, String eventName, Integer eventType,
                                     Integer eventCategory, String venueName) {
        if (competition == null) {
            return null;
        }
        return new CompetitionVO(
                competition.getId(),
                competition.getEventId(),
                eventName,
                eventType,
                eventCategory,
                competition.getVenueId(),
                venueName,
                competition.getRound(),
                competition.getGroupNo(),
                competition.getStartTime(),
                competition.getEndTime(),
                competition.getStatus(),
                competition.getRemark(),
                competition.getCreatedAt()
        );
    }
}
