package com.fanfan.apexsports.vos;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 破纪录汇总单项 VO
 * <p>
 * 严格映射 Docs/13-统计与看板模块.md §87 的响应字段（camelCase 契约），
 * 为 {@link BreakingRecordsVO#getRecords()} 列表中每条破纪录项的字段定义。
 * </p>
 * <p>
 * 字段语义：
 * <ul>
 *   <li>{@code newScoreDisplay} —— 新纪录的成绩展示字符串。</li>
 *   <li>{@code oldScoreDisplay} —— 前驱旧最优记录的成绩展示字符串；
 *       首次设立纪录时（无前驱 status=0 记录）为 {@code null}（Docs §87 规则#3）。</li>
 *   <li>{@code recordType} —— 中文文案，取值 {@code 校记录 / 市记录 / 省记录}。</li>
 *   <li>{@code breakDate} —— 破纪录日期，格式 "yyyy-MM-dd"。</li>
 *   <li>{@code athleteName} —— 个人项目为运动员姓名；团体项目为 {@code null}
 *       （Docs §87 规则#5）。</li>
 * </ul>
 * </p>
 * <p>
 * 使用 {@code @Getter} 保证不可变性，无 {@code @Setter}，仅通过
 * {@code @AllArgsConstructor} 一次性构造赋值。
 * </p>
 *
 * @author fanfan187
 * @version v1.0.0
 * @since v1.0.0
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class BreakingRecordItemVO implements Serializable {

    /** 破纪录ID */
    private Long id;

    /** 项目名称 */
    private String eventName;

    /** 运动员姓名（个人项目）；团体项目为 null */
    private String athleteName;

    /** 学院名称（团体项目） */
    private String collegeName;

    /** 新纪录成绩展示 */
    private String newScoreDisplay;

    /** 前驱旧记录成绩展示；首次设立为 null */
    private String oldScoreDisplay;

    /** 记录类型中文：校记录 / 市记录 / 省记录 */
    private String recordType;

    /** 破纪录日期，格式 "yyyy-MM-dd" */
    private String breakDate;
}