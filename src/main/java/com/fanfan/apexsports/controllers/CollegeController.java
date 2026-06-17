package com.fanfan.apexsports.controllers;

import com.fanfan.apexsports.common.result.Result;
import com.fanfan.apexsports.dtos.CollegeCreateDTO;
import com.fanfan.apexsports.dtos.CollegeUpdateDTO;
import com.fanfan.apexsports.services.CollegeService;
import com.fanfan.apexsports.vos.CollegeVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 学院管理控制器
 * <p>
 * 提供学院的列表查询、详情查询、创建、修改和删除等接口。
 * 读接口对所有角色开放，写接口需要管理员权限（由服务层校验）
 * </p>
 *
 * @author fanfan187
 * @version v1.0.0
 * @since v1.0.0
 */
@RestController
@RequestMapping("/colleges")
@RequiredArgsConstructor
public class CollegeController {

    private final CollegeService collegeService;

    /**
     * 获取学院列表
     *
     * @return 学院列表
     */
    @GetMapping
    public Result<List<CollegeVO>> list() {
        return Result.success(collegeService.list());
    }

    /**
     * 根据 ID 获取学院详情
     *
     * @param id 学院ID
     * @return 学院信息
     */
    @GetMapping("/{id}")
    public Result<CollegeVO> getById(@PathVariable Long id) {
        return Result.success("操作成功", collegeService.getById(id));
    }

    /**
     * 新增学院
     *
     * @param dto 新增学院请求数据
     * @return 新创建的学院信息
     */
    @PostMapping
    public Result<CollegeVO> create(@RequestBody @Valid CollegeCreateDTO dto) {
        return Result.success("创建成功", collegeService.create(dto));
    }

    /**
     * 修改学院信息
     *
     * @param id  学院ID
     * @param dto 修改学院请求数据
     * @return 修改后的学院信息
     */
    @PutMapping("/{id}")
    public Result<CollegeVO> update(@PathVariable Long id, @RequestBody @Valid CollegeUpdateDTO dto) {
        return Result.success("修改成功", collegeService.update(id, dto));
    }

    /**
     * 删除学院
     *
     * @param id 学院ID
     * @return 操作结果
     */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        collegeService.deleteById(id);
        return Result.success("删除成功", null);
    }
}
