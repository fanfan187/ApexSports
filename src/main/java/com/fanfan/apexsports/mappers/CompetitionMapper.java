package com.fanfan.apexsports.mappers;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fanfan.apexsports.entities.Competition;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

/**
 * 比赛场次 Mapper
 * <p>
 * 继承 MyBatis-Plus BaseMapper，提供比赛场次表的 CRUD 基础操作
 * </p>
 *
 * @author fanfan187
 * @version v1.0.0
 * @since v1.0.0
 */
@Mapper
public interface CompetitionMapper extends BaseMapper<Competition> {

    // ==================== 聚合统计查询（XML 实现，等价原 JdbcTemplate SQL） ====================
    // 对应 CompetitionMapper.xml；Logic/DAO 迁移时直接调用，禁止再写 JdbcTemplate 原生 SQL

    /**
     * 统计单场次的团体参赛人数（as_team_participation.participant_count 之和）。
     * <p>
     * 对应 CompetitionDAO#countParticipantsByCompetitionId 的团体部分（原 L189-192）。
     * 个人参赛数（COUNT）由调用方使用 BaseMapper selectCount 完成，无需在此暴露。
     * </p>
     *
     * @param competitionId 场次ID
     * @return 团体参赛人数；无记录返回 0（SQL 中 COALESCE 保证非 null）
     */
    Long countTeamParticipantSum(@Param("competitionId") Long competitionId);

    /**
     * 批量统计多场次的个人参赛数（as_participation 行数 GROUP BY）。
     * <p>
     * 对应 CompetitionDAO#batchQueryParticipantCounts 的个人部分（原 L614-618）。
     * 仅返回有记录的场次；无记录的场次由调用方补 0。
     * </p>
     *
     * @param competitionIds 场次ID集合（非空）
     * @return {@code List<Map>}，每个 Map 的 key 为 {@code competition_id} 与 {@code cnt}
     */
    List<Map<String, Object>> batchCountIndividualParticipants(@Param("ids") List<Long> competitionIds);

    /**
     * 批量统计多场次的团体参赛数（as_team_participation.participant_count 求和 GROUP BY）。
     * <p>
     * 对应 CompetitionDAO#batchQueryParticipantCounts 的团体部分（原 L624-128）。
     * 仅返回有记录的场次；无记录的场次由调用方补 0。
     * </p>
     *
     * @param competitionIds 场次ID集合（非空）
     * @return {@code List<Map>}，每个 Map 的 key 为 {@code competition_id} 与 {@code cnt}
     */
    List<Map<String, Object>> batchSumTeamParticipants(@Param("ids") List<Long> competitionIds);
}
