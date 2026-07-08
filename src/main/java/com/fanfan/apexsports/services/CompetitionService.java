package com.fanfan.apexsports.services;

import com.fanfan.apexsports.common.result.PageResult;
import com.fanfan.apexsports.dtos.BatchArrangeDTO;
import com.fanfan.apexsports.dtos.BatchTeamArrangeDTO;
import com.fanfan.apexsports.dtos.CompetitionCreateDTO;
import com.fanfan.apexsports.dtos.CompetitionQueryDTO;
import com.fanfan.apexsports.dtos.CompetitionUpdateDTO;
import com.fanfan.apexsports.dtos.UpdateCompetitionStatusDTO;
import com.fanfan.apexsports.vos.CompetitionDetailVO;
import com.fanfan.apexsports.vos.CompetitionVO;
import com.fanfan.apexsports.vos.ParticipantArrangementVO;
import com.fanfan.apexsports.vos.ScheduleBookletVO;
import com.fanfan.apexsports.vos.TeamParticipantArrangementVO;

import java.util.List;

/**
 * 比赛场次服务接口
 * <p>
 * 定义比赛场次的分页查询、详情查询、创建（含径赛互斥与场地冲突检测）、
 * 修改（部分更新语义、终态拦截、冲突重检）、删除（状态校验、级联清理）、
 * 状态流转（正向流转合法性校验）以及秩序册聚合查询等业务方法。
 * 所有角色均可执行读操作，写操作需要管理员权限（由实现层校验）。
 * </p>
 * <p>
 * <b>接口分阶段演进说明：</b>本接口当前定义 T9（场次 CRUD + 状态流转 + 秩序册）的 7 个方法，
 * 以及 T10（运动员编排 + 团体编排）的 5 个方法。编排方法支持将个人/团体参赛记录批量编排到场次、
 * 查询场次参赛者、移除编排等业务能力。
 * </p>
 *
 * @author fanfan187
 * @version v1.0.0
 * @since v1.0.0
 */
public interface CompetitionService {

    /**
     * 新增比赛场次
     * <p>
     * 需要管理员权限。校验 startTime 早于 endTime、项目存在且启用、场地存在且可用，
     * 执行径赛互斥校验（category=1 时同时间段不得有其他径赛）与场地冲突校验
     * （同场地同时间段不得有其他场次）。新建场次默认状态为未开始（status=0）。
     * </p>
     *
     * @param dto 新增比赛场次请求数据
     * @return 新创建的比赛场次 VO（含项目名称/类型/类别、场地名称）
     */
    CompetitionVO create(CompetitionCreateDTO dto);

    /**
     * 分页查询比赛场次列表
     * <p>
     * 所有角色均可查询，支持按日期/场地/项目/状态/轮次可选过滤，
     * 批量关联项目与场地名称以构建列表视图。
     * </p>
     *
     * @param dto 分页查询条件
     * @return 分页结果
     */
    PageResult<CompetitionVO> page(CompetitionQueryDTO dto);

    /**
     * 根据ID查询比赛场次详情
     * <p>
     * 所有角色均可查询。聚合比赛基本信息、裁判分配、器材分配、
     * 个人参赛者与团体参赛者列表。
     * </p>
     *
     * @param id 比赛场次ID
     * @return 比赛场次详情 VO
     */
    CompetitionDetailVO getById(Long id);

    /**
     * 修改比赛场次信息
     * <p>
     * 需要管理员权限，支持部分更新语义。比赛状态为已结束(2)或已取消(3)时禁止修改。
     * 修改时间/场地/项目时重新触发径赛互斥与场地冲突检测（排除自身）。
     * </p>
     *
     * @param id  比赛场次ID
     * @param dto 修改比赛场次请求数据
     * @return 修改后的比赛场次 VO
     */
    CompetitionVO update(Long id, CompetitionUpdateDTO dto);

    /**
     * 删除比赛场次（逻辑删除）
     * <p>
     * 需要管理员权限。比赛状态为进行中(1)时禁止删除。
     * 级联处理：裁判分配逻辑删除、器材使用记录标记归还。
     * </p>
     *
     * @param id 比赛场次ID
     */
    void deleteById(Long id);

    /**
     * 变更比赛状态（仅正向流转）
     * <p>
     * 需要管理员权限。合法流转：未开始(0)→进行中(1)/已取消(3)、进行中(1)→已结束(2)；
     * 已结束(2)与已取消(3)为终态，不可流转。
     * </p>
     *
     * @param id  比赛场次ID
     * @param dto 状态更新请求数据
     */
    void updateStatus(Long id, UpdateCompetitionStatusDTO dto);

    /**
     * 获取秩序册（赛程一览表）
     * <p>
     * 所有角色均可查询。按开始时间升序返回赛程列表，聚合项目信息、场地名称、
     * 参赛人数与主裁判姓名。结果带缓存（由 DAO 层 cache-aside 管理）。
     * </p>
     *
     * @param date 日期筛选（格式 yyyy-MM-dd），可为 null 表示聚合全部日期
     * @return 秩序册条目列表
     */
    List<ScheduleBookletVO> getBooklet(String date);

    /**
     * 批量编排个人参赛者到场次
     * <p>
     * 需要管理员权限。校验场次未结束、项目为个人项目、参赛记录存在且属于该项目的运动员、
     * 参赛记录未被编排、运动员无时间冲突，通过后批量写入 competitionId/laneNo/bibNumber。
     * </p>
     *
     * @param competitionId 比赛场次ID
     * @param dto           批量编排请求（含 participationId / laneNo / bibNumber 列表）
     * @return 编排后的个人参赛安排 VO 列表
     */
    List<ParticipantArrangementVO> arrangeParticipants(Long competitionId, BatchArrangeDTO dto);

    /**
     * 查询场次的个人参赛者编排列表
     * <p>
     * 所有角色可查询。按 competitionId 筛选参赛记录，批量关联运动员姓名与学院名称。
     * </p>
     *
     * @param competitionId 比赛场次ID
     * @return 个人参赛安排 VO 列表，无编排记录返回空列表
     */
    List<ParticipantArrangementVO> getParticipants(Long competitionId);

    /**
     * 移除场次的个人参赛编排
     * <p>
     * 需要管理员权限。校验场次未结束、参赛记录存在且确实编排到此场次，
     * 通过后将 competitionId/laneNo/bibNumber 置空。
     * </p>
     *
     * @param competitionId  比赛场次ID
     * @param participationId 参赛记录ID
     */
    void removeParticipant(Long competitionId, Long participationId);

    /**
     * 批量编排团体参赛者到场次
     * <p>
     * 需要管理员权限。校验场次未结束、项目为集体项目、团体参赛记录存在且未被编排、
     * 学院无集体项目时间冲突，通过后批量写入 competitionId。
     * </p>
     *
     * @param competitionId 比赛场次ID
     * @param dto           批量团体编排请求（含 teamParticipationId 列表）
     * @return 编排后的团体参赛安排 VO 列表
     */
    List<TeamParticipantArrangementVO> arrangeTeamParticipants(Long competitionId, BatchTeamArrangeDTO dto);

    /**
     * 移除场次的团体参赛编排
     * <p>
     * 需要管理员权限。校验场次未结束、团体参赛记录存在且确实编排到此场次，
     * 通过后将 competitionId 置空。
     * </p>
     *
     * @param competitionId       比赛场次ID
     * @param teamParticipationId 团体参赛记录ID
     */
    void removeTeamParticipant(Long competitionId, Long teamParticipationId);
}
