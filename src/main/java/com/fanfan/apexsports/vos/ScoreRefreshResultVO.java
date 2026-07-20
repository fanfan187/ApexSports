package com.fanfan.apexsports.vos;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

/**
 * 积分刷新结果 VO
 * <p>
 * 返回给前端的手动全量重算结果视图对象，对应接口84（{@code POST /api/rankings/refresh}）。
 * 由 Logic 层在 {@code @Transactional} 全量重算 + 缓存驱逐后组装。
 * 使用 {@code @Getter} 保证不可变性，不允许外部修改。
 * 字段参考 Docs/12-学院积分与排名模块.md 接口84 契约。
 * </p>
 *
 * @author fanfan187
 * @version v1.0.0
 * @since v1.0.0
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ScoreRefreshResultVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 刷新时间（yyyy-MM-dd HH:mm:ss 格式字符串） */
    private String refreshTime;

    /** 受影响学院数（聚合 INSERT 的行数） */
    private Integer affectedColleges;

    /** 参与项目总数（as_score 中 distinct event_id 的记录数） */
    private Integer totalEvents;
}