package com.fanfan.apexsports.vos;

import com.fanfan.apexsports.entities.RefereeAssignment;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

/**
 * 裁判分配响应 VO
 * <p>
 * 返回给前端的裁判分配视图对象，提供从 Entity 转换的静态工厂方法。
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
public class RefereeAssignmentVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 分配记录ID */
    private Long id;

    /** 比赛场次ID */
    private Long competitionId;

    /** 裁判ID */
    private Long refereeId;

    /** 裁判姓名（关联字段，外部传入） */
    private String refereeName;

    /** 角色: 1-主裁判, 2-副裁判, 3-计时员, 4-记录员 */
    private Integer role;

    /** 状态: 1-已分配 */
    private Integer status;

    /**
     * 从 Entity 转换为 VO
     *
     * @param assignment    裁判分配实体
     * @param refereeName   裁判姓名
     * @return 裁判分配 VO，如果实体为 null 则返回 null
     */
    public static RefereeAssignmentVO from(RefereeAssignment assignment, String refereeName) {
        if (assignment == null) {
            return null;
        }
        return new RefereeAssignmentVO(
                assignment.getId(),
                assignment.getCompetitionId(),
                assignment.getRefereeId(),
                refereeName,
                assignment.getRole(),
                assignment.getStatus()
        );
    }
}
