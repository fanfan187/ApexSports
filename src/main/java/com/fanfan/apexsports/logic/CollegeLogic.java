package com.fanfan.apexsports.logic;

import com.fanfan.apexsports.config.UserContext;
import com.fanfan.apexsports.constants.UserConstants;
import com.fanfan.apexsports.daos.CollegeDAO;
import com.fanfan.apexsports.dtos.CollegeCreateDTO;
import com.fanfan.apexsports.dtos.CollegeUpdateDTO;
import com.fanfan.apexsports.entities.College;
import com.fanfan.apexsports.exception.BusinessException;
import com.fanfan.apexsports.exception.ErrorCode;
import com.fanfan.apexsports.services.CollegeService;
import com.fanfan.apexsports.vos.CollegeVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 学院业务实现
 * <p>
 * 实现学院的列表查询、详情查询、创建（含代码唯一性校验）、修改（部分更新语义）、删除（含引用完整性校验）等业务逻辑。
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
public class CollegeLogic implements CollegeService {

    private final CollegeDAO collegeDAO;

    /**
     * 查询所有学院列表
     * <p>
     * 所有角色均可查询，无需管理员权限
     * </p>
     *
     * @return 学院列表
     */
    @Override
    public List<CollegeVO> list() {
        return collegeDAO.findAll().stream()
                .map(CollegeVO::from)
                .toList();
    }

    /**
     * 根据 ID 查询学院详情
     * <p>
     * 所有角色均可查询，无需管理员权限
     * </p>
     *
     * @param id 学院ID
     * @return 学院 VO
     */
    @Override
    public CollegeVO getById(Long id) {
        College college = getCollegeOrThrow(id);
        return CollegeVO.from(college);
    }

    /**
     * 新增学院
     * <p>
     * 需要管理员权限，校验学院代码唯一性
     * </p>
     *
     * @param dto 新增学院请求数据
     * @return 新创建的学院 VO
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public CollegeVO create(CollegeCreateDTO dto) {
        checkAdmin();

        // 校验学院代码唯一性
        Long count = collegeDAO.countByCode(dto.getCode());
        if (count > 0) {
            throw new BusinessException(ErrorCode.COLLEGE_CODE_EXISTS);
        }

        // 构建实体
        College college = new College()
                .setName(dto.getName())
                .setCode(dto.getCode())
                .setDescription(dto.getDescription());

        College inserted = collegeDAO.insert(college);
        log.info("创建学院: id={}, code={}", inserted.getId(), inserted.getCode());
        return CollegeVO.from(inserted);
    }

    /**
     * 修改学院信息
     * <p>
     * 需要管理员权限，支持部分更新语义，修改代码时排除自身进行唯一性校验
     * </p>
     *
     * @param id  学院ID
     * @param dto 修改学院请求数据
     * @return 修改后的学院 VO
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public CollegeVO update(Long id, CollegeUpdateDTO dto) {
        checkAdmin();

        College college = getCollegeOrThrow(id);

        // 更新非空字段
        if (dto.getName() != null) {
            college.setName(dto.getName());
        }
        if (dto.getCode() != null) {
            // 校验学院代码唯一性（排除自身）
            Long count = collegeDAO.countByCodeExcludeId(dto.getCode(), id);
            if (count > 0) {
                throw new BusinessException(ErrorCode.COLLEGE_CODE_EXISTS);
            }
            college.setCode(dto.getCode());
        }
        if (dto.getDescription() != null) {
            college.setDescription(dto.getDescription());
        }

        College updated = collegeDAO.update(college);
        log.info("修改学院: id={}", id);
        return CollegeVO.from(updated);
    }

    /**
     * 删除学院（逻辑删除）
     * <p>
     * 需要管理员权限，删除前校验是否存在关联运动员或参赛记录
     * </p>
     *
     * @param id 学院ID
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteById(Long id) {
        checkAdmin();

        getCollegeOrThrow(id);

        // 引用完整性校验：检查关联运动员
        Long athleteCount = collegeDAO.countAthletesByCollegeId(id);
        if (athleteCount > 0) {
            throw new BusinessException(ErrorCode.COLLEGE_IN_USE);
        }

        // 引用完整性校验：检查关联参赛记录
        Long participationCount = collegeDAO.countParticipationsByCollegeId(id);
        if (participationCount > 0) {
            throw new BusinessException(ErrorCode.COLLEGE_IN_USE);
        }

        collegeDAO.deleteById(id);
        log.info("删除学院: id={}", id);
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
     * 获取学院，不存在则抛异常
     *
     * @param id 学院ID
     * @return 学院实体
     */
    private College getCollegeOrThrow(Long id) {
        College college = collegeDAO.getById(id);
        if (college == null) {
            throw new BusinessException(ErrorCode.COLLEGE_NOT_FOUND);
        }
        return college;
    }
}
