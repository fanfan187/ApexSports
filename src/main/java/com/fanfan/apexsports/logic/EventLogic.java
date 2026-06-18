package com.fanfan.apexsports.logic;

import com.fanfan.apexsports.config.UserContext;
import com.fanfan.apexsports.constants.EventConstants;
import com.fanfan.apexsports.constants.UserConstants;
import com.fanfan.apexsports.daos.EventDAO;
import com.fanfan.apexsports.dtos.EventCreateDTO;
import com.fanfan.apexsports.dtos.EventUpdateDTO;
import com.fanfan.apexsports.entities.Event;
import com.fanfan.apexsports.entities.Record;
import com.fanfan.apexsports.exception.BusinessException;
import com.fanfan.apexsports.exception.ErrorCode;
import com.fanfan.apexsports.services.EventService;
import com.fanfan.apexsports.vos.EventVO;
import com.fanfan.apexsports.vos.RecordVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 比赛项目业务实现
 * <p>
 * 实现比赛项目的列表查询、详情查询、创建（含代码唯一性校验、个人项目强制清空最大参赛人数）、
 * 修改（部分更新语义、排除自身代码唯一性校验）、删除（引用完整性校验：个人报名、集体报名、赛程三表）、
 * 历史记录查询等业务逻辑。
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
public class EventLogic implements EventService {

    private final EventDAO eventDAO;

    /**
     * 查询比赛项目列表
     * <p>
     * 所有角色均可查询，无需管理员权限
     * </p>
     *
     * @param type     项目类型（1-个人，2-集体），可为 null 表示不限制
     * @param category 项目类别（1-径赛，2-田赛），可为 null 表示不限制
     * @param gender   性别限制（0-女子，1-男子，2-混合），可为 null 表示不限制
     * @param status   状态（0-停用，1-启用），可为 null 表示不限制
     * @return 比赛项目列表
     */
    @Override
    public List<EventVO> list(Integer type, Integer category, Integer gender, Integer status) {
        return eventDAO.findAll(type, category, gender, status).stream()
                .map(EventVO::from)
                .toList();
    }

    /**
     * 根据 ID 查询比赛项目详情
     * <p>
     * 所有角色均可查询，无需管理员权限
     * </p>
     *
     * @param id 项目ID
     * @return 比赛项目 VO
     */
    @Override
    public EventVO getById(Long id) {
        Event event = getEventOrThrow(id);
        return EventVO.from(event);
    }

    /**
     * 新增比赛项目
     * <p>
     * 需要管理员权限，校验项目代码唯一性；
     * 业务规则：当类型为个人项目（type=1）时，无论前端传入何值，强制 {@code maxParticipants=null}
     * </p>
     *
     * @param dto 新增比赛项目请求数据
     * @return 新创建的比赛项目 VO
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public EventVO create(EventCreateDTO dto) {
        checkAdmin();

        // 校验项目代码唯一性
        Long count = eventDAO.countByCode(dto.getCode());
        if (count > 0) {
            throw new BusinessException(ErrorCode.EVENT_CODE_EXISTS);
        }

        // 构建实体
        Event event = new Event()
                .setName(dto.getName())
                .setCode(dto.getCode())
                .setType(dto.getType())
                .setCategory(dto.getCategory())
                .setGender(dto.getGender())
                .setUnit(dto.getUnit())
                .setMaxParticipants(dto.getMaxParticipants())
                .setMaxIndividualEvents(dto.getMaxIndividualEvents())
                .setDescription(dto.getDescription());

        // 业务规则：个人项目（type=1）强制 maxParticipants=null，不信任前端传入值
        if (EventConstants.TYPE_INDIVIDUAL == event.getType()) {
            event.setMaxParticipants(null);
        }

        Event inserted = eventDAO.insert(event);
        log.info("创建比赛项目: id={}, code={}", inserted.getId(), inserted.getCode());
        return EventVO.from(inserted);
    }

    /**
     * 修改比赛项目信息
     * <p>
     * 需要管理员权限，支持部分更新语义，修改代码时排除自身进行唯一性校验。
     * 业务规则：当类型变为个人项目（type=1）时，强制 {@code maxParticipants=null}，
     * 覆盖任何前端传入的 maxParticipants 值
     * </p>
     *
     * @param id  项目ID
     * @param dto 修改比赛项目请求数据
     * @return 修改后的比赛项目 VO
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public EventVO update(Long id, EventUpdateDTO dto) {
        checkAdmin();

        Event event = getEventOrThrow(id);

        // 部分更新：仅更新非空字段
        if (dto.getName() != null) {
            event.setName(dto.getName());
        }
        if (dto.getCode() != null) {
            // 校验项目代码唯一性（排除自身）
            Long count = eventDAO.countByCodeExcludeId(dto.getCode(), id);
            if (count > 0) {
                throw new BusinessException(ErrorCode.EVENT_CODE_EXISTS);
            }
            event.setCode(dto.getCode());
        }
        if (dto.getType() != null) {
            event.setType(dto.getType());
        }
        if (dto.getCategory() != null) {
            event.setCategory(dto.getCategory());
        }
        if (dto.getGender() != null) {
            event.setGender(dto.getGender());
        }
        if (dto.getUnit() != null) {
            event.setUnit(dto.getUnit());
        }
        if (dto.getMaxParticipants() != null) {
            event.setMaxParticipants(dto.getMaxParticipants());
        }
        if (dto.getMaxIndividualEvents() != null) {
            event.setMaxIndividualEvents(dto.getMaxIndividualEvents());
        }
        if (dto.getDescription() != null) {
            event.setDescription(dto.getDescription());
        }
        if (dto.getStatus() != null) {
            event.setStatus(dto.getStatus());
        }

        // 业务规则：若最终类型为个人项目（type=1），强制 maxParticipants=null
        // 注意：此规则在所有字段更新后执行，无论 dto 是否传入 maxParticipants/type
        if (EventConstants.TYPE_INDIVIDUAL == event.getType()) {
            event.setMaxParticipants(null);
        }

        Event updated = eventDAO.update(event);
        log.info("修改比赛项目: id={}", id);
        return EventVO.from(updated);
    }

    /**
     * 删除比赛项目（逻辑删除）
     * <p>
     * 需要管理员权限，删除前校验三张引用表：
     * 个人参赛记录（as_participation）、集体参赛记录（as_team_participation）、赛程安排（as_competition），
     * 任一表存在未删除引用即拒绝删除
     * </p>
     *
     * @param id 项目ID
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteById(Long id) {
        checkAdmin();

        getEventOrThrow(id);

        // 引用完整性校验：个人参赛记录
        if (eventDAO.countParticipationsByEventId(id) > 0) {
            throw new BusinessException(ErrorCode.EVENT_IN_USE);
        }

        // 引用完整性校验：集体参赛记录
        if (eventDAO.countTeamParticipationsByEventId(id) > 0) {
            throw new BusinessException(ErrorCode.EVENT_IN_USE);
        }

        // 引用完整性校验：赛程安排
        if (eventDAO.countCompetitionsByEventId(id) > 0) {
            throw new BusinessException(ErrorCode.EVENT_IN_USE);
        }

        eventDAO.deleteById(id);
        log.info("删除比赛项目: id={}", id);
    }

    /**
     * 查询指定比赛项目下的历史记录
     * <p>
     * 所有角色均可查询，无需管理员权限；先校验项目存在，再返回该项目下的有效记录
     * </p>
     *
     * @param id         项目ID
     * @param recordType 记录类型（1-校记录，2-市记录，3-省记录），可为 null 表示不限制
     * @return 有效记录列表（按记录日期倒序排列）
     */
    @Override
    public List<RecordVO> getRecords(Long id, Integer recordType) {
        getEventOrThrow(id); // 校验项目存在
        return eventDAO.getRecordsByEventId(id, recordType).stream()
                .map(RecordVO::from)
                .toList();
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
     * 获取比赛项目，不存在则抛异常
     *
     * @param id 项目ID
     * @return 比赛项目实体
     */
    private Event getEventOrThrow(Long id) {
        Event event = eventDAO.getById(id);
        if (event == null) {
            throw new BusinessException(ErrorCode.EVENT_NOT_FOUND);
        }
        return event;
    }
}
