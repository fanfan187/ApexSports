package com.fanfan.apexsports.controllers;

import com.fanfan.apexsports.common.result.PageResult;
import com.fanfan.apexsports.common.result.Result;
import com.fanfan.apexsports.dtos.PasswordChangeDTO;
import com.fanfan.apexsports.dtos.UserCreateDTO;
import com.fanfan.apexsports.dtos.UserUpdateDTO;
import com.fanfan.apexsports.services.UserService;
import com.fanfan.apexsports.vos.UserVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 用户管理控制器
 * <p>
 * 提供用户的分页查询、创建、修改、删除、重置密码和修改密码等接口
 * </p>
 *
 * @author fanfan187
 * @version v1.0.0
 * @since v1.0.0
 */
@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    /**
     * 获取用户列表（分页）
     * <p>
     * 支持按角色、状态和关键词筛选
     * </p>
     *
     * @param current 当前页码
     * @param size    每页大小
     * @param role    角色筛选
     * @param status  状态筛选
     * @param keyword 关键词
     * @return 分页用户列表
     */
    @GetMapping
    public Result<PageResult<UserVO>> page(
            @RequestParam(required = false) Integer current,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) Integer role,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) String keyword) {
        return Result.success(userService.page(current, size, role, status, keyword));
    }

    /**
     * 新增用户
     *
     * @param dto 新增用户请求数据
     * @return 新创建的用户信息
     */
    @PostMapping
    public Result<UserVO> create(@RequestBody @Valid UserCreateDTO dto) {
        return Result.success("创建成功", userService.create(dto));
    }

    /**
     * 修改用户信息
     *
     * @param id  用户ID
     * @param dto 修改用户请求数据
     * @return 修改后的用户信息
     */
    @PutMapping("/{id}")
    public Result<UserVO> update(@PathVariable Long id, @RequestBody UserUpdateDTO dto) {
        return Result.success("修改成功", userService.update(id, dto));
    }

    /**
     * 删除用户
     *
     * @param id 用户ID
     * @return 操作结果
     */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        userService.deleteById(id);
        return Result.success("删除成功", null);
    }

    /**
     * 重置密码
     * <p>
     * 将用户密码重置为默认密码
     * </p>
     *
     * @param id 用户ID
     * @return 操作结果
     */
    @PutMapping("/{id}/reset-password")
    public Result<Void> resetPassword(@PathVariable Long id) {
        userService.resetPassword(id);
        return Result.success("密码已重置为默认密码", null);
    }

    /**
     * 修改密码
     *
     * @param id  用户ID
     * @param dto 修改密码请求数据（原密码 + 新密码）
     * @return 操作结果
     */
    @PutMapping("/{id}/password")
    public Result<Void> changePassword(@PathVariable Long id, @RequestBody @Valid PasswordChangeDTO dto) {
        userService.changePassword(id, dto);
        return Result.success("密码修改成功", null);
    }
}
