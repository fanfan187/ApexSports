package com.fanfan.apexsports.vos;

import com.fanfan.apexsports.entities.TeamParticipation;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

/**
 * 团体参赛安排响应 VO
 * <p>
 * 返回给前端的团体参赛安排视图对象（用于比赛场次详情中的团体参赛者列表），
 * 提供从 Entity 转换的静态工厂方法。
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
public class TeamParticipantArrangementVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 团体参赛记录ID */
    private Long teamParticipationId;

    /** 学院ID */
    private Long collegeId;

    /** 学院名称（关联字段，外部传入） */
    private String collegeName;

    /** 实际参赛人数 */
    private Integer participantCount;

    /** 状态: 0-已报名, 1-检录通过, 2-已参赛, 3-弃权, 4-取消资格 */
    private Integer status;

    /**
     * 从 Entity 转换为 VO
     * <p>
     * 通过外部传入的关联字段（学院名称）组装 VO，
     * 适用于比赛场次详情中的团体参赛者列表展示
     * </p>
     *
     * @param teamParticipation 团体参赛记录实体
     * @param collegeName       学院名称
     * @return 团体参赛安排 VO，如果实体为 null 则返回 null
     */
    public static TeamParticipantArrangementVO from(TeamParticipation teamParticipation, String collegeName) {
        if (teamParticipation == null) {
            return null;
        }
        return new TeamParticipantArrangementVO(
                teamParticipation.getId(),
                teamParticipation.getCollegeId(),
                collegeName,
                teamParticipation.getParticipantCount(),
                teamParticipation.getStatus()
        );
    }
}
