package com.fanfan.apexsports.controllers;

import com.fanfan.apexsports.common.result.Result;
import com.fanfan.apexsports.dtos.VenueCreateDTO;
import com.fanfan.apexsports.dtos.VenueUpdateDTO;
import com.fanfan.apexsports.services.VenueService;
import com.fanfan.apexsports.vos.VenueScheduleVO;
import com.fanfan.apexsports.vos.VenueVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/**
 * 场地管理控制器
 * <p>
 * 提供场地的列表查询、详情查询、创建、修改、删除和赛程占用查询等接口。
 * 读接口（列表 / 详情 / 赛程）对所有角色开放，写接口（创建 / 修改 / 删除）需要管理员权限（由服务层校验）
 * </p>
 *
 * @author fanfan187
 * @version v1.0.0
 * @since v1.0.0
 */
@RestController
@RequestMapping("/venues")
@RequiredArgsConstructor
public class VenueController {

    private final VenueService venueService;

    /**
     * 获取场地列表（全量，不分页）
     *
     * @param type   场地类型筛选（可选）：1-径赛 / 2-田赛 / 3-综合
     * @param status 状态筛选（可选）：0-不可用 / 1-可用
     * @return 场地列表
     */
    @GetMapping
    public Result<List<VenueVO>> list(
            @RequestParam(required = false) Integer type,
            @RequestParam(required = false) Integer status) {
        return Result.success(venueService.list(type, status));
    }

    /**
     * 根据 ID 获取场地详情
     *
     * @param id 场地ID
     * @return 场地信息
     */
    @GetMapping("/{id}")
    public Result<VenueVO> getById(@PathVariable Long id) {
        return Result.success("操作成功", venueService.getById(id));
    }

    /**
     * 新增场地
     *
     * @param dto 新增场地请求数据
     * @return 新创建的场地信息
     */
    @PostMapping
    public Result<VenueVO> create(@RequestBody @Valid VenueCreateDTO dto) {
        return Result.success("创建成功", venueService.create(dto));
    }

    /**
     * 修改场地信息
     *
     * @param id  场地ID
     * @param dto 修改场地请求数据
     * @return 修改后的场地信息
     */
    @PutMapping("/{id}")
    public Result<VenueVO> update(@PathVariable Long id, @RequestBody @Valid VenueUpdateDTO dto) {
        return Result.success("修改成功", venueService.update(id, dto));
    }

    /**
     * 删除场地
     *
     * @param id 场地ID
     * @return 操作结果
     */
    @DeleteMapping("/{id}")
    public Result<Void> deleteById(@PathVariable Long id) {
        venueService.deleteById(id);
        return Result.success("删除成功", null);
    }

    /**
     * 查看场地赛程占用情况
     *
     * @param id   场地ID
     * @param date 筛选日期（可选，格式 YYYY-MM-DD）
     * @return 赛程列表
     */
    @GetMapping("/{id}/schedule")
    public Result<List<VenueScheduleVO>> getSchedule(
            @PathVariable Long id,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return Result.success("操作成功", venueService.getSchedule(id, date));
    }
}
