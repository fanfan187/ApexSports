package com.fanfan.apexsports.controllers;

import com.fanfan.apexsports.common.result.Result;
import com.fanfan.apexsports.dtos.RecordCreateDTO;
import com.fanfan.apexsports.dtos.RecordUpdateDTO;
import com.fanfan.apexsports.services.RecordService;
import com.fanfan.apexsports.vos.RecordDetailVO;
import com.fanfan.apexsports.vos.RecordListVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 记录管理控制器
 * <p>
 * 提供校记录 / 市记录 / 省记录的列表查询、详情查询、创建、修改等接口。
 * 读接口（列表 / 详情）对所有角色开放，写接口（创建 / 修改）需要管理员权限（由服务层校验）；
 * 无删除接口 —— 作废记录使用 PUT status=0。
 * </p>
 *
 * @author fanfan187
 * @version v1.0.0
 * @since v1.0.0
 */
@RestController
@RequestMapping("/records")
@RequiredArgsConstructor
public class RecordController {

    private final RecordService recordService;

    /**
     * 获取记录列表（全量，不分页）
     *
     * @param eventId    项目ID筛选（可选）
     * @param recordType 记录类型筛选（可选）：1-校记录 / 2-市记录 / 3-省记录
     * @param status     状态筛选（可选）：0-已作废 / 1-有效
     * @return 记录列表
     */
    @GetMapping
    public Result<List<RecordListVO>> list(
            @RequestParam(required = false) Long eventId,
            @RequestParam(required = false) Integer recordType,
            @RequestParam(required = false) Integer status) {
        return Result.success("查询成功", recordService.list(eventId, recordType, status));
    }

    /**
     * 根据 ID 获取记录详情
     *
     * @param id 记录ID
     * @return 记录详情信息
     */
    @GetMapping("/{id}")
    public Result<RecordDetailVO> getById(@PathVariable Long id) {
        return Result.success("查询成功", recordService.getById(id));
    }

    /**
     * 新增记录
     *
     * @param dto 新增记录请求数据
     * @return 新创建的记录详情信息
     */
    @PostMapping
    public Result<RecordDetailVO> create(@RequestBody @Valid RecordCreateDTO dto) {
        return Result.success("新增成功", recordService.create(dto));
    }

    /**
     * 修改记录信息
     *
     * @param id  记录ID
     * @param dto 修改记录请求数据（部分更新，null 表示不修改）
     * @return 修改后的记录详情信息
     */
    @PutMapping("/{id}")
    public Result<RecordDetailVO> update(@PathVariable Long id, @RequestBody @Valid RecordUpdateDTO dto) {
        return Result.success("修改成功", recordService.update(id, dto));
    }
}
