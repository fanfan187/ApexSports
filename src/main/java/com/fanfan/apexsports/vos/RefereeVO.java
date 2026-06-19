package com.fanfan.apexsports.vos;

import com.fanfan.apexsports.entities.Referee;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 裁判响应 VO
 * <p>
 * 返回给前端的裁判视图对象，提供从 Entity 转换的静态工厂方法。
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
public class RefereeVO implements Serializable {

    private Long id;
    private String name;
    private Integer gender;
    private String phone;
    private String level;
    private Integer status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /**
     * 从 Entity 转换为 VO
     *
     * @param referee 裁判实体
     * @return 裁判 VO，如果实体为 null 则返回 null
     */
    public static RefereeVO from(Referee referee) {
        if (referee == null) {
            return null;
        }
        return new RefereeVO(
                referee.getId(),
                referee.getName(),
                referee.getGender(),
                referee.getPhone(),
                referee.getLevel(),
                referee.getStatus(),
                referee.getCreatedAt(),
                referee.getUpdatedAt()
        );
    }
}
