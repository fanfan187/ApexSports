package com.fanfan.apexsports.controllers;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.fanfan.apexsports.common.result.Result;
import com.fanfan.apexsports.dtos.BatchScoreDTO;
import com.fanfan.apexsports.dtos.BatchTeamScoreDTO;
import com.fanfan.apexsports.dtos.ScoreQueryDTO;
import com.fanfan.apexsports.dtos.ScoreUpdateDTO;
import com.fanfan.apexsports.dtos.SingleScoreDTO;
import com.fanfan.apexsports.services.ScoreService;
import com.fanfan.apexsports.vos.CompetitionScoreVO;
import com.fanfan.apexsports.vos.ScoreDetailVO;
import com.fanfan.apexsports.vos.ScoreEntryResultVO;
import com.fanfan.apexsports.vos.ScoreVO;
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

/**
 * 成绩管理控制器
 * <p>
 * 承担成绩模块对外的 REST 入口，本类仅负责参数校验与响应封装，
 * 业务逻辑（破纪录判定、积分联动、学院汇总）全部委托 {@link ScoreService} 实现。
 * </p>
 * <p>
 * <b>路由顺序说明：</b>静态路径必须先于动态通配 {@code /{id}} 声明，避免被捕获：
 * <ul>
 *   <li>{@code POST /batch}、{@code POST /team-batch}：批量录入静态路径</li>
 *   <li>{@code GET /competition/{competitionId}}：场次汇总静态前缀，必须在 {@code /{id}} 之前</li>
 *   <li>{@code /{id}}：单条修改/删除（PUT/DELETE）</li>
 * </ul>
 * 根 {@code GET /scores}（分页）与 {@code POST /scores}（单条录入）不带路径变量，不受影响。
 * </p>
 * <p>
 * <b>路径前缀说明：</b>后端 {@code application.yml} 已配置
 * {@code server.servlet.context-path: /api}，所有控制器映射自动带 /api 前缀，
 * 故本类 {@code @RequestMapping} 写 {@code /scores}，前端 baseURL {@code /api} + {@code /scores}
 * 命中 {@code /api/scores}，与 {@link CompetitionController} 的 {@code /competitions} 模式一致。
 * </p>
 *
 * @author fanfan187
 * @version v1.0.0
 * @since v1.0.0
 */
@RestController
@RequestMapping("/scores")
@RequiredArgsConstructor
@Tag(name = "成绩管理", description = "个人/团体成绩录入（含破纪录判定与积分联动）")
public class ScoreController {

    private final ScoreService scoreService;

    // ==================== 成绩录入 ====================

    /**
     * 批量录入个人成绩
     * <p>
     * 静态路径，必须声明在 {@code /{id}} 之前。请求体携带比赛场次ID与成绩条目列表，
     * 由 Service 层逐条处理并联动触发破纪录判定与学院积分汇总。
     * </p>
     *
     * @param dto 批量录入请求（含 competitionId 与成绩条目）
     * @return 录入结果（是否破纪录、成功条数）
     */
    @Operation(summary = "批量录入个人成绩", description = "逐条校验唯一性，联动破纪录判定与学院积分汇总")
    @PostMapping("/batch")
    public Result<ScoreEntryResultVO> batchAddScores(@RequestBody @Valid BatchScoreDTO dto) {
        return Result.success(scoreService.batchAddScores(dto));
    }

    /**
     * 录入单条个人成绩
     * <p>
     * SingleScoreDTO 不含 competitionId 字段（Todo 6 决策），competitionId 由前端以
     * query param 形式传递（{@code POST /scores?competitionId=X}），Controller 接收后注入 Service 调用。
     * 语义等价于批量录入的单条版本，支持 points 覆盖自动计算。
     * </p>
     *
     * @param dto           单条成绩录入请求
     * @param competitionId 比赛场次ID（query param，可选；为空时由 Service 层抛业务异常）
     * @return 录入结果（是否破纪录、成功条数恒为 1）
     */
    @Operation(summary = "录入单条个人成绩", description = "competitionId 由 query param 注入，支持 points 覆盖")
    @PostMapping
    public Result<ScoreEntryResultVO> addScore(
            @RequestBody @Valid SingleScoreDTO dto,
            @RequestParam(required = false) Long competitionId) {
        return Result.success(scoreService.addScore(dto, competitionId));
    }

    /**
     * 批量录入团体成绩
     * <p>
     * 静态路径，必须声明在 {@code /{id}} 之前。逻辑与个人批量录入对应，差异在于
     * 团体参赛记录唯一性校验、破纪录按学院维度、积分联动 isTeam=true。
     * </p>
     *
     * @param dto 批量团体成绩录入请求
     * @return 录入结果（是否破纪录、成功条数）
     */
    @Operation(summary = "批量录入团体成绩", description = "团体维度破纪录判定，积分联动 isTeam=true")
    @PostMapping("/team-batch")
    public Result<ScoreEntryResultVO> batchAddTeamScores(@RequestBody @Valid BatchTeamScoreDTO dto) {
        return Result.success(scoreService.batchAddTeamScores(dto));
    }

    // ==================== 成绩查询/维护（Todo 11） ====================

    /**
     * 分页查询成绩列表
     * <p>
     * 静态根路径（无路径变量），不被 {@code /{id}} 通配捕获。
     * 支持按比赛场次/项目可选过滤，结果为连表 {@link ScoreDetailVO}（含 athleteName/collegeName
     * /eventName/competitionName）。分页参数缺省由 DAO 补齐 1/10。
     * </p>
     *
     * @param query 分页查询条件（competitionId/eventId/page/size 均可选）
     * @return 分页结果
     */
    @Operation(summary = "分页查询成绩列表", description = "5 表 LEFT JOIN 分页，支持按场次/项目过滤")
    @GetMapping
    public Result<IPage<ScoreDetailVO>> getScorePage(@ModelAttribute ScoreQueryDTO query) {
        return Result.success(scoreService.getScorePage(query));
    }

    /**
     * 获取比赛场次成绩汇总
     * <p>
     * <b>静态路径</b>，必须声明在 {@code /{id}} 通配之前，避免被 {@code GET /scores/{id}} 误捕获
     * （competitionId 字面值会被当作 scoreId 解析）。
     * </p>
     * <p>
     * 返回该场次下排名列表（按 rank 升序）与项目/场次名称聚合、破纪录总数统计。
     * </p>
     *
     * @param competitionId 比赛场次ID
     * @return 场次成绩汇总 VO
     */
    @Operation(summary = "获取比赛场次成绩汇总", description = "返回按 rank 升序的排名列表与项目/场次聚合信息")
    @GetMapping("/competition/{competitionId}")
    public Result<CompetitionScoreVO> getCompetitionScores(
            @Parameter(description = "比赛场次ID") @PathVariable Long competitionId) {
        return Result.success(scoreService.getCompetitionScores(competitionId));
    }

    /**
     * 修改成绩
     * <p>
     * 部分更新语义，未提供的字段（null）不修改。修改后由 Service 层触发积分回滚+重算与破纪录重判。
     * 终态（已结束/已取消）的场次下的成绩不可修改。
     * </p>
     *
     * @param id  成绩ID
     * @param dto 修改请求（id 取自路径或请求体均可，由 Service 校验一致性）
     * @return 修改后的成绩 VO
     */
    @Operation(summary = "修改成绩", description = "部分更新，含积分回滚+重算与破纪录重判")
    @PutMapping("/{id}")
    public Result<ScoreVO> updateScore(
            @Parameter(description = "成绩ID") @PathVariable Long id,
            @RequestBody @Valid ScoreUpdateDTO dto) {
        // 路径 id 与请求体 id 一致性兜底：以路径为准，避免请求体 id 与 URL 不一致
        if (dto.getId() == null) {
            dto.setId(id);
        }
        return Result.success("修改成功", scoreService.updateScore(dto));
    }

    /**
     * 删除成绩（逻辑删除）
     * <p>
     * 删除后由 Service 层触发积分回滚与破纪录联动（作废对应 Record）。
     * 终态（已结束/已取消）的场次下的成绩不可删除。
     * </p>
     *
     * @param id 成绩ID
     * @return 操作结果
     */
    @Operation(summary = "删除成绩", description = "逻辑删除，含积分回滚与破纪录联动")
    @DeleteMapping("/{id}")
    public Result<Void> deleteScoreById(
            @Parameter(description = "成绩ID") @PathVariable Long id) {
        scoreService.deleteScoreById(id);
        return Result.success("删除成功", null);
    }
}
