package com.fanfan.apexsports.controllers;

import com.fanfan.apexsports.common.result.PageResult;
import com.fanfan.apexsports.common.result.Result;
import com.fanfan.apexsports.dtos.RefereeCreateDTO;
import com.fanfan.apexsports.dtos.RefereeUpdateDTO;
import com.fanfan.apexsports.services.RefereeService;
import com.fanfan.apexsports.vos.RefereeScheduleVO;
import com.fanfan.apexsports.vos.RefereeVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 裁判管理控制器
 * <p>
 * 提供裁判的分页查询、详情查询、创建、修改、删除和赛程查询等接口。
 * 读接口（列表 / 详情 / 赛程）对所有角色开放，写接口（创建 / 修改 / 删除）需要管理员权限（由服务层校验）
 * </p>
 *
 * @author fanfan187
 * @version v1.0.0
 * @since v1.0.0
 */
@RestController
@RequestMapping("/referees")
@RequiredArgsConstructor
public class RefereeController {

    private final RefereeService refereeService;

    /**
     * 分页查询裁判列表
     *
     * @param current 当前页码（可选，默认1）
     * @param size    每页大小（可选，默认10，最大100）
     * @param level   裁判等级筛选（可选）
     * @param status  状态筛选（可选）
     * @param keyword 姓名/证件号等关键字（可选，模糊匹配）
     * @return 分页结果
     */
    @GetMapping
    public Result<PageResult<RefereeVO>> page(
            @RequestParam(required = false) Integer current,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) String level,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) String keyword) {
        PageResult<RefereeVO> result = refereeService.page(current, size, level, status, keyword);
        return Result.success("操作成功", result);
    }

    /**
     * 根据 ID 获取裁判详情
     *
     * @param id 裁判ID
     * @return 裁判信息
     */
    @GetMapping("/{id}")
    public Result<RefereeVO> getById(@PathVariable Long id) {
        RefereeVO vo = refereeService.getById(id);
        return Result.success("操作成功", vo);
    }

    /**
     * 新增裁判
     *
     * @param dto 新增裁判请求数据
     * @return 新创建的裁判信息
     */
    @PostMapping
    public Result<RefereeVO> create(@RequestBody @Valid RefereeCreateDTO dto) {
        RefereeVO vo = refereeService.create(dto);
        return Result.success("创建成功", vo);
    }

    /**
     * 修改裁判信息
     *
     * @param id  裁判ID
     * @param dto 修改裁判请求数据
     * @return 修改后的裁判信息
     */
    @PutMapping("/{id}")
    public Result<RefereeVO> update(@PathVariable Long id, @RequestBody @Valid RefereeUpdateDTO dto) {
        RefereeVO vo = refereeService.update(id, dto);
        return Result.success("修改成功", vo);
    }

    /**
     * 删除裁判
     *
     * @param id 裁判ID
     * @return 操作结果
     */
    @DeleteMapping("/{id}")
    public Result<Void> deleteById(@PathVariable Long id) {
        refereeService.deleteById(id);
        return Result.success("删除成功", null);
    }

    /**
     * 查看裁判赛程安排
     *
     * @param id 裁判ID
     * @return 赛程列表
     */
    @GetMapping("/{id}/schedule")
    public Result<List<RefereeScheduleVO>> getSchedule(@PathVariable Long id) {
        List<RefereeScheduleVO> list = refereeService.getSchedule(id);
        return Result.success("操作成功", list);
    }
}
