package com.fanfan.apexsports.logic;

import cn.hutool.crypto.digest.BCrypt;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fanfan.apexsports.common.result.PageResult;
import com.fanfan.apexsports.config.UserContext;
import com.fanfan.apexsports.constants.UserConstants;
import com.fanfan.apexsports.daos.UserDAO;
import com.fanfan.apexsports.dtos.PasswordChangeDTO;
import com.fanfan.apexsports.dtos.UserCreateDTO;
import com.fanfan.apexsports.dtos.UserUpdateDTO;
import com.fanfan.apexsports.entities.User;
import com.fanfan.apexsports.exception.BusinessException;
import com.fanfan.apexsports.exception.ErrorCode;
import com.fanfan.apexsports.services.UserService;
import com.fanfan.apexsports.vos.UserVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 用户业务实现
 * <p>
 * 实现用户的分页查询、创建（含用户名唯一性校验）、修改、逻辑删除、密码重置和密码修改等业务逻辑，
 * 所有写操作均需要管理员权限
 * </p>
 *
 * @author fanfan187
 * @version v1.0.0
 * @since v1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserLogic implements UserService {

    private final UserDAO userDAO;

    /**
     * 分页查询用户列表
     * <p>
     * 需要管理员权限，支持按角色、状态和关键词筛选
     * </p>
     *
     * @param current 当前页码
     * @param size    每页大小
     * @param role    角色筛选
     * @param status  状态筛选
     * @param keyword 关键词
     * @return 分页结果
     */
    @Override
    public PageResult<UserVO> page(Integer current, Integer size, Integer role, Integer status, String keyword) {
        checkAdmin();

        int pageNum = (current == null || current < 1) ? 1 : current;
        int pageSize = (size == null || size < 1) ? 10 : Math.min(size, 100);

        Page<User> page = userDAO.page(pageNum, pageSize, role, status, keyword);

        List<UserVO> records = page.getRecords().stream()
                .map(UserVO::from)
                .toList();

        return new PageResult<>(records, page.getTotal(), page.getCurrent(), page.getSize(), page.getPages());
    }

    /**
     * 新增用户
     * <p>
     * 需要管理员权限，校验用户名唯一性，密码默认使用 BCrypt 加密
     * </p>
     *
     * @param dto 新增用户请求数据
     * @return 新创建的用户 VO
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public UserVO create(UserCreateDTO dto) {
        checkAdmin();

        // 检查用户名唯一性
        Long count = userDAO.countByUsername(dto.getUsername());
        if (count > 0) {
            throw new BusinessException(ErrorCode.USERNAME_EXISTS);
        }

        // 构建实体
        User user = new User();
        user.setUsername(dto.getUsername());
        user.setPassword(BCrypt.hashpw(
                StringUtils.hasText(dto.getPassword()) ? dto.getPassword() : UserConstants.DEFAULT_PASSWORD
        ));
        user.setRealName(dto.getRealName());
        user.setRole(dto.getRole() != null ? dto.getRole() : UserConstants.ROLE_STAFF);
        user.setPhone(dto.getPhone());
        user.setEmail(dto.getEmail());
        user.setStatus(UserConstants.STATUS_ENABLED);

        // 通过 DAO 插入（自动填充内部字段，并从数据库重新提取）
        User inserted = userDAO.insert(user);
        log.info("创建用户: userId={}, username={}", inserted.getId(), inserted.getUsername());
        return UserVO.from(inserted);
    }

    /**
     * 修改用户信息
     * <p>
     * 需要管理员权限，不允许禁用自己
     * </p>
     *
     * @param id  用户ID
     * @param dto 修改用户请求数据
     * @return 修改后的用户 VO
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public UserVO update(Long id, UserUpdateDTO dto) {
        checkAdmin();

        User user = getUserOrThrow(id);

        // 不允许禁用自己
        if (dto.getStatus() != null && UserConstants.STATUS_DISABLED == dto.getStatus()
                && id.equals(UserContext.getUserId())) {
            throw new BusinessException(ErrorCode.CANNOT_MODIFY_SELF, "不能禁用自己");
        }

        // 更新非空字段
        if (StringUtils.hasText(dto.getRealName())) {
            user.setRealName(dto.getRealName());
        }
        if (dto.getRole() != null) {
            user.setRole(dto.getRole());
        }
        if (dto.getPhone() != null) {
            user.setPhone(dto.getPhone());
        }
        if (dto.getEmail() != null) {
            user.setEmail(dto.getEmail());
        }
        if (dto.getStatus() != null) {
            user.setStatus(dto.getStatus());
        }

        // 通过 DAO 更新（自动填充修改时间，并从数据库重新提取）
        User updated = userDAO.update(user);
        log.info("更新用户: userId={}", id);
        return UserVO.from(updated);
    }

    /**
     * 删除用户（逻辑删除）
     * <p>
     * 需要管理员权限，不允许删除自己
     * </p>
     *
     * @param id 用户ID
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteById(Long id) {
        checkAdmin();

        if (id.equals(UserContext.getUserId())) {
            throw new BusinessException(ErrorCode.CANNOT_MODIFY_SELF, "不能删除自己");
        }

        getUserOrThrow(id);
        userDAO.deleteById(id);
        log.info("删除用户: userId={}", id);
    }

    /**
     * 重置密码为默认密码
     * <p>
     * 需要管理员权限
     * </p>
     *
     * @param id 用户ID
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void resetPassword(Long id) {
        checkAdmin();

        User user = getUserOrThrow(id);
        user.setPassword(BCrypt.hashpw(UserConstants.DEFAULT_PASSWORD));
        userDAO.update(user);
        log.info("重置用户密码: userId={}", id);
    }

    /**
     * 修改密码
     * <p>
     * 只能修改自己的密码，校验原密码正确性并确保新密码与旧密码不同
     * </p>
     *
     * @param id  用户ID
     * @param dto 修改密码请求数据
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void changePassword(Long id, PasswordChangeDTO dto) {
        // 只能修改自己的密码
        if (!id.equals(UserContext.getUserId())) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "只能修改自己的密码");
        }

        User user = getUserOrThrow(id);

        // 校验旧密码
        if (!BCrypt.checkpw(dto.getOldPassword(), user.getPassword())) {
            throw new BusinessException(ErrorCode.OLD_PASSWORD_WRONG);
        }

        // 新密码不能与旧密码相同
        if (BCrypt.checkpw(dto.getNewPassword(), user.getPassword())) {
            throw new BusinessException(ErrorCode.NEW_PASSWORD_SAME_AS_OLD);
        }

        user.setPassword(BCrypt.hashpw(dto.getNewPassword()));
        userDAO.update(user);
        log.info("用户修改密码: userId={}", id);
    }

    /**
     * 检查当前用户是否为管理员
     * <p>
     * 非管理员抛出 {@link BusinessException}（FORBIDDEN）
     * </p>
     */
    private void checkAdmin() {
        Integer role = UserContext.getRole();
        if (role == null || UserConstants.ROLE_ADMIN != role) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
    }

    /**
     * 获取用户，不存在则抛异常
     *
     * @param id 用户ID
     * @return 用户实体
     */
    private User getUserOrThrow(Long id) {
        User user = userDAO.getById(id);
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
        return user;
    }
}
