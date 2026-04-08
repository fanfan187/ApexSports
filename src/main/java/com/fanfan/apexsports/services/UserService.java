package com.fanfan.apexsports.services;

import com.fanfan.apexsports.common.result.PageResult;
import com.fanfan.apexsports.dtos.PasswordChangeDTO;
import com.fanfan.apexsports.dtos.UserCreateDTO;
import com.fanfan.apexsports.dtos.UserUpdateDTO;
import com.fanfan.apexsports.vos.UserVO;

/**
 * 用户服务接口
 * <p>
 * 定义用户的分页查询、创建、修改、删除、重置密码和修改密码等业务方法
 * </p>
 *
 * @author fanfan187
 * @version v1.0.0
 * @since v1.0.0
 */
public interface UserService {

    /**
     * 分页查询用户列表
     * <p>
     * 支持按角色、状态和关键词（用户名或真实姓名）进行筛选
     * </p>
     *
     * @param current 当前页码，为空或小于1时默认第1页
     * @param size    每页大小，为空或小于1时默认10条
     * @param role    角色筛选条件，为空时不筛选
     * @param status  状态筛选条件，为空时不筛选
     * @param keyword 关键词（模糊匹配用户名或真实姓名），为空时不筛选
     * @return 分页结果
     */
    PageResult<UserVO> page(Integer current, Integer size, Integer role, Integer status, String keyword);

    /**
     * 新增用户
     *
     * @param dto 新增用户请求数据
     * @return 新创建的用户 VO
     */
    UserVO create(UserCreateDTO dto);

    /**
     * 修改用户信息
     *
     * @param id  用户ID
     * @param dto 修改用户请求数据
     * @return 修改后的用户 VO
     */
    UserVO update(Long id, UserUpdateDTO dto);

    /**
     * 删除用户（逻辑删除）
     *
     * @param id 用户ID
     */
    void deleteById(Long id);

    /**
     * 重置密码为默认密码
     *
     * @param id 用户ID
     */
    void resetPassword(Long id);

    /**
     * 修改密码
     *
     * @param id  用户ID
     * @param dto 修改密码请求数据
     */
    void changePassword(Long id, PasswordChangeDTO dto);
}
