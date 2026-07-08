package com.fanfan.apexsports.controllers;

import com.fanfan.apexsports.common.result.PageResult;
import com.fanfan.apexsports.common.result.Result;
import com.fanfan.apexsports.dtos.AssignEquipmentDTO;
import com.fanfan.apexsports.dtos.AssignRefereeDTO;
import com.fanfan.apexsports.dtos.BatchArrangeDTO;
import com.fanfan.apexsports.dtos.BatchTeamArrangeDTO;
import com.fanfan.apexsports.dtos.CompetitionCreateDTO;
import com.fanfan.apexsports.dtos.CompetitionQueryDTO;
import com.fanfan.apexsports.dtos.CompetitionUpdateDTO;
import com.fanfan.apexsports.dtos.UpdateCompetitionStatusDTO;
import com.fanfan.apexsports.dtos.UpdateEquipmentStatusDTO;
import com.fanfan.apexsports.services.CompetitionEquipmentService;
import com.fanfan.apexsports.services.CompetitionService;
import com.fanfan.apexsports.services.RefereeAssignmentService;
import com.fanfan.apexsports.vos.CompetitionDetailVO;
import com.fanfan.apexsports.vos.CompetitionEquipmentVO;
import com.fanfan.apexsports.vos.CompetitionVO;
import com.fanfan.apexsports.vos.ParticipantArrangementVO;
import com.fanfan.apexsports.vos.RefereeAssignmentVO;
import com.fanfan.apexsports.vos.ScheduleBookletVO;
import com.fanfan.apexsports.vos.TeamParticipantArrangementVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 赛程安排控制器
 * <p>
 * 整合比赛场次 CRUD、状态流转、运动员编排、团体编排、裁判分配、
 * 器材分配与秩序册查询共 18 个 REST 端点。读接口对所有角色开放，
 * 写接口需要管理员权限（由服务层校验）。
 * </p>
 * <p>
 * <b>路由顺序说明：</b>静态路径（如 {@code /booklet}）必须在动态路径
 * {@code /{id}} 之前声明，避免被 {@code /{id}} 捕获。
 * </p>
 *
 * @author fanfan187
 * @version v1.0.0
 * @since v1.0.0
 */
@RestController
@RequestMapping("/competitions")
@RequiredArgsConstructor
@Tag(name = "赛程安排", description = "比赛场次CRUD/编排/裁判分配/器材分配/秩序册")
public class CompetitionController {

    private final CompetitionService competitionService;
    private final RefereeAssignmentService refereeAssignmentService;
    private final CompetitionEquipmentService competitionEquipmentService;

    // ==================== 场次 CRUD ====================

    /**
     * 创建比赛场次
     * <p>
     * 含径赛互斥校验与场地冲突校验，新建场次默认状态为未开始（status=0）。
     * </p>
     *
     * @param dto 新增比赛场次请求数据
     * @return 新创建的比赛场次 VO
     */
    @Operation(summary = "创建比赛场次", description = "管理员创建比赛场次，含径赛互斥与场地冲突检测")
    @PostMapping
    public Result<CompetitionVO> create(@RequestBody @Valid CompetitionCreateDTO dto) {
        return Result.success("创建成功", competitionService.create(dto));
    }

    /**
     * 分页查询比赛场次列表
     *
     * @param dto 分页查询条件（current/size/date/venueId/eventId/status/round 均可选）
     * @return 分页结果
     */
    @Operation(summary = "获取赛程列表", description = "分页查询赛程列表，支持按日期/场地/项目/状态/轮次筛选")
    @GetMapping
    public Result<PageResult<CompetitionVO>> page(@ModelAttribute CompetitionQueryDTO dto) {
        return Result.success(competitionService.page(dto));
    }

    /**
     * 获取秩序册（赛程一览表）
     * <p>
     * 静态路径，必须声明在 {@code /{id}} 之前。
     * </p>
     *
     * @param date 日期筛选（格式 yyyy-MM-dd），可为 null 表示聚合全部日期
     * @return 秩序册条目列表
     */
    @Operation(summary = "获取秩序册", description = "按时间升序返回赛程一览表，聚合项目/场地/参赛人数/主裁判")
    @GetMapping("/booklet")
    public Result<List<ScheduleBookletVO>> getBooklet(
            @Parameter(description = "日期筛选（yyyy-MM-dd），为空或 all 表示全部")
            @RequestParam(required = false, defaultValue = "all") String date) {
        return Result.success(competitionService.getBooklet(date));
    }

    /**
     * 根据 ID 获取比赛场次详情
     * <p>
     * 聚合比赛基本信息、裁判分配、器材分配、个人与团体参赛者列表。
     * </p>
     *
     * @param id 比赛场次ID
     * @return 比赛场次详情 VO
     */
    @Operation(summary = "获取比赛场次详情", description = "聚合比赛基本信息、裁判/器材/个人与团体参赛者")
    @GetMapping("/{id}")
    public Result<CompetitionDetailVO> getById(
            @Parameter(description = "比赛场次ID") @PathVariable Long id) {
        return Result.success("操作成功", competitionService.getById(id));
    }

    /**
     * 修改比赛场次信息
     * <p>
     * 支持部分更新；已结束(2)/已取消(3)时禁止修改；修改时间/场地/项目时重检冲突。
     * </p>
     *
     * @param id  比赛场次ID
     * @param dto 修改请求数据
     * @return 修改后的比赛场次 VO
     */
    @Operation(summary = "修改比赛场次", description = "管理员修改场次，终态拦截，改时间/场地/项目重检冲突")
    @PutMapping("/{id}")
    public Result<CompetitionVO> update(
            @Parameter(description = "比赛场次ID") @PathVariable Long id,
            @RequestBody @Valid CompetitionUpdateDTO dto) {
        return Result.success("修改成功", competitionService.update(id, dto));
    }

    /**
     * 删除比赛场次（逻辑删除）
     * <p>
     * 进行中(1)时禁止删除；级联处理裁判分配与器材使用记录。
     * </p>
     *
     * @param id 比赛场次ID
     * @return 操作结果
     */
    @Operation(summary = "删除比赛场次", description = "管理员逻辑删除场次，级联清理裁判分配与器材记录")
    @DeleteMapping("/{id}")
    public Result<Void> deleteById(
            @Parameter(description = "比赛场次ID") @PathVariable Long id) {
        competitionService.deleteById(id);
        return Result.success("删除成功", null);
    }

    // ==================== 状态流转 ====================

    /**
     * 变更比赛状态（仅正向流转）
     *
     * @param id  比赛场次ID
     * @param dto 状态更新请求数据
     * @return 操作结果
     */
    @Operation(summary = "比赛状态流转", description = "仅允许正向流转：未开始→进行中/已取消，进行中→已结束")
    @PutMapping("/{id}/status")
    public Result<Void> updateStatus(
            @Parameter(description = "比赛场次ID") @PathVariable Long id,
            @RequestBody @Valid UpdateCompetitionStatusDTO dto) {
        competitionService.updateStatus(id, dto);
        return Result.success("比赛状态已更新", null);
    }

    // ==================== 个人编排 ====================

    /**
     * 批量编排个人参赛者到场次
     * <p>
     * 校验场次未结束、项目为个人项目、参赛记录归属与时间冲突，分配道次与号码牌。
     * </p>
     *
     * @param id  比赛场次ID
     * @param dto 批量编排请求
     * @return 编排后的个人参赛安排 VO 列表
     */
    @Operation(summary = "批量编排运动员到场次", description = "分配道次与号码牌，含运动员时间冲突校验")
    @PostMapping("/{id}/participants")
    public Result<List<ParticipantArrangementVO>> arrangeParticipants(
            @Parameter(description = "比赛场次ID") @PathVariable Long id,
            @RequestBody @Valid BatchArrangeDTO dto) {
        return Result.success("编排成功", competitionService.arrangeParticipants(id, dto));
    }

    /**
     * 查询场次的个人参赛者编排列表
     *
     * @param id 比赛场次ID
     * @return 个人参赛安排 VO 列表，无编排记录返回空列表
     */
    @Operation(summary = "获取场次参赛运动员列表", description = "按场次筛选参赛记录，关联运动员姓名与学院")
    @GetMapping("/{id}/participants")
    public Result<List<ParticipantArrangementVO>> getParticipants(
            @Parameter(description = "比赛场次ID") @PathVariable Long id) {
        return Result.success(competitionService.getParticipants(id));
    }

    /**
     * 移除场次的个人参赛编排
     *
     * @param id             比赛场次ID
     * @param participationId 参赛记录ID
     * @return 操作结果
     */
    @Operation(summary = "移除运动员编排", description = "将 competitionId/laneNo/bibNumber 置空，已有成绩禁止移除")
    @DeleteMapping("/{id}/participants/{participationId}")
    public Result<Void> removeParticipant(
            @Parameter(description = "比赛场次ID") @PathVariable Long id,
            @Parameter(description = "参赛记录ID") @PathVariable Long participationId) {
        competitionService.removeParticipant(id, participationId);
        return Result.success("已移除编排", null);
    }

    // ==================== 团体编排 ====================

    /**
     * 批量编排团体参赛者到场次
     * <p>
     * 校验场次未结束、项目为集体项目、学院集体项目时间冲突，写入 competitionId。
     * </p>
     *
     * @param id  比赛场次ID
     * @param dto 批量团体编排请求
     * @return 编排后的团体参赛安排 VO 列表
     */
    @Operation(summary = "批量编排团体到场次", description = "含学院集体项目时间冲突校验（规则8）")
    @PostMapping("/{id}/team-participants")
    public Result<List<TeamParticipantArrangementVO>> arrangeTeamParticipants(
            @Parameter(description = "比赛场次ID") @PathVariable Long id,
            @RequestBody @Valid BatchTeamArrangeDTO dto) {
        return Result.success("编排成功", competitionService.arrangeTeamParticipants(id, dto));
    }

    /**
     * 移除场次的团体参赛编排
     *
     * @param id                  比赛场次ID
     * @param teamParticipationId 团体参赛记录ID
     * @return 操作结果
     */
    @Operation(summary = "移除团体编排", description = "将 competitionId 置空，已有成绩禁止移除")
    @DeleteMapping("/{id}/team-participants/{teamParticipationId}")
    public Result<Void> removeTeamParticipant(
            @Parameter(description = "比赛场次ID") @PathVariable Long id,
            @Parameter(description = "团体参赛记录ID") @PathVariable Long teamParticipationId) {
        competitionService.removeTeamParticipant(id, teamParticipationId);
        return Result.success("已移除编排", null);
    }

    // ==================== 裁判分配 ====================

    /**
     * 为比赛场次分配裁判
     * <p>
     * 校验裁判可用、时间不冲突、同场不重复、主裁判唯一。
     * </p>
     *
     * @param id  比赛场次ID
     * @param dto 分配裁判请求数据
     * @return 新创建的裁判分配 VO
     */
    @Operation(summary = "分配裁判", description = "含裁判时间冲突校验与主裁判唯一性校验")
    @PostMapping("/{id}/referee-assignments")
    public Result<RefereeAssignmentVO> assignReferee(
            @Parameter(description = "比赛场次ID") @PathVariable Long id,
            @RequestBody @Valid AssignRefereeDTO dto) {
        return Result.success("分配成功", refereeAssignmentService.assign(id, dto));
    }

    /**
     * 查询某场比赛的全部裁判分配列表
     *
     * @param id 比赛场次ID
     * @return 裁判分配 VO 列表，按角色升序
     */
    @Operation(summary = "获取裁判分配列表", description = "按角色升序返回该场次全部裁判分配")
    @GetMapping("/{id}/referee-assignments")
    public Result<List<RefereeAssignmentVO>> listRefereeAssignments(
            @Parameter(description = "比赛场次ID") @PathVariable Long id) {
        return Result.success(refereeAssignmentService.listByCompetitionId(id));
    }

    /**
     * 移除裁判分配（逻辑删除）
     *
     * @param id           比赛场次ID
     * @param assignmentId 裁判分配记录ID
     * @return 操作结果
     */
    @Operation(summary = "移除裁判分配", description = "逻辑删除，比赛已结束禁止移除，并驱逐秩序册缓存")
    @DeleteMapping("/{id}/referee-assignments/{assignmentId}")
    public Result<Void> removeRefereeAssignment(
            @Parameter(description = "比赛场次ID") @PathVariable Long id,
            @Parameter(description = "裁判分配记录ID") @PathVariable Long assignmentId) {
        refereeAssignmentService.remove(id, assignmentId);
        return Result.success("已移除裁判分配", null);
    }

    // ==================== 器材分配 ====================

    /**
     * 为比赛场次分配器材，同步扣减库存
     *
     * @param id  比赛场次ID
     * @param dto 分配器材请求数据
     * @return 新创建的器材分配 VO
     */
    @Operation(summary = "分配器材", description = "同步扣减库存，校验库存充足与器材可用")
    @PostMapping("/{id}/equipments")
    public Result<CompetitionEquipmentVO> assignEquipment(
            @Parameter(description = "比赛场次ID") @PathVariable Long id,
            @RequestBody @Valid AssignEquipmentDTO dto) {
        return Result.success("分配成功", competitionEquipmentService.assign(id, dto));
    }

    /**
     * 查询某场比赛的所有器材使用记录
     *
     * @param id 比赛场次ID
     * @return 器材使用记录列表
     */
    @Operation(summary = "获取器材使用列表", description = "返回该场次全部器材使用记录")
    @GetMapping("/{id}/equipments")
    public Result<List<CompetitionEquipmentVO>> listEquipments(
            @Parameter(description = "比赛场次ID") @PathVariable Long id) {
        return Result.success(competitionEquipmentService.listByCompetitionId(id));
    }

    /**
     * 更新器材使用状态（流转为使用中或归还），归还时恢复库存
     *
     * @param id       比赛场次ID
     * @param recordId 器材使用记录ID
     * @param dto      状态更新请求数据
     * @return 操作结果
     */
    @Operation(summary = "更新器材状态", description = "已分配→使用中→已归还，归还时恢复库存，已归还不可变更")
    @PutMapping("/{id}/equipments/{recordId}")
    public Result<Void> updateEquipmentStatus(
            @Parameter(description = "比赛场次ID") @PathVariable Long id,
            @Parameter(description = "器材使用记录ID") @PathVariable Long recordId,
            @RequestBody @Valid UpdateEquipmentStatusDTO dto) {
        competitionEquipmentService.updateStatus(id, recordId, dto);
        return Result.success("器材状态已更新", null);
    }
}
