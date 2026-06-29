package com.fanfan.apexsports.controllers;

import com.fanfan.apexsports.common.result.PageResult;
import com.fanfan.apexsports.common.result.Result;
import com.fanfan.apexsports.dtos.ParticipationCreateDTO;
import com.fanfan.apexsports.services.ParticipationService;
import com.fanfan.apexsports.vos.ParticipationVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 个人参赛记录管理控制器
 * <p>
 * 提供个人参赛记录的分页查询、详情查询、报名、取消报名和检录等接口。
 * 读接口对所有角色开放，写接口需要相应权限（由服务层校验）
 * </p>
 *
 * @author fanfan187
 * @version v1.0.0
 * @since v1.0.0
 */
@RestController
@RequestMapping("/participations")
@RequiredArgsConstructor
public class ParticipationController {

    private final ParticipationService participationService;

    /**
     * 分页查询个人参赛记录列表
     * <p>
     * 支持按项目、学院、运动员、状态、比赛场次筛选。
     * collegeId 由服务层转换为对应运动员ID集合后筛选（Participation 表无 collegeId 字段）
     * </p>
     *
     * @param current       当前页码（可选，默认1）
     * @param size          每页大小（可选，默认10，最大100）
     * @param eventId       项目ID筛选（可选）
     * @param collegeId     学院ID筛选（可选）
     * @param athleteId     运动员ID筛选（可选）
     * @param status        状态筛选（可选）
     * @param competitionId 比赛场次ID筛选（可选）
     * @return 分页结果
     */
    @GetMapping
    public Result<PageResult<ParticipationVO>> page(
            @RequestParam(required = false) Integer current,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) Long eventId,
            @RequestParam(required = false) Long collegeId,
            @RequestParam(required = false) Long athleteId,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) Long competitionId) {
        return Result.success(participationService.page(current, size, eventId, collegeId,
                athleteId, status, competitionId));
    }

    /**
     * 根据 ID 获取个人参赛记录详情
     *
     * @param id 参赛记录ID
     * @return 参赛记录信息（含运动员姓名/学号、学院信息、项目名称）
     */
    @GetMapping("/{id}")
    public Result<ParticipationVO> getById(@PathVariable Long id) {
        return Result.success("操作成功", participationService.getById(id));
    }

    /**
     * 新增个人参赛记录（报名）
     *
     * @param dto 新增参赛记录请求数据
     * @return 新创建的参赛记录信息
     */
    @PostMapping
    public Result<ParticipationVO> create(@RequestBody @Valid ParticipationCreateDTO dto) {
        return Result.success("报名成功", participationService.create(dto));
    }

    /**
     * 删除个人参赛记录（取消报名）
     *
     * @param id 参赛记录ID
     * @return 操作结果
     */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        participationService.deleteById(id);
        return Result.success("取消报名成功", null);
    }

    /**
     * 检录（将状态从"已报名"更新为"检录通过"）
     *
     * @param id 参赛记录ID
     * @return 操作结果
     */
    @PutMapping("/{id}/check-in")
    public Result<Void> checkIn(@PathVariable Long id) {
        participationService.checkIn(id);
        return Result.success("检录成功", null);
    }
}
