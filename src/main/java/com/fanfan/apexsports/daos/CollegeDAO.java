package com.fanfan.apexsports.daos;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fanfan.apexsports.entities.Athlete;
import com.fanfan.apexsports.entities.College;
import com.fanfan.apexsports.entities.TeamParticipation;
import com.fanfan.apexsports.mappers.AthleteMapper;
import com.fanfan.apexsports.mappers.CollegeMapper;
import com.fanfan.apexsports.mappers.TeamParticipationMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 学院数据调度层
 * <p>
 * 协调 CollegeMapper，负责学院数据的存储、查询和调度。
 * 作为普通 Spring Bean，不继承 ServiceImpl，内部自动填充创建时间、更新时间等字段。
 * 引用完整性检查通过 AthleteMapper / TeamParticipationMapper 进行计数查询。
 * </p>
 *
 * @author fanfan187
 * @version v1.0.0
 * @since v1.0.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CollegeDAO {

    private final CollegeMapper collegeMapper;
    private final AthleteMapper athleteMapper;
    private final TeamParticipationMapper teamParticipationMapper;

    // ==================== 查询操作 ====================

    /**
     * 查询所有未删除的学院（按 ID 升序）
     *
     * @return 学院列表
     */
    public List<College> findAll() {
        return collegeMapper.selectList(
                new LambdaQueryWrapper<College>().orderByAsc(College::getId)
        );
    }

    /**
     * 根据 ID 查询学院
     *
     * @param id 学院ID
     * @return 学院实体，不存在返回 null
     */
    public College getById(Long id) {
        return collegeMapper.selectById(id);
    }

    /**
     * 统计指定学院代码的记录数（用于新增时唯一性校验）
     *
     * @param code 学院代码
     * @return 记录数
     */
    public Long countByCode(String code) {
        return collegeMapper.selectCount(
                new LambdaQueryWrapper<College>().eq(College::getCode, code)
        );
    }

    /**
     * 统计指定学院代码的记录数，排除指定 ID（用于更新时唯一性校验）
     *
     * @param code      学院代码
     * @param excludeId 需排除的学院ID
     * @return 记录数
     */
    public Long countByCodeExcludeId(String code, Long excludeId) {
        return collegeMapper.selectCount(
                new LambdaQueryWrapper<College>()
                        .eq(College::getCode, code)
                        .ne(College::getId, excludeId)
        );
    }

    // ==================== 引用完整性检查 ====================

    /**
     * 检查指定学院下是否存在未删除的运动员
     *
     * @param collegeId 学院ID
     * @return 关联运动员数量
     */
    public Long countAthletesByCollegeId(Long collegeId) {
        return athleteMapper.selectCount(
                new LambdaQueryWrapper<Athlete>().eq(Athlete::getCollegeId, collegeId)
        );
    }

    /**
     * 检查指定学院下是否存在未删除的参赛记录
     *
     * @param collegeId 学院ID
     * @return 关联参赛记录数量
     */
    public Long countParticipationsByCollegeId(Long collegeId) {
        return teamParticipationMapper.selectCount(
                new LambdaQueryWrapper<TeamParticipation>().eq(TeamParticipation::getCollegeId, collegeId)
        );
    }

    // ==================== 写入操作 ====================

    /**
     * 插入学院（自动填充创建时间、更新时间等内部字段）
     *
     * @param college 学院实体（不含主键和审计字段）
     * @return 插入后的学院实体（含自动生成的主键）
     */
    public College insert(College college) {
        college.setCreatedAt(LocalDateTime.now());
        college.setUpdatedAt(LocalDateTime.now());
        collegeMapper.insert(college);
        // 从数据库重新提取，确保字段完整
        return collegeMapper.selectById(college.getId());
    }

    /**
     * 更新学院（自动更新修改时间）
     *
     * @param college 学院实体
     * @return 更新后的学院实体
     */
    public College update(College college) {
        college.setUpdatedAt(LocalDateTime.now());
        collegeMapper.updateById(college);
        return collegeMapper.selectById(college.getId());
    }

    /**
     * 逻辑删除学院（MyBatis-Plus 自动处理 @TableLogic）
     *
     * @param id 学院ID
     */
    public void deleteById(Long id) {
        collegeMapper.deleteById(id);
    }
}
