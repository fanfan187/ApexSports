package com.fanfan.apexsports.vos;

import com.fanfan.apexsports.entities.Athlete;
import com.fanfan.apexsports.entities.College;
import com.fanfan.apexsports.entities.Event;
import com.fanfan.apexsports.entities.Participation;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 个人参赛记录响应 VO
 * <p>
 * 返回给前端的参赛记录视图对象，提供从 Entity 转换的静态工厂方法。
 * 使用 {@code @Getter} 保证不可变性，不允许外部修改。
 * 支持两种转换场景：列表视图（含关联字段）与详情视图（含关联实体）
 * </p>
 *
 * @author fanfan187
 * @version v1.0.0
 * @since v1.0.0
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ParticipationVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 参赛记录ID */
    private Long id;

    /** 运动员ID */
    private Long athleteId;

    /** 运动员姓名 */
    private String athleteName;

    /** 学号 */
    private String studentNo;

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

    /** 跑道号/道次 */
    private Integer laneNo;

    /** 号码牌 */
    private String bibNumber;

    /** 状态: 0-已报名, 1-检录通过, 2-已参赛, 3-弃权, 4-取消资格 */
    private Integer status;

    /** 创建时间 */
    private LocalDateTime createdAt;

    /** 更新时间 */
    private LocalDateTime updatedAt;

    /**
     * 从 Entity 转换为 VO（列表视图）
     * <p>
     * 通过外部传入的关联字段（运动员姓名/学号、学院信息、项目名称）组装 VO，适用于列表展示场景
     * </p>
     *
     * @param participation 参赛记录实体
     * @param athleteName   运动员姓名
     * @param studentNo     学号
     * @param collegeId     学院ID
     * @param collegeName   学院名称
     * @param eventId       项目ID
     * @param eventName     项目名称
     * @return 参赛记录 VO，如果实体为 null 则返回 null
     */
    public static ParticipationVO from(Participation participation, String athleteName, String studentNo,
                                       Long collegeId, String collegeName, Long eventId, String eventName) {
        if (participation == null) {
            return null;
        }
        return new ParticipationVO(
                participation.getId(),
                participation.getAthleteId(),
                athleteName,
                studentNo,
                collegeId,
                collegeName,
                participation.getEventId(),
                eventName,
                participation.getCompetitionId(),
                participation.getLaneNo(),
                participation.getBibNumber(),
                participation.getStatus(),
                participation.getCreatedAt(),
                participation.getUpdatedAt()
        );
    }

    /**
     * 从 Entity 转换为 VO（详情视图）
     * <p>
     * 通过关联实体（Athlete、College、Event）组装 VO，适用于详情展示场景。
     * 注意：collegeId 来源于 Athlete 的 collegeId 关联字段
     * </p>
     *
     * @param participation 参赛记录实体
     * @param athlete       运动员实体
     * @param college       学院实体
     * @param event         项目实体
     * @return 参赛记录 VO，如果实体为 null 则返回 null
     */
    public static ParticipationVO from(Participation participation, Athlete athlete, College college, Event event) {
        if (participation == null) {
            return null;
        }
        return new ParticipationVO(
                participation.getId(),
                participation.getAthleteId(),
                athlete != null ? athlete.getName() : null,
                athlete != null ? athlete.getStudentNo() : null,
                athlete != null ? athlete.getCollegeId() : null,
                college != null ? college.getName() : null,
                participation.getEventId(),
                event != null ? event.getName() : null,
                participation.getCompetitionId(),
                participation.getLaneNo(),
                participation.getBibNumber(),
                participation.getStatus(),
                participation.getCreatedAt(),
                participation.getUpdatedAt()
        );
    }
}
