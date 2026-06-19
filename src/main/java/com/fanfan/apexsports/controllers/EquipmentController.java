package com.fanfan.apexsports.controllers;

import com.fanfan.apexsports.common.result.Result;
import com.fanfan.apexsports.dtos.EquipmentCreateDTO;
import com.fanfan.apexsports.dtos.EquipmentUpdateDTO;
import com.fanfan.apexsports.services.EquipmentService;
import com.fanfan.apexsports.vos.EquipmentVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 器材管理控制器
 * <p>
 * 提供器材的列表查询、详情查询、创建、修改、删除等接口。
 * 读接口（列表 / 详情）对所有角色开放，写接口（创建 / 修改 / 删除）需要管理员权限（由服务层校验）
 * </p>
 *
 * @author fanfan187
 * @version v1.0.0
 * @since v1.0.0
 */
@RestController
@RequestMapping("/equipments")
@RequiredArgsConstructor
public class EquipmentController {

    private final EquipmentService equipmentService;

    /**
     * 获取器材列表（全量，不分页）
     *
     * @param type   器材类型筛选（可选，字符串类型）
     * @param status 状态筛选（可选）：0-停用 / 1-可用
     * @return 器材列表
     */
    @GetMapping
    public Result<List<EquipmentVO>> list(
            @RequestParam(required = false) String type,
            @RequestParam(required = false) Integer status) {
        return Result.success(equipmentService.list(type, status));
    }

    /**
     * 根据 ID 获取器材详情
     *
     * @param id 器材ID
     * @return 器材信息
     */
    @GetMapping("/{id}")
    public Result<EquipmentVO> getById(@PathVariable Long id) {
        return Result.success("操作成功", equipmentService.getById(id));
    }

    /**
     * 新增器材
     *
     * @param dto 新增器材请求数据
     * @return 新创建的器材信息
     */
    @PostMapping
    public Result<EquipmentVO> create(@RequestBody @Valid EquipmentCreateDTO dto) {
        return Result.success("创建成功", equipmentService.create(dto));
    }

    /**
     * 修改器材信息
     *
     * @param id  器材ID
     * @param dto 修改器材请求数据
     * @return 修改后的器材信息
     */
    @PutMapping("/{id}")
    public Result<EquipmentVO> update(@PathVariable Long id, @RequestBody @Valid EquipmentUpdateDTO dto) {
        return Result.success("修改成功", equipmentService.update(id, dto));
    }

    /**
     * 删除器材
     *
     * @param id 器材ID
     * @return 操作结果
     */
    @DeleteMapping("/{id}")
    public Result<Void> deleteById(@PathVariable Long id) {
        equipmentService.deleteById(id);
        return Result.success("删除成功", null);
    }
}
