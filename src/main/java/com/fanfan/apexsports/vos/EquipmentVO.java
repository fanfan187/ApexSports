package com.fanfan.apexsports.vos;

import com.fanfan.apexsports.entities.Equipment;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 器材响应 VO
 * <p>
 * 返回给前端的器材视图对象，提供从 Entity 转换的静态工厂方法。
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
public class EquipmentVO implements Serializable {

    private Long id;
    private String name;
    private String code;
    private String type;
    private String unit;
    private Integer totalQuantity;
    private Integer availableQuantity;
    private Integer status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /**
     * 从 Entity 转换为 VO
     *
     * @param equipment 器材实体
     * @return 器材 VO，如果实体为 null 则返回 null
     */
    public static EquipmentVO from(Equipment equipment) {
        if (equipment == null) {
            return null;
        }
        return new EquipmentVO(
                equipment.getId(),
                equipment.getName(),
                equipment.getCode(),
                equipment.getType(),
                equipment.getUnit(),
                equipment.getTotalQuantity(),
                equipment.getAvailableQuantity(),
                equipment.getStatus(),
                equipment.getCreatedAt(),
                equipment.getUpdatedAt()
        );
    }
}
