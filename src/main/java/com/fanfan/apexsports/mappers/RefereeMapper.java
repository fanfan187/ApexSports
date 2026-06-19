package com.fanfan.apexsports.mappers;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fanfan.apexsports.entities.Referee;
import com.fanfan.apexsports.vos.RefereeScheduleVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 裁判 Mapper
 * <p>
 * 继承 MyBatis-Plus BaseMapper，提供裁判表（as_referee）的 CRUD 基础操作，
 * 并扩展以下原生 XML SQL：
 * <ul>
 *     <li>4 表 join 赛程查询（as_referee_assignment → as_competition → as_event / as_venue）</li>
 *     <li>删除前校验：统计该裁判身上未结束比赛的活跃分配数</li>
 * </ul>
 * </p>
 *
 * <p><b>依赖说明：</b>{@link RefereeScheduleVO} 由裁判模块 Task 2 创建；
 * 若该类缺失，编译将失败，请先执行 Task 2。</p>
 *
 * <p><b>⚠️ 注意：</b>原生 XML SQL 中 {@code @TableLogic} 逻辑删除不生效，
 * 所有被 join 的表必须在 WHERE 子句显式添加 {@code deleted = 0} 过滤。</p>
 *
 * @author fanfan187
 * @version v1.0.0
 * @since v1.0.0
 */
@Mapper
public interface RefereeMapper extends BaseMapper<Referee> {

    /**
     * 查询指定裁判的赛程列表（4 表 join）
     * <p>
     * 跨表 join 路径：
     * <pre>
     * as_referee_assignment a
     *   JOIN as_competition c ON a.competition_id = c.id
     *   LEFT JOIN as_event    e ON c.event_id = e.id
     *   LEFT JOIN as_venue    v ON c.venue_id = v.id
     * </pre>
     * 过滤条件：
     * <ul>
     *     <li>a.referee_id = #{refereeId}</li>
     *     <li>a.deleted = 0 AND a.status = 1（仅查已分配且未取消）</li>
     *     <li>c / e / v 各表 deleted = 0</li>
     * </ul>
     * 结果按 c.start_time ASC 升序。
     * </p>
     *
     * <p><b>role 字段</b>：返回 TINYINT（1-主裁判 2-副裁判 3-计时员 4-记录员），
     * 不在 SQL 中做 CASE WHEN 转中文，由前端 StatusTag 组件映射。</p>
     *
     * @param refereeId 裁判ID（必填）
     * @return 赛程列表（按开始时间升序）
     */
    List<RefereeScheduleVO> selectRefereeSchedule(@Param("refereeId") Long refereeId);

    /**
     * 删除前校验：统计该裁判身上"活跃且比赛未结束"的分配数
     * <p>
     * 用于 DELETE /referees/{id} 接口的前置校验：若返回值 &gt; 0，
     * 说明该裁判仍有进行中或未开始的比赛任务，不可删除，
     * 应抛出 {@code BusinessException(REFEREE_HAS_ACTIVE_ASSIGNMENTS)}。
     * </p>
     *
     * <p>跨表 join：as_referee_assignment a JOIN as_competition c。</p>
     * <p>过滤条件：</p>
     * <ul>
     *     <li>a.referee_id = #{refereeId}</li>
     *     <li>a.deleted = 0 AND a.status = 1</li>
     *     <li>c.deleted = 0</li>
     *     <li>c.status IN (0, 1)（仅未开始 / 进行中，已结束和已取消的不计入）</li>
     * </ul>
     *
     * @param refereeId 裁判ID（必填）
     * @return 活跃分配数（0 表示可安全删除）
     */
    Long countActiveAssignmentsByRefereeId(@Param("refereeId") Long refereeId);
}
