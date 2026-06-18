package com.fanfan.apexsports.vos;

import com.fanfan.apexsports.entities.Record;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 记录响应 VO
 * <p>
 * 返回给前端的记录视图对象，提供从 Entity 转换的静态工厂方法。
 * 注意：本 VO 进行了 DB→API 字段映射，对外暴露的字段名与数据库字段名不同：
 * <ul>
 *     <li>{@code scoreDisplay} → {@code achievement}（成绩显示）</li>
 *     <li>{@code recordDate} → {@code achievementDate}（创纪录日期，ISO-8601 字符串）</li>
 * </ul>
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
public class RecordVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 记录ID */
    private Long id;

    /** 记录类型: 1-校记录, 2-市记录, 3-省记录 */
    private Integer recordType;

    /** 运动员姓名 */
    private String athleteName;

    /** 学院名称 */
    private String collegeName;

    /** 成绩(显示值): 对应 DB 字段 score_display */
    private String achievement;

    /** 创纪录日期(ISO-8601 字符串, 如 2024-05-20): 对应 DB 字段 record_date */
    private String achievementDate;

    /** 创造记录的比赛名称 */
    private String competitionName;

    /** 创建时间 */
    private LocalDateTime createdAt;

    /**
     * 从 Entity 转换为 VO（含 DB→API 字段映射）
     * <p>
     * 字段映射说明：
     * <ul>
     *     <li>{@code achievement} ← {@link Record#getScoreDisplay()}</li>
     *     <li>{@code achievementDate} ← {@link Record#getRecordDate()} 转为 ISO-8601 字符串</li>
     * </ul>
     *
     * @param record 记录实体
     * @return 记录 VO，如果实体为 null 则返回 null
     */
    public static RecordVO from(Record record) {
        if (record == null) {
            return null;
        }
        return new RecordVO(
                record.getId(),
                record.getRecordType(),
                record.getAthleteName(),
                record.getCollegeName(),
                record.getScoreDisplay(),                                                                       // DB scoreDisplay → API achievement
                record.getRecordDate() != null ? record.getRecordDate().toString() : null,                       // DB recordDate(LocalDate) → API achievementDate(ISO-8601 String)
                record.getCompetitionName(),
                record.getCreatedAt()
        );
    }
}
