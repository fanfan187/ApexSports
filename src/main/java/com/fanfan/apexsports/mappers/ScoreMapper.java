package com.fanfan.apexsports.mappers;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.fanfan.apexsports.entities.Score;
import com.fanfan.apexsports.vos.ScoreDetailVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 成绩 Mapper
 * <p>
 * 继承 MyBatis-Plus BaseMapper，提供 as_score 表的 CRUD 基础操作，
 * 并提供连表分页查询（as_score JOIN as_participation/as_athlete/as_college/as_event/as_competition）
 * </p>
 *
 * <p><b>返回类型说明：</b>连表查询返回 {@link ScoreDetailVO}，
 * 字段与 xml resultMap 完全对齐（id/participationId/teamParticipationId/athleteName/
 * collegeName/eventName/competitionName/rank/points/scoreValue/scoreDisplay/
 * isRecordBreaking/createdAt）。</p>
 *
 * @author fanfan187
 * @version v1.0.0
 * @since v1.0.0
 */
@Mapper
public interface ScoreMapper extends BaseMapper<Score> {

    /**
     * 连表分页查询成绩明细
     * <p>
     * JOIN as_participation / as_athlete / as_college / as_competition / as_event 取名称，
     * 返回每行为 {@link ScoreDetailVO}。
     * 分页由 MyBatis-Plus 插件注入，xml 中不写 LIMIT。
     * </p>
     * <p>
     * competitionId / eventId 均为可选过滤：as_score 表无 event_id 列，
     * 场次过滤直接按 {@code s.competition_id}，项目过滤经连表按
     * {@code cp.event_id}（as_competition.event_id）实现。
     * </p>
     *
     * @param page          分页参数（由调用方 new Page）
     * @param competitionId 比赛场次ID(nullable 可选过滤)
     * @param eventId       项目ID(nullable 可选过滤，经 as_competition.event_id 连表过滤)
     * @return 每行 ScoreDetailVO
     */
    IPage<ScoreDetailVO> findByPage(IPage<ScoreDetailVO> page,
                                    @Param("competitionId") Long competitionId,
                                    @Param("eventId") Long eventId);
}
