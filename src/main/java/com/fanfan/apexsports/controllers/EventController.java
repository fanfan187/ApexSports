package com.fanfan.apexsports.controllers;

import com.fanfan.apexsports.common.result.Result;
import com.fanfan.apexsports.dtos.EventCreateDTO;
import com.fanfan.apexsports.dtos.EventUpdateDTO;
import com.fanfan.apexsports.services.EventService;
import com.fanfan.apexsports.vos.EventVO;
import com.fanfan.apexsports.vos.RecordVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 比赛项目管理控制器
 * <p>
 * 提供比赛项目的列表查询、详情查询、创建、修改、删除以及历史记录查询等接口。
 * 读接口对所有角色开放，写接口需要管理员权限（由服务层校验）
 * </p>
 *
 * @author fanfan187
 * @version v1.0.0
 * @since v1.0.0
 */
@RestController
@RequestMapping("/events")
@RequiredArgsConstructor
public class EventController {

    private final EventService eventService;

    /**
     * 查询比赛项目列表
     * <p>
     * 支持按项目类型、类别、性别、状态可选过滤，按 ID 升序返回
     * </p>
     *
     * @param type     项目类型（1-个人，2-集体），可选
     * @param category 项目类别（1-径赛，2-田赛），可选
     * @param gender   性别限制（0-女子，1-男子，2-混合），可选
     * @param status   状态（0-停用，1-启用），可选
     * @return 比赛项目列表
     */
    @GetMapping
    public Result<List<EventVO>> list(
            @RequestParam(required = false) Integer type,
            @RequestParam(required = false) Integer category,
            @RequestParam(required = false) Integer gender,
            @RequestParam(required = false) Integer status) {
        return Result.success(eventService.list(type, category, gender, status));
    }

    /**
     * 根据 ID 获取比赛项目详情
     *
     * @param id 项目ID
     * @return 比赛项目信息
     */
    @GetMapping("/{id}")
    public Result<EventVO> getById(@PathVariable Long id) {
        return Result.success("操作成功", eventService.getById(id));
    }

    /**
     * 新增比赛项目
     *
     * @param dto 新增比赛项目请求数据
     * @return 新创建的比赛项目信息
     */
    @PostMapping
    public Result<EventVO> create(@RequestBody @Valid EventCreateDTO dto) {
        return Result.success("创建成功", eventService.create(dto));
    }

    /**
     * 修改比赛项目信息
     *
     * @param id  项目ID
     * @param dto 修改比赛项目请求数据
     * @return 修改后的比赛项目信息
     */
    @PutMapping("/{id}")
    public Result<EventVO> update(@PathVariable Long id, @RequestBody @Valid EventUpdateDTO dto) {
        return Result.success("修改成功", eventService.update(id, dto));
    }

    /**
     * 删除比赛项目（逻辑删除）
     *
     * @param id 项目ID
     * @return 操作结果
     */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        eventService.deleteById(id);
        return Result.success("删除成功", null);
    }

    /**
     * 查询指定比赛项目下的历史记录
     * <p>
     * 仅返回有效（status=1）的记录，按记录日期倒序排列
     * </p>
     *
     * @param id         项目ID
     * @param recordType 记录类型（1-校记录，2-市记录，3-省记录），可选
     * @return 历史记录列表
     */
    @GetMapping("/{id}/records")
    public Result<List<RecordVO>> getRecords(
            @PathVariable Long id,
            @RequestParam(required = false) Integer recordType) {
        return Result.success(eventService.getRecords(id, recordType));
    }
}
