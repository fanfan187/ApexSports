package com.fanfan.apexsports.vos;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

/**
 * 破纪录汇总 VO
 * <p>
 * 严格映射 Docs/13-统计与看板模块.md §87 的响应字段（camelCase 契约）。
 * {@code records} 为所有 status=1 有效记录，按 {@code breakDate}（即 Record.recordDate）
 * 降序、id 降序排列。{@code totalCount} 等于 {@code records.size()}。
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
public class BreakingRecordsVO implements Serializable {

    /** 破纪录总数 */
    private Integer totalCount;

    /** 破纪录项列表（按 breakDate 降序） */
    private List<BreakingRecordItemVO> records;
}