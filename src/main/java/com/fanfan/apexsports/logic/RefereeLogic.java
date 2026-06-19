package com.fanfan.apexsports.logic;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fanfan.apexsports.common.result.PageResult;
import com.fanfan.apexsports.config.UserContext;
import com.fanfan.apexsports.constants.RefereeConstants;
import com.fanfan.apexsports.constants.UserConstants;
import com.fanfan.apexsports.daos.RefereeDAO;
import com.fanfan.apexsports.dtos.RefereeCreateDTO;
import com.fanfan.apexsports.dtos.RefereeUpdateDTO;
import com.fanfan.apexsports.entities.Referee;
import com.fanfan.apexsports.exception.BusinessException;
import com.fanfan.apexsports.exception.ErrorCode;
import com.fanfan.apexsports.mappers.RefereeMapper;
import com.fanfan.apexsports.services.RefereeService;
import com.fanfan.apexsports.vos.RefereeScheduleVO;
import com.fanfan.apexsports.vos.RefereeVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 裁判业务实现
 * <p>
 * 实现裁判的分页查询、详情查询、创建（状态默认可用）、修改（部分更新语义）、
 * 删除（含未完成分配校验）和赛程查询等业务逻辑。
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
public class RefereeLogic implements RefereeService {

    private final RefereeDAO refereeDAO;
    private final RefereeMapper refereeMapper;

    /**
     * 分页查询裁判列表
     * <p>
     * 所有角色均可查询，无需管理员权限。支持按裁判等级、状态和姓名关键字筛选
     * </p>
     *
     * @param current 当前页码
     * @param size    每页大小
     * @param level   裁判等级筛选（可选，精确匹配）
     * @param status  状态筛选（可选）：0-休息 / 1-可用
     * @param keyword 姓名关键字（可选，模糊匹配）
     * @return 分页结果
     */
    @Override
    public PageResult<RefereeVO> page(Integer current, Integer size, String level, Integer status, String keyword) {
        int pageNum = (current == null || current < 1) ? 1 : current;
        int pageSize = (size == null || size < 1) ? 10 : Math.min(size, 100);

        Page<Referee> page = refereeDAO.page(pageNum, pageSize, level, status, keyword);

        List<RefereeVO> records = page.getRecords().stream()
                .map(RefereeVO::from)
                .collect(Collectors.toList());

        return new PageResult<>(records, page.getTotal(), page.getCurrent(), page.getSize(), page.getPages());
    }

    /**
     * 根据 ID 查询裁判详情
     * <p>
     * 所有角色均可查询
     * </p>
     *
     * @param id 裁判ID
     * @return 裁判 VO
     */
    @Override
    public RefereeVO getById(Long id) {
        Referee referee = getRefereeOrThrow(id);
        return RefereeVO.from(referee);
    }

    /**
     * 新增裁判
     * <p>
     * 需要管理员权限，状态默认设为可用（1），不由前端传入
     * </p>
     *
     * @param dto 新增裁判请求数据
     * @return 新创建的裁判 VO
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public RefereeVO create(RefereeCreateDTO dto) {
        checkAdmin();

        // 构建实体，状态默认为可用
        Referee referee = new Referee()
                .setName(dto.getName())
                .setGender(dto.getGender())
                .setPhone(dto.getPhone())
                .setLevel(dto.getLevel())
                .setStatus(RefereeConstants.STATUS_AVAILABLE);

        Referee inserted = refereeDAO.insert(referee);
        log.info("创建裁判: id={}, name={}", inserted.getId(), inserted.getName());
        return RefereeVO.from(inserted);
    }

    /**
     * 修改裁判信息
     * <p>
     * 需要管理员权限，支持部分更新语义，仅更新非空字段
     * </p>
     *
     * @param id  裁判ID
     * @param dto 修改裁判请求数据
     * @return 修改后的裁判 VO
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public RefereeVO update(Long id, RefereeUpdateDTO dto) {
        checkAdmin();

        Referee existing = getRefereeOrThrow(id);

        // 部分更新：仅更新非空字段
        if (StringUtils.hasText(dto.getName())) {
            existing.setName(dto.getName());
        }
        if (dto.getGender() != null) {
            existing.setGender(dto.getGender());
        }
        if (StringUtils.hasText(dto.getPhone())) {
            existing.setPhone(dto.getPhone());
        }
        if (StringUtils.hasText(dto.getLevel())) {
            existing.setLevel(dto.getLevel());
        }
        if (dto.getStatus() != null) {
            existing.setStatus(dto.getStatus());
        }

        Referee updated = refereeDAO.update(existing);
        log.info("修改裁判: id={}", id);
        return RefereeVO.from(updated);
    }

    /**
     * 删除裁判（逻辑删除）
     * <p>
     * 需要管理员权限，若该裁判存在未完成的裁判分配则禁止删除
     * </p>
     *
     * @param id 裁判ID
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteById(Long id) {
        checkAdmin();

        getRefereeOrThrow(id);

        // 删除前校验：若存在未完成的裁判分配则禁止删除
        Long activeCount = refereeDAO.countActiveAssignments(id);
        if (activeCount != null && activeCount > 0) {
            throw new BusinessException(ErrorCode.REFEREE_IN_USE);
        }

        refereeDAO.deleteById(id);
        log.info("删除裁判: id={}", id);
    }

    /**
     * 查询裁判赛程列表
     * <p>
     * 所有角色均可查询。委派 Mapper 的自定义 join 查询方法，返回结果已是 VO，无需转换
     * </p>
     *
     * @param id 裁判ID
     * @return 赛程 VO 列表（按比赛开始时间升序）
     */
    @Override
    public List<RefereeScheduleVO> getSchedule(Long id) {
        getRefereeOrThrow(id);
        return refereeMapper.selectRefereeSchedule(id);
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
     * 获取裁判，不存在则抛异常
     *
     * @param id 裁判ID
     * @return 裁判实体
     */
    private Referee getRefereeOrThrow(Long id) {
        Referee referee = refereeDAO.getById(id);
        if (referee == null) {
            throw new BusinessException(ErrorCode.REFEREE_NOT_FOUND);
        }
        return referee;
    }
}
