package com.fanfan.apexsports.vos;

import com.fanfan.apexsports.entities.TeamParticipation;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 团体参赛记录响应 VO
 * <p>
 * 返回给前端的团体参赛记录视图对象，提供从 Entity 转换的静态工厂方法。
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
public class TeamParticipationVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 团体参赛记录ID */
    private Long id;

    /** 学院ID */
    private Long collegeId;

    /** 学院名称 */
    private String collegeName;

    /** 项目ID */
    private Long eventId;

    /** 项目名称 */
    private String eventName;

    /** 比赛场次ID */
    private Long competitionId;

    /** 实际参赛人数 */
    private Integer participantCount;

    /** 状态: 0-已报名, 1-检录通过, 2-已参赛, 3-弃权, 4-取消资格 */
    private Integer status;

    /** 创建时间 */
    private LocalDateTime createdAt;

    /** 更新时间 */
    private LocalDateTime updatedAt;

    /**
     * 从 Entity 转换为 VO
     * <p>
     * 通过外部传入的学院名称与项目名称组装 VO
     * </p>
     *
     * @param teamParticipation 团体参赛记录实体
     * @param collegeName       学院名称
     * @param eventName         项目名称
     * @return 团体参赛记录 VO，如果实体为 null 则返回 null
     */
    public static TeamParticipationVO from(TeamParticipation teamParticipation, String collegeName, String eventName) {
        if (teamParticipation == null) {
            return null;
        }
        return new TeamParticipationVO(
                teamParticipation.getId(),
                teamParticipation.getCollegeId(),
                collegeName,
                teamParticipation.getEventId(),
                eventName,
                teamParticipation.getCompetitionId(),
                teamParticipation.getParticipantCount(),
                teamParticipation.getStatus(),
                teamParticipation.getCreatedAt(),
                teamParticipation.getUpdatedAt()
        );
    }
}
