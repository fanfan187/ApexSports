package com.fanfan.apexsports.vos;

import com.fanfan.apexsports.entities.CompetitionEquipment;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

/**
 * 比赛器材分配响应 VO
 * <p>
 * 返回给前端的比赛器材分配视图对象，提供从 Entity 转换的静态工厂方法。
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
public class CompetitionEquipmentVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 分配记录ID */
    private Long id;

    /** 比赛场次ID */
    private Long competitionId;

    /** 器材ID */
    private Long equipmentId;

    /** 器材名称（关联字段，外部传入） */
    private String equipmentName;

    /** 分配数量 */
    private Integer quantity;

    /** 状态: 0-已分配, 1-使用中, 2-已归还 */
    private Integer status;

    /** 备注 */
    private String remark;

    /**
     * 从 Entity 转换为 VO
     *
     * @param equipment      比赛器材分配实体
     * @param equipmentName  器材名称
     * @return 比赛器材分配 VO，如果实体为 null 则返回 null
     */
    public static CompetitionEquipmentVO from(CompetitionEquipment equipment, String equipmentName) {
        if (equipment == null) {
            return null;
        }
        return new CompetitionEquipmentVO(
                equipment.getId(),
                equipment.getCompetitionId(),
                equipment.getEquipmentId(),
                equipmentName,
                equipment.getQuantity(),
                equipment.getStatus(),
                equipment.getRemark()
        );
    }
}
