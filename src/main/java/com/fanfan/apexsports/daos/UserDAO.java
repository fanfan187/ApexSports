package com.fanfan.apexsports.daos;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fanfan.apexsports.cache.AuthCache;
import com.fanfan.apexsports.entities.User;
import com.fanfan.apexsports.mappers.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 用户数据调度层
 * <p>
 * 协调 UserMapper 和缓存服务，负责用户数据的存储、查询和调度。
 * 作为普通 Spring Bean，不继承 ServiceImpl，内部自动填充主键、创建时间等字段
 * </p>
 *
 * @author fanfan187
 * @version v1.0.0
 * @since v1.0.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class UserDAO {

    private final UserMapper userMapper;
    private final AuthCache authCache;

    // ==================== 查询操作 ====================

    /**
     * 根据用户名查询用户
     *
     * @param username 用户名
     * @return 用户实体，不存在返回 null
     */
    public User getByUsername(String username) {
        return userMapper.selectOne(
                new LambdaQueryWrapper<User>().eq(User::getUsername, username)
        );
    }

    /**
     * 根据 ID 查询用户
     *
     * @param id 用户ID
     * @return 用户实体，不存在返回 null
     */
    public User getById(Long id) {
        return userMapper.selectById(id);
    }

    /**
     * 统计指定用户名的记录数
     *
     * @param username 用户名
     * @return 记录数
     */
    public Long countByUsername(String username) {
        return userMapper.selectCount(
                new LambdaQueryWrapper<User>().eq(User::getUsername, username)
        );
    }

    /**
     * 分页查询用户列表
     *
     * @param pageNum  当前页码
     * @param pageSize 每页大小
     * @param role     角色筛选（可选）
     * @param status   状态筛选（可选）
     * @param keyword  关键词（可选，匹配用户名或真实姓名）
     * @return 分页结果
     */
    public Page<User> page(int pageNum, int pageSize, Integer role, Integer status, String keyword) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(role != null, User::getRole, role)
                .eq(status != null, User::getStatus, status)
                .and(StringUtils.hasText(keyword), w -> w
                        .like(User::getUsername, keyword)
                        .or()
                        .like(User::getRealName, keyword)
                )
                .orderByDesc(User::getCreatedAt);

        return userMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
    }

    // ==================== 写入操作 ====================

    /**
     * 插入用户（自动填充创建时间等内部字段）
     *
     * @param user 用户实体（不含主键和审计字段）
     * @return 插入后的用户实体（含自动生成的主键）
     */
    public User insert(User user) {
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        userMapper.insert(user);
        // 从数据库重新提取，确保字段完整
        return userMapper.selectById(user.getId());
    }

    /**
     * 更新用户（自动更新修改时间）
     *
     * @param user 用户实体
     * @return 更新后的用户实体
     */
    public User update(User user) {
        user.setUpdatedAt(LocalDateTime.now());
        userMapper.updateById(user);
        return userMapper.selectById(user.getId());
    }

    /**
     * 逻辑删除用户
     *
     * @param id 用户ID
     */
    public void deleteById(Long id) {
        userMapper.deleteById(id);
    }

    // ==================== 认证相关 ====================

    /**
     * 将 Token 加入黑名单
     *
     * @param token     原始 JWT Token
     * @param remaining Token 剩余有效时间
     */
    public void blacklistToken(String token, Duration remaining) {
        if (!remaining.isZero()) {
            authCache.addToBlacklist(token, remaining);
        }
    }

    /**
     * 检查 Token 是否已被拉黑
     *
     * @param token 原始 JWT Token
     * @return true 表示已被拉黑
     */
    public boolean isTokenBlacklisted(String token) {
        return authCache.isTokenBlacklisted(token);
    }
}
