package com.fanfan.apexsports.vos;

import com.fanfan.apexsports.entities.Event;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 比赛项目响应 VO
 * <p>
 * 返回给前端的比赛项目视图对象，提供从 Entity 转换的静态工厂方法。
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
public class EventVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 项目ID */
    private Long id;

    /** 项目名称 */
    private String name;

    /** 项目代码 */
    private String code;

    /** 类型: 1-个人项目, 2-集体项目 */
    private Integer type;

    /** 类别: 1-径赛, 2-田赛 */
    private Integer category;

    /** 性别限制: 0-女子, 1-男子, 2-混合 */
    private Integer gender;

    /** 成绩单位(秒/米/分等) */
    private String unit;

    /** 集体项目最大参赛人数 */
    private Integer maxParticipants;

    /** 个人最多报名该项目次数 */
    private Integer maxIndividualEvents;

    /** 项目描述 */
    private String description;

    /** 状态: 0-停用, 1-启用 */
    private Integer status;

    /** 创建时间 */
    private LocalDateTime createdAt;

    /** 更新时间 */
    private LocalDateTime updatedAt;

    /**
     * 从 Entity 转换为 VO
     *
     * @param event 比赛项目实体
     * @return 比赛项目 VO，如果实体为 null 则返回 null
     */
    public static EventVO from(Event event) {
        if (event == null) {
            return null;
        }
        return new EventVO(
                event.getId(),
                event.getName(),
                event.getCode(),
                event.getType(),
                event.getCategory(),
                event.getGender(),
                event.getUnit(),
                event.getMaxParticipants(),
                event.getMaxIndividualEvents(),
                event.getDescription(),
                event.getStatus(),
                event.getCreatedAt(),
                event.getUpdatedAt()
        );
    }
}
