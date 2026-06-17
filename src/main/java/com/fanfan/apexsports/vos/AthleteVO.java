package com.fanfan.apexsports.vos;

import com.fanfan.apexsports.entities.Athlete;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 运动员响应 VO
 * <p>
 * 返回给前端的运动员视图对象，提供从 Entity 转换的静态工厂方法。
 * 使用 {@code @Getter} 保证不可变性，不允许外部修改。
 * 支持两种转换场景：列表视图（含学院名称）和详情视图（含学院名称与代码）
 * </p>
 *
 * @author fanfan187
 * @version v1.0.0
 * @since v1.0.0
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class AthleteVO implements Serializable {

    private Long id;
    private Long collegeId;
    private String collegeName;
    private String collegeCode;
    private String name;
    private String studentNo;
    private Integer gender;
    private String grade;
    private String phone;
    private Integer status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /**
     * 从 Entity 转换为 VO（列表视图）
     * <p>
     * 仅填充学院名称，不包含学院代码，适用于列表展示场景
     * </p>
     *
     * @param athlete     运动员实体
     * @param collegeName 学院名称
     * @return 运动员 VO，如果实体为 null 则返回 null
     */
    public static AthleteVO from(Athlete athlete, String collegeName) {
        if (athlete == null) {
            return null;
        }
        return new AthleteVO(
                athlete.getId(),
                athlete.getCollegeId(),
                collegeName,
                null,
                athlete.getName(),
                athlete.getStudentNo(),
                athlete.getGender(),
                athlete.getGrade(),
                athlete.getPhone(),
                athlete.getStatus(),
                athlete.getCreatedAt(),
                athlete.getUpdatedAt()
        );
    }

    /**
     * 从 Entity 转换为 VO（详情视图）
     * <p>
     * 同时填充学院名称与代码，适用于详情展示场景
     * </p>
     *
     * @param athlete     运动员实体
     * @param collegeName 学院名称
     * @param collegeCode 学院代码
     * @return 运动员 VO，如果实体为 null 则返回 null
     */
    public static AthleteVO from(Athlete athlete, String collegeName, String collegeCode) {
        if (athlete == null) {
            return null;
        }
        return new AthleteVO(
                athlete.getId(),
                athlete.getCollegeId(),
                collegeName,
                collegeCode,
                athlete.getName(),
                athlete.getStudentNo(),
                athlete.getGender(),
                athlete.getGrade(),
                athlete.getPhone(),
                athlete.getStatus(),
                athlete.getCreatedAt(),
                athlete.getUpdatedAt()
        );
    }
}
