package com.fanfan.apexsports.controllers;

import com.fanfan.apexsports.common.result.Result;
import com.fanfan.apexsports.services.RankingService;
import com.fanfan.apexsports.vos.CollegeRankingVO;
import com.fanfan.apexsports.vos.CollegeScoreDetailVO;
import com.fanfan.apexsports.vos.ScoreRefreshResultVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 排名控制器
 * <p>
 * 提供学院排名查询、学院积分详情查询、积分刷新等接口。
 * 读接口对所有角色开放，积分刷新需要管理员权限（由服务层校验）
 * </p>
 *
 * @author fanfan187
 * @version v1.0.0
 * @since v1.0.0
 */
@RestController
@RequestMapping("/rankings")
@RequiredArgsConstructor
public class RankingController {

    private final RankingService rankingService;

    /**
     * 查询学院排名列表
     * <p>
     * 接口82：GET /rankings/colleges
     * </p>
     *
     * @return 学院排名列表
     */
    @GetMapping("/colleges")
    public Result<List<CollegeRankingVO>> listRankings() {
        return Result.success("查询成功", rankingService.listRankings());
    }

    /**
     * 根据 ID 查询学院积分详情
     * <p>
     * 接口83：GET /rankings/colleges/{collegeId}
     * </p>
     *
     * @param collegeId 学院ID
     * @return 学院积分详情
     */
    @GetMapping("/colleges/{collegeId}")
    public Result<CollegeScoreDetailVO> getCollegeDetail(@PathVariable Long collegeId) {
        return Result.success("查询成功", rankingService.getCollegeDetail(collegeId));
    }

    /**
     * 刷新学院积分汇总
     * <p>
     * 接口84：POST /rankings/refresh（注意：无 /colleges 前缀）
     * </p>
     *
     * @return 刷新结果
     */
    @PostMapping("/refresh")
    public Result<ScoreRefreshResultVO> refreshScores() {
        return Result.success("积分刷新成功", rankingService.refreshScores());
    }
}
