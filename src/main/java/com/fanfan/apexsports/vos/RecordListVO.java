package com.fanfan.apexsports.vos;

import com.fanfan.apexsports.entities.Record;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 记录列表响应 VO
 * <p>
 * 返回给前端的记录列表视图对象，提供从 Entity 转换的静态工厂方法。
 * 使用 {@code @Getter} 保证不可变性，不允许外部修改。
 * 字段名严格对齐 API 文档（Docs/11-记录管理模块.md L41-50），不沿用旧 RecordVO 的
 * achievement/achievementDate 别名。
 * </p>
 *
 * @author fanfan187
 * @version v1.0.0
 * @since v1.0.0
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class RecordListVO implements Serializable {

    /** 记录ID */
    private Long id;

    /** 项目名称（由 Logic 层根据 eventId 查询后传入） */
    private String eventName;

    /** 记录类型: 1-校记录, 2-市记录, 3-省记录 */
    private Integer recordType;

    /** 运动员姓名（个人项目） */
    private String athleteName;

    /** 学院名称（团体项目） */
    private String collegeName;

    /** 成绩值 */
    private BigDecimal score;

    /** 成绩显示 */
    private String scoreDisplay;

    /** 创造记录日期 */
    private LocalDate recordDate;

    /** 创造记录的比赛名称 */
    private String competitionName;

    /** 状态: 0-已作废, 1-有效 */
    private Integer status;

    /**
     * 从 Entity 转换为列表 VO
     *
     * @param record    记录实体
     * @param eventName 项目名称（由 Logic 层查询 Event 后传入，不在此 VO 内查 DB）
     * @return 记录列表 VO，如果实体为 null 则返回 null
     */
    public static RecordListVO from(Record record, String eventName) {
        if (record == null) {
            return null;
        }
        return new RecordListVO(
                record.getId(),
                eventName,
                record.getRecordType(),
                record.getAthleteName(),
                record.getCollegeName(),
                record.getScore(),
                record.getScoreDisplay(),
                record.getRecordDate(),
                record.getCompetitionName(),
                record.getStatus()
        );
    }
}
