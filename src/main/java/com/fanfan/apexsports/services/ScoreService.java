package com.fanfan.apexsports.services;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.fanfan.apexsports.dtos.BatchScoreDTO;
import com.fanfan.apexsports.dtos.BatchTeamScoreDTO;
import com.fanfan.apexsports.dtos.ScoreQueryDTO;
import com.fanfan.apexsports.dtos.ScoreUpdateDTO;
import com.fanfan.apexsports.dtos.SingleScoreDTO;
import com.fanfan.apexsports.vos.CompetitionScoreVO;
import com.fanfan.apexsports.vos.ScoreDetailVO;
import com.fanfan.apexsports.vos.ScoreEntryResultVO;
import com.fanfan.apexsports.vos.ScoreVO;

/**
 * 成绩服务接口
 * <p>
 * 定义成绩模块对外的业务方法：个人/团体成绩录入（含破纪录判定与积分联动）、
 * 成绩分页查询、单条修改、删除以及比赛场次成绩汇总查询。
 * 写操作（录入/修改/删除）需要管理员权限（由实现层校验），读操作对所有角色开放。
 * </p>
 * <p>
 * <b>接口分阶段演进说明：</b>本接口定义 Todo 9 阶段的 3 个录入方法（71/72/73），
 * 以及 Todo 11 阶段的 4 个查询/维护方法。后者当前在实现层为 stub，
 * 待 Todo 11 完成查询/修改/删除/汇总逻辑后补全。
 * </p>
 *
 * @author fanfan187
 * @version v1.0.0
 * @since v1.0.0
 */
public interface ScoreService {

    /**
     * 批量录入个人成绩
     * <p>
     * 需要管理员权限。校验比赛场次未结束，逐条处理成绩条目：
     * 参赛记录唯一性校验、查询参赛记录与项目类别、按名次计算积分、
     * 判定是否破校记录（径赛越小越优/田赛越大越优），破纪录时插入新 Record 并作废旧 Record，
     * 写入 Score 行并联动累计学院积分汇总（含奖牌计数）。
     * </p>
     *
     * @param dto 批量录入请求（含比赛场次ID与成绩条目列表）
     * @return 录入结果（是否触发破纪录、成功条数）
     */
    ScoreEntryResultVO batchAddScores(BatchScoreDTO dto);

    /**
     * 录入单条个人成绩
     * <p>
     * 需要管理员权限。语义等价于 {@link #batchAddScores} 的单条版本，
     * competitionId 由调用方（Controller 路径参数）显式注入。
     * 若 {@code dto.points} 非空，则覆盖按 rank 自动计算的积分。
     * </p>
     *
     * @param dto           单条成绩录入请求
     * @param competitionId 比赛场次ID
     * @return 录入结果（是否破纪录、成功条数恒为 1）
     */
    ScoreEntryResultVO addScore(SingleScoreDTO dto, Long competitionId);

    /**
     * 批量录入团体成绩
     * <p>
     * 需要管理员权限。逻辑与 {@link #batchAddScores} 对应，差异在于：
     * 团体参赛记录唯一性校验、破纪录按学院维度（athleteId 置 null）、积分联动 isTeam=true。
     * </p>
     *
     * @param dto 批量团体成绩录入请求
     * @return 录入结果（是否破纪录、成功条数）
     */
    ScoreEntryResultVO batchAddTeamScores(BatchTeamScoreDTO dto);

    /**
     * 成绩分页查询
     * <p>
     * 所有角色可查询。支持按比赛场次/项目可选过滤，结果为连表 ScoreDetailVO 列表。
     * </p>
     *
     * @param query 分页查询条件
     * @return 分页结果
     */
    IPage<ScoreDetailVO> getScorePage(ScoreQueryDTO query);

    /**
     * 修改成绩
     * <p>
     * 需要管理员权限。部分更新语义，未提供的字段（null）不修改；
     * 修改后需触发破纪录重判与积分重算（由 Todo 11 实现）。
     * </p>
     *
     * @param dto 修改请求
     * @return 修改后的成绩 VO
     */
    ScoreVO updateScore(ScoreUpdateDTO dto);

    /**
     * 删除成绩（逻辑删除）
     * <p>
     * 需要管理员权限。删除后需回滚学院积分汇总与破纪录联动（由 Todo 11 实现）。
     * </p>
     *
     * @param id 成绩ID
     */
    void deleteScoreById(Long id);

    /**
     * 查询比赛场次成绩汇总
     * <p>
     * 所有角色可查询。返回该场次下的排名列表与项目/场次名称聚合（由 Todo 11 实现）。
     * </p>
     *
     * @param competitionId 比赛场次ID
     * @return 场次成绩汇总 VO
     */
    CompetitionScoreVO getCompetitionScores(Long competitionId);
}
