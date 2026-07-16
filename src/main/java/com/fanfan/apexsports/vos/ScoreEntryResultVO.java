package com.fanfan.apexsports.vos;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

/**
 * 成绩录入结果响应 VO
 * <p>
 * 成绩录入（批量/单条）接口的响应对象，回传本次录入是否触发破纪录及成功条数。
 * isRecordBreaking 在录入响应用 {@code Boolean}（语义「本次是否破纪录」），
 * 与列表响应 {@link ScoreVO}/{@link ScoreDetailVO} 的 {@code Integer isRecordBreaking}（0/1 标志位）语义不同，故类型刻意区分。
 * 使用 {@code @Getter} 保证不可变性，不允许外部修改。
 * </p>
 *
 * @author fanfan187
 * @version v1.0.0
 * @since v1.0.0
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ScoreEntryResultVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 本次录入是否破纪录（录入响应用 Boolean，非 Integer 标志位） */
    private Boolean isRecordBreaking;

    /** 成功录入条数 */
    private Integer successCount;
}
