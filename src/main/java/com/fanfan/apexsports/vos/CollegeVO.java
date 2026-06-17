package com.fanfan.apexsports.vos;

import com.fanfan.apexsports.entities.College;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 学院响应 VO
 * <p>
 * 返回给前端的学院视图对象，提供从 Entity 转换的静态工厂方法。
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
public class CollegeVO implements Serializable {

    private Long id;
    private String name;
    private String code;
    private String description;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /**
     * 从 Entity 转换为 VO
     *
     * @param college 学院实体
     * @return 学院 VO，如果实体为 null 则返回 null
     */
    public static CollegeVO from(College college) {
        if (college == null) {
            return null;
        }
        return new CollegeVO(
                college.getId(),
                college.getName(),
                college.getCode(),
                college.getDescription(),
                college.getCreatedAt(),
                college.getUpdatedAt()
        );
    }
}
