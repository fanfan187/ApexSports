package com.fanfan.apexsports.vos;

import com.fanfan.apexsports.entities.User;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 用户响应 VO（不含密码）
 * <p>
 * 返回给前端的用户视图对象，自动排除密码字段，提供从 Entity 转换的静态工厂方法
 * </p>
 *
 * @author fanfan187
 * @version v1.0.0
 * @since v1.0.0
 */
@Data
public class UserVO implements Serializable {

    private Long id;
    private String username;
    private String realName;
    private Integer role;
    private String phone;
    private String email;
    private Integer status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /**
     * 从 Entity 转换为 VO
     *
     * @param user 用户实体
     * @return 用户 VO，如果实体为 null 则返回 null
     */
    public static UserVO from(User user) {
        if (user == null) {
            return null;
        }
        UserVO vo = new UserVO();
        vo.setId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setRealName(user.getRealName());
        vo.setRole(user.getRole());
        vo.setPhone(user.getPhone());
        vo.setEmail(user.getEmail());
        vo.setStatus(user.getStatus());
        vo.setCreatedAt(user.getCreatedAt());
        vo.setUpdatedAt(user.getUpdatedAt());
        return vo;
    }
}
