package com.fanfan.apexsports.logic;

import com.fanfan.apexsports.config.UserContext;
import com.fanfan.apexsports.constants.UserConstants;
import com.fanfan.apexsports.constants.VenueConstants;
import com.fanfan.apexsports.daos.VenueDAO;
import com.fanfan.apexsports.dtos.VenueCreateDTO;
import com.fanfan.apexsports.dtos.VenueUpdateDTO;
import com.fanfan.apexsports.entities.Venue;
import com.fanfan.apexsports.exception.BusinessException;
import com.fanfan.apexsports.exception.ErrorCode;
import com.fanfan.apexsports.services.VenueService;
import com.fanfan.apexsports.vos.VenueScheduleVO;
import com.fanfan.apexsports.vos.VenueVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.util.List;

/**
 * 场地业务实现
 * <p>
 * 实现场地的列表查询、详情查询、创建（含 type 合法值与 code 唯一性校验）、
 * 修改（部分更新语义、code 修改排除自身）、删除（含关联赛程占用检查）和赛程占用查询等业务逻辑。
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
public class VenueLogic implements VenueService {

    private final VenueDAO venueDAO;

    /**
     * 全量查询场地列表（不分页）
     * <p>
     * 所有角色均可查询，无需管理员权限。直接将 DO 列表转换为 VO 列表
     * </p>
     *
     * @param type   场地类型筛选（可选）
     * @param status 状态筛选（可选）
     * @return 场地 VO 列表
     */
    @Override
    public List<VenueVO> list(Integer type, Integer status) {
        return venueDAO.findAll(type, status).stream()
                .map(VenueVO::from)
                .toList();
    }

    /**
     * 根据 ID 查询场地详情
     * <p>
     * 所有角色均可查询
     * </p>
     *
     * @param id 场地ID
     * @return 场地 VO
     */
    @Override
    public VenueVO getById(Long id) {
        Venue venue = getVenueOrThrow(id);
        return VenueVO.from(venue);
    }

    /**
     * 新增场地
     * <p>
     * 需要管理员权限，校验 type 合法值（1-径赛 / 2-田赛 / 3-综合）和 code 唯一性，状态默认为可用（1）
     * </p>
     *
     * @param dto 新增场地请求数据
     * @return 新创建的场地 VO
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public VenueVO create(VenueCreateDTO dto) {
        checkAdmin();

        // 校验 type 合法值
        validateType(dto.getType());

        // 校验 code 唯一性
        Long count = venueDAO.countByCode(dto.getCode());
        if (count > 0) {
            throw new BusinessException(ErrorCode.VENUE_CODE_EXISTS);
        }

        // 构建实体，状态默认为可用
        Venue venue = new Venue()
                .setName(dto.getName())
                .setCode(dto.getCode())
                .setType(dto.getType())
                .setLocation(dto.getLocation())
                .setCapacity(dto.getCapacity())
                .setStatus(VenueConstants.STATUS_AVAILABLE);

        Venue inserted = venueDAO.insert(venue);
        log.info("创建场地: id={}, code={}", inserted.getId(), inserted.getCode());
        return VenueVO.from(inserted);
    }

    /**
     * 修改场地信息
     * <p>
     * 需要管理员权限，支持部分更新语义。修改 code 时排除自身进行唯一性校验，
     * 修改 type 时校验合法值（1/2/3）
     * </p>
     *
     * @param id  场地ID
     * @param dto 修改场地请求数据
     * @return 修改后的场地 VO
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public VenueVO update(Long id, VenueUpdateDTO dto) {
        checkAdmin();

        Venue venue = getVenueOrThrow(id);

        // 部分更新：仅更新非空字段
        if (StringUtils.hasText(dto.getName())) {
            venue.setName(dto.getName());
        }
        if (StringUtils.hasText(dto.getCode())) {
            // 校验 code 唯一性（排除自身）
            Long count = venueDAO.countByCodeExcludeId(dto.getCode(), id);
            if (count > 0) {
                throw new BusinessException(ErrorCode.VENUE_CODE_EXISTS);
            }
            venue.setCode(dto.getCode());
        }
        if (dto.getType() != null) {
            // 校验 type 合法值
            validateType(dto.getType());
            venue.setType(dto.getType());
        }
        if (StringUtils.hasText(dto.getLocation())) {
            venue.setLocation(dto.getLocation());
        }
        if (dto.getCapacity() != null) {
            venue.setCapacity(dto.getCapacity());
        }
        if (dto.getStatus() != null) {
            venue.setStatus(dto.getStatus());
        }

        Venue updated = venueDAO.update(venue);
        log.info("修改场地: id={}", id);
        return VenueVO.from(updated);
    }

    /**
     * 删除场地（逻辑删除）
     * <p>
     * 需要管理员权限，若该场地存在关联赛程（as_competition.venue_id）则禁止删除
     * </p>
     *
     * @param id 场地ID
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteById(Long id) {
        checkAdmin();

        getVenueOrThrow(id);

        // 检查赛程关联
        Long competitionCount = venueDAO.countCompetitionsByVenueId(id);
        if (competitionCount > 0) {
            throw new BusinessException(ErrorCode.VENUE_IN_USE);
        }

        venueDAO.deleteById(id);
        log.info("删除场地: id={}", id);
    }

    /**
     * 查询场地赛程占用情况
     * <p>
     * 所有角色均可查询。直接返回 DAO 的 join 查询结果（已是 VO，无需转换）
     * </p>
     *
     * @param id   场地ID
     * @param date 筛选日期（可选，null 时返回全部赛程）
     * @return 赛程 VO 列表
     */
    @Override
    public List<VenueScheduleVO> getSchedule(Long id, LocalDate date) {
        getVenueOrThrow(id);
        return venueDAO.findScheduleByVenueId(id, date);
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
     * 获取场地，不存在则抛异常
     *
     * @param id 场地ID
     * @return 场地实体
     */
    private Venue getVenueOrThrow(Long id) {
        Venue venue = venueDAO.getById(id);
        if (venue == null) {
            throw new BusinessException(ErrorCode.VENUE_NOT_FOUND);
        }
        return venue;
    }

    /**
     * 校验场地类型合法性
     * <p>
     * 仅允许 1（径赛）、2（田赛）、3（综合）三个值
     * </p>
     *
     * @param type 场地类型
     */
    private void validateType(Integer type) {
        if (type == null
                || (type != VenueConstants.TYPE_TRACK
                && type != VenueConstants.TYPE_FIELD
                && type != VenueConstants.TYPE_COMPOSITE)) {
            throw new BusinessException(ErrorCode.PARAM_VALIDATION_FAILED);
        }
    }
}
