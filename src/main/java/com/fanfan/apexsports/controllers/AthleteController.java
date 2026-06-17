package com.fanfan.apexsports.controllers;

import com.fanfan.apexsports.common.result.PageResult;
import com.fanfan.apexsports.common.result.Result;
import com.fanfan.apexsports.dtos.AthleteCreateDTO;
import com.fanfan.apexsports.dtos.AthleteUpdateDTO;
import com.fanfan.apexsports.services.AthleteService;
import com.fanfan.apexsports.vos.AthleteVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 运动员管理控制器
 * <p>
 * 提供运动员的分页查询、详情查询、创建、修改和删除等接口。
 * 读接口对所有角色开放，写接口需要管理员权限（由服务层校验）
 * </p>
 *
 * @author fanfan187
 * @version v1.0.0
 * @since v1.0.0
 */
@RestController
@RequestMapping("/athletes")
@RequiredArgsConstructor
public class AthleteController {

    private final AthleteService athleteService;

    /**
     * 分页查询运动员列表
     *
     * @param current   当前页码（可选，默认1）
     * @param size      每页大小（可选，默认10，最大100）
     * @param collegeId 学院ID筛选（可选）
     * @param name      运动员姓名（可选，模糊匹配）
     * @param studentNo 学号（可选，精确匹配）
     * @param status    状态筛选（可选）
     * @param gender    性别筛选（可选）
     * @return 分页结果
     */
    @GetMapping
    public Result<PageResult<AthleteVO>> page(
            @RequestParam(required = false) Integer current,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) Long collegeId,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String studentNo,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) Integer gender) {
        return Result.success(athleteService.page(current, size, collegeId, name, studentNo, status, gender));
    }

    /**
     * 根据 ID 获取运动员详情
     *
     * @param id 运动员ID
     * @return 运动员信息（含学院名称与代码）
     */
    @GetMapping("/{id}")
    public Result<AthleteVO> getById(@PathVariable Long id) {
        return Result.success("操作成功", athleteService.getById(id));
    }

    /**
     * 新增运动员
     *
     * @param dto 新增运动员请求数据
     * @return 新创建的运动员信息
     */
    @PostMapping
    public Result<AthleteVO> create(@RequestBody @Valid AthleteCreateDTO dto) {
        return Result.success("创建成功", athleteService.create(dto));
    }

    /**
     * 修改运动员信息
     *
     * @param id  运动员ID
     * @param dto 修改运动员请求数据
     * @return 修改后的运动员信息
     */
    @PutMapping("/{id}")
    public Result<AthleteVO> update(@PathVariable Long id, @RequestBody @Valid AthleteUpdateDTO dto) {
        return Result.success("修改成功", athleteService.update(id, dto));
    }

    /**
     * 删除运动员
     *
     * @param id 运动员ID
     * @return 操作结果
     */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        athleteService.deleteById(id);
        return Result.success("删除成功", null);
    }
}
