package com.fanfan.apexsports.vos;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * 比赛场次详情响应 VO
 * <p>
 * 返回给前端的比赛场次详情视图对象，聚合比赛基本信息、裁判分配、器材分配、
 * 个人参赛者与团体参赛者列表。提供静态工厂方法从已组装的子 VO 列表构造。
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
public class CompetitionDetailVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 比赛场次基本信息 */
    private CompetitionVO competition;

    /** 裁判分配列表 */
    private List<RefereeAssignmentVO> referees;

    /** 器材分配列表 */
    private List<CompetitionEquipmentVO> equipments;

    /** 个人参赛者安排列表 */
    private List<ParticipantArrangementVO> participants;

    /** 团体参赛者安排列表 */
    private List<TeamParticipantArrangementVO> teamParticipants;

    /**
     * 从已组装的子 VO 构造详情 VO
     *
     * @param competition      比赛场次 VO
     * @param referees         裁判分配 VO 列表
     * @param equipments       器材分配 VO 列表
     * @param participants     个人参赛安排 VO 列表
     * @param teamParticipants 团体参赛安排 VO 列表
     * @return 比赛场次详情 VO，如果 competition 为 null 则返回 null
     */
    public static CompetitionDetailVO from(CompetitionVO competition,
                                           List<RefereeAssignmentVO> referees,
                                           List<CompetitionEquipmentVO> equipments,
                                           List<ParticipantArrangementVO> participants,
                                           List<TeamParticipantArrangementVO> teamParticipants) {
        if (competition == null) {
            return null;
        }
        return new CompetitionDetailVO(
                competition,
                referees,
                equipments,
                participants,
                teamParticipants
        );
    }
}
