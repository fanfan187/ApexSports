package com.fanfan.apexsports.logic;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fanfan.apexsports.common.result.PageResult;
import com.fanfan.apexsports.config.UserContext;
import com.fanfan.apexsports.constants.AthleteConstants;
import com.fanfan.apexsports.constants.UserConstants;
import com.fanfan.apexsports.daos.AthleteDAO;
import com.fanfan.apexsports.daos.CollegeDAO;
import com.fanfan.apexsports.dtos.AthleteCreateDTO;
import com.fanfan.apexsports.dtos.AthleteUpdateDTO;
import com.fanfan.apexsports.entities.Athlete;
import com.fanfan.apexsports.entities.College;
import com.fanfan.apexsports.exception.BusinessException;
import com.fanfan.apexsports.exception.ErrorCode;
import com.fanfan.apexsports.services.AthleteService;
import com.fanfan.apexsports.vos.AthleteVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 运动员业务实现
 * <p>
 * 实现运动员的分页查询、详情查询、创建（含学院存在性与学号唯一性校验）、修改（部分更新语义）、删除等业务逻辑。
 * 所有写操作均需要管理员权限，读操作对所有角色开放
 * </p>
 *
 * @author fanfan187
 * @version v1.0.0
 * @since v1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AthleteLogic implements AthleteService {

    private final AthleteDAO athleteDAO;
    private final CollegeDAO collegeDAO;

    /**
     * 分页查询运动员列表
     * <p>
     * 所有角色均可查询，无需管理员权限。批量查询学院名称以构建列表视图
     * </p>
     *
     * @param current   当前页码
     * @param size      每页大小
     * @param collegeId 学院ID筛选（可选）
     * @param name      运动员姓名（可选，模糊匹配）
     * @param studentNo 学号（可选，精确匹配）
     * @param status    状态筛选（可选）
     * @param gender    性别筛选（可选）
     * @return 分页结果
     */
    @Override
    public PageResult<AthleteVO> page(Integer current, Integer size, Long collegeId, String name, String studentNo, Integer status, Integer gender) {
        int pageNum = (current == null || current < 1) ? 1 : current;
        int pageSize = (size == null || size < 1) ? 10 : Math.min(size, 100);

        Page<Athlete> page = athleteDAO.page(pageNum, pageSize, collegeId, name, studentNo, status, gender);

        // 批量构建学院名称映射，避免 N+1 查询
        Map<Long, String> collegeNameMap = collegeDAO.findAll().stream()
                .collect(Collectors.toMap(College::getId, College::getName));

        List<AthleteVO> records = page.getRecords().stream()
                .map(athlete -> AthleteVO.from(athlete, collegeNameMap.get(athlete.getCollegeId())))
                .toList();

        return new PageResult<>(records, page.getTotal(), page.getCurrent(), page.getSize(), page.getPages());
    }

    /**
     * 根据 ID 查询运动员详情
     * <p>
     * 所有角色均可查询，返回包含学院名称与学院代码的详情视图
     * </p>
     *
     * @param id 运动员ID
     * @return 运动员 VO
     */
    @Override
    public AthleteVO getById(Long id) {
        Athlete athlete = getAthleteOrThrow(id);
        College college = collegeDAO.getById(athlete.getCollegeId());
        String collegeName = college != null ? college.getName() : null;
        String collegeCode = college != null ? college.getCode() : null;
        return AthleteVO.from(athlete, collegeName, collegeCode);
    }

    /**
     * 新增运动员
     * <p>
     * 需要管理员权限，校验所属学院存在性和学号唯一性，状态默认为正常
     * </p>
     *
     * @param dto 新增运动员请求数据
     * @return 新创建的运动员 VO
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public AthleteVO create(AthleteCreateDTO dto) {
        checkAdmin();

        // 校验所属学院是否存在
        College college = collegeDAO.getById(dto.getCollegeId());
        if (college == null) {
            throw new BusinessException(ErrorCode.COLLEGE_NOT_FOUND);
        }

        // 校验学号唯一性
        Long count = athleteDAO.countByStudentNo(dto.getStudentNo());
        if (count > 0) {
            throw new BusinessException(ErrorCode.STUDENT_NO_EXISTS);
        }

        // 构建实体，状态默认为正常
        Athlete athlete = new Athlete()
                .setCollegeId(dto.getCollegeId())
                .setName(dto.getName())
                .setStudentNo(dto.getStudentNo())
                .setGender(dto.getGender())
                .setGrade(dto.getGrade())
                .setPhone(dto.getPhone())
                .setStatus(AthleteConstants.STATUS_ACTIVE);

        Athlete inserted = athleteDAO.insert(athlete);
        log.info("创建运动员: id={}, studentNo={}", inserted.getId(), inserted.getStudentNo());
        return AthleteVO.from(inserted, college.getName());
    }

    /**
     * 修改运动员信息
     * <p>
     * 需要管理员权限，支持部分更新语义，修改学号时排除自身进行唯一性校验
     * </p>
     *
     * @param id  运动员ID
     * @param dto 修改运动员请求数据
     * @return 修改后的运动员 VO
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public AthleteVO update(Long id, AthleteUpdateDTO dto) {
        checkAdmin();

        Athlete athlete = getAthleteOrThrow(id);

        // 部分更新：仅更新非空字段
        if (dto.getCollegeId() != null) {
            // 校验新学院是否存在
            College newCollege = collegeDAO.getById(dto.getCollegeId());
            if (newCollege == null) {
                throw new BusinessException(ErrorCode.COLLEGE_NOT_FOUND);
            }
            athlete.setCollegeId(dto.getCollegeId());
        }
        if (StringUtils.hasText(dto.getName())) {
            athlete.setName(dto.getName());
        }
        if (StringUtils.hasText(dto.getStudentNo())) {
            // 校验学号唯一性（排除自身）
            Long count = athleteDAO.countByStudentNoExcludeId(dto.getStudentNo(), id);
            if (count > 0) {
                throw new BusinessException(ErrorCode.STUDENT_NO_EXISTS);
            }
            athlete.setStudentNo(dto.getStudentNo());
        }
        if (dto.getGender() != null) {
            athlete.setGender(dto.getGender());
        }
        if (StringUtils.hasText(dto.getGrade())) {
            athlete.setGrade(dto.getGrade());
        }
        if (StringUtils.hasText(dto.getPhone())) {
            athlete.setPhone(dto.getPhone());
        }
        if (dto.getStatus() != null) {
            athlete.setStatus(dto.getStatus());
        }

        Athlete updated = athleteDAO.update(athlete);
        log.info("修改运动员: id={}", id);

        // 查询学院名称用于响应
        College college = collegeDAO.getById(updated.getCollegeId());
        String collegeName = college != null ? college.getName() : null;
        return AthleteVO.from(updated, collegeName);
    }

    /**
     * 删除运动员（逻辑删除）
     * <p>
     * 需要管理员权限
     * </p>
     *
     * @param id 运动员ID
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteById(Long id) {
        checkAdmin();

        getAthleteOrThrow(id);

        athleteDAO.deleteById(id);
        log.info("删除运动员: id={}", id);
    }

    // ==================== 私有辅助方法 ====================

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
     * 获取运动员，不存在则抛异常
     *
     * @param id 运动员ID
     * @return 运动员实体
     */
    private Athlete getAthleteOrThrow(Long id) {
        Athlete athlete = athleteDAO.getById(id);
        if (athlete == null) {
            throw new BusinessException(ErrorCode.ATHLETE_NOT_FOUND);
        }
        return athlete;
    }
}
