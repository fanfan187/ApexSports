package com.fanfan.apexsports.daos;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fanfan.apexsports.entities.Athlete;
import com.fanfan.apexsports.mappers.AthleteMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 运动员数据调度层
 * <p>
 * 协调 AthleteMapper，负责运动员数据的存储、查询和调度。
 * 作为普通 Spring Bean，不继承 ServiceImpl，内部自动填充创建时间、更新时间等字段。
 * </p>
 *
 * @author fanfan187
 * @version v1.0.0
 * @since v1.0.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AthleteDAO {

    private final AthleteMapper athleteMapper;

    // ==================== 查询操作 ====================

    /**
     * 分页查询运动员列表（支持多条件筛选）
     *
     * @param pageNum    当前页码
     * @param pageSize   每页大小
     * @param collegeId  学院ID筛选（可选）
     * @param name       运动员姓名（可选，模糊匹配）
     * @param studentNo  学号（可选，精确匹配）
     * @param status     状态筛选（可选）
     * @param gender     性别筛选（可选）
     * @return 分页结果
     */
    public Page<Athlete> page(int pageNum, int pageSize, Long collegeId, String name, String studentNo, Integer status, Integer gender) {
        LambdaQueryWrapper<Athlete> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(collegeId != null, Athlete::getCollegeId, collegeId)
                .like(StringUtils.hasText(name), Athlete::getName, name)
                .eq(StringUtils.hasText(studentNo), Athlete::getStudentNo, studentNo)
                .eq(status != null, Athlete::getStatus, status)
                .eq(gender != null, Athlete::getGender, gender)
                .orderByDesc(Athlete::getCreatedAt);
        return athleteMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
    }

    /**
     * 根据 ID 查询运动员
     *
     * @param id 运动员ID
     * @return 运动员实体，不存在返回 null
     */
    public Athlete getById(Long id) {
        return athleteMapper.selectById(id);
    }

    /**
     * 统计指定学号的记录数（用于新增时唯一性校验）
     *
     * @param studentNo 学号
     * @return 记录数
     */
    public Long countByStudentNo(String studentNo) {
        return athleteMapper.selectCount(
                new LambdaQueryWrapper<Athlete>().eq(Athlete::getStudentNo, studentNo)
        );
    }

    /**
     * 统计指定学号的记录数，排除指定 ID（用于更新时唯一性校验）
     *
     * @param studentNo  学号
     * @param excludeId  需排除的运动员ID
     * @return 记录数
     */
    public Long countByStudentNoExcludeId(String studentNo, Long excludeId) {
        return athleteMapper.selectCount(
                new LambdaQueryWrapper<Athlete>()
                        .eq(Athlete::getStudentNo, studentNo)
                        .ne(Athlete::getId, excludeId)
        );
    }

    // ==================== 写入操作 ====================

    /**
     * 插入运动员（自动填充创建时间、更新时间等内部字段）
     *
     * @param athlete 运动员实体（不含主键和审计字段）
     * @return 插入后的运动员实体（含自动生成的主键）
     */
    public Athlete insert(Athlete athlete) {
        athlete.setCreatedAt(LocalDateTime.now());
        athlete.setUpdatedAt(LocalDateTime.now());
        athleteMapper.insert(athlete);
        // 从数据库重新提取，确保字段完整
        return athleteMapper.selectById(athlete.getId());
    }

    /**
     * 更新运动员（自动更新修改时间）
     *
     * @param athlete 运动员实体
     * @return 更新后的运动员实体
     */
    public Athlete update(Athlete athlete) {
        athlete.setUpdatedAt(LocalDateTime.now());
        athleteMapper.updateById(athlete);
        return athleteMapper.selectById(athlete.getId());
    }

    /**
     * 逻辑删除运动员（MyBatis-Plus 自动处理 @TableLogic）
     *
     * @param id 运动员ID
     */
    public void deleteById(Long id) {
        athleteMapper.deleteById(id);
    }
}
