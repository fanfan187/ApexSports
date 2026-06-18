package com.fanfan.apexsports.vos;

import com.fanfan.apexsports.entities.Venue;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 场地响应 VO
 * <p>
 * 返回给前端的场地视图对象，提供从 Entity 转换的静态工厂方法。
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
public class VenueVO implements Serializable {

    private Long id;
    private String name;
    private String code;
    private Integer type;
    private String location;
    private Integer capacity;
    private Integer status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /**
     * 从 Entity 转换为 VO
     *
     * @param venue 场地实体
     * @return 场地 VO，如果实体为 null 则返回 null
     */
    public static VenueVO from(Venue venue) {
        if (venue == null) {
            return null;
        }
        return new VenueVO(
                venue.getId(),
                venue.getName(),
                venue.getCode(),
                venue.getType(),
                venue.getLocation(),
                venue.getCapacity(),
                venue.getStatus(),
                venue.getCreatedAt(),
                venue.getUpdatedAt()
        );
    }
}
