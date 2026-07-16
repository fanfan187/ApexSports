package com.fanfan.apexsports.logic;

import com.fanfan.apexsports.config.UserContext;
import com.fanfan.apexsports.constants.EventConstants;
import com.fanfan.apexsports.constants.UserConstants;
import com.fanfan.apexsports.daos.EventDAO;
import com.fanfan.apexsports.daos.RecordDAO;
import com.fanfan.apexsports.dtos.RecordCreateDTO;
import com.fanfan.apexsports.dtos.RecordUpdateDTO;
import com.fanfan.apexsports.entities.Event;
import com.fanfan.apexsports.entities.Record;
import com.fanfan.apexsports.exception.BusinessException;
import com.fanfan.apexsports.exception.ErrorCode;
import com.fanfan.apexsports.services.RecordService;
import com.fanfan.apexsports.vos.RecordDetailVO;
import com.fanfan.apexsports.vos.RecordListVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 记录业务实现
 * <p>
 * 实现校记录 / 市记录 / 省记录的列表查询、详情查询、创建（含个人/团体互斥校验、eventId 存在性校验）、
 * 修改（部分更新语义，缓存一致性由 DAO 内部处理）等业务逻辑。
 * 读操作对所有角色开放，写操作（create / update）需要管理员权限。
 * </p>
 * <p>
 * 个人/团体校验依据 {@link EventConstants#TYPE_INDIVIDUAL} / {@link EventConstants#TYPE_TEAM}：
 * 个人项目必须提供运动员信息、禁止提供学院信息；团体项目反之。违反时抛出 PARAM_VALIDATION_FAILED。
 * </p>
 *
 * @author fanfan187
 * @version v1.0.0
 * @since v1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RecordLogic implements RecordService {

    private final RecordDAO recordDAO;
    private final EventDAO eventDAO;

    /**
     * 查询记录列表（不分页），结果按创造记录日期倒序，并附带项目名称
     * <p>
     * 所有角色均可查询。先取记录 DO 列表，再批量查询关联项目（按不重复的 eventId 集合），
     * 构建 eventId → eventName 映射后在内存中完成 DO → VO 转换
     * </p>
     *
     * @param eventId    项目ID筛选（可选）
     * @param recordType 记录类型筛选（可选）
     * @param status     状态筛选（可选）
     * @return 记录列表 VO
     */
    @Override
    public List<RecordListVO> list(Long eventId, Integer recordType, Integer status) {
        List<Record> records = recordDAO.findAll(eventId, recordType, status);
        if (records.isEmpty()) {
            return List.of();
        }
        // 收集不重复的 eventId，批量查询项目名称（避免 N+1 查询）
        Set<Long> eventIds = records.stream()
                .map(Record::getEventId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, String> eventNameMap = eventDAO.findByIds(eventIds).stream()
                .filter(e -> e.getName() != null)
                .collect(Collectors.toMap(Event::getId, Event::getName));
        return records.stream()
                .map(r -> RecordListVO.from(
                        r,
                        r.getEventId() != null ? eventNameMap.get(r.getEventId()) : null
                ))
                .toList();
    }

    /**
     * 根据 ID 查询记录详情
     * <p>
     * 所有角色均可查询。记录不存在抛出 RECORD_NOT_FOUND
     * </p>
     *
     * @param id 记录ID
     * @return 记录详情 VO（含关联项目名称）
     */
    @Override
    public RecordDetailVO getById(Long id) {
        Record record = getRecordOrThrow(id);
        Event event = eventDAO.getById(record.getEventId());
        return RecordDetailVO.from(record, event != null ? event.getName() : null);
    }

    /**
     * 新增记录
     * <p>
     * 需要管理员权限。校验 eventId 存在性 + 个人/团体互斥规则后组装实体，
     * 主键 / 审计字段 / 逻辑删除标记 / 状态默认值由 DAO 层填充
     * </p>
     *
     * @param dto 新增记录请求数据
     * @return 新创建的记录详情 VO
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public RecordDetailVO create(RecordCreateDTO dto) {
        checkAdmin();

        // 1. 校验项目存在性
        Event event = eventDAO.getById(dto.getEventId());
        if (event == null) {
            throw new BusinessException(ErrorCode.EVENT_NOT_FOUND);
        }

        // 2. 个人 / 团体互斥校验
        validateRecordSubjectByEventType(dto, event);

        // 3. 组装实体：不 set 主键 / 审计字段 / 逻辑删除标记 / status（DAO 层统一填充）
        Record record = new Record()
                .setEventId(dto.getEventId())
                .setRecordType(dto.getRecordType())
                .setAthleteId(dto.getAthleteId())
                .setCollegeId(dto.getCollegeId())
                .setAthleteName(dto.getAthleteName())
                .setCollegeName(dto.getCollegeName())
                .setScore(dto.getScore())
                .setScoreDisplay(dto.getScoreDisplay())
                .setRecordDate(dto.getRecordDate())
                .setCompetitionName(dto.getCompetitionName());

        // 4. 写入（DAO 内部填充默认值 + 驱逐该维度缓存）
        Record inserted = recordDAO.insertRecord(record);
        log.info("创建记录: id={}, eventId={}, recordType={}",
                inserted.getId(), inserted.getEventId(), inserted.getRecordType());
        return RecordDetailVO.from(inserted, event.getName());
    }

    /**
     * 修改记录（部分更新语义）
     * <p>
     * 需要管理员权限。仅更新 DTO 中非空字段，eventId / athleteId / collegeId 不可变（DTO 中不暴露）。
     * 缓存一致性由 DAO 层 {@link RecordDAO#updateSelective} 内部处理（G2 修复）
     * </p>
     *
     * @param id  记录ID
     * @param dto 修改记录请求数据
     * @return 修改后的记录详情 VO
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public RecordDetailVO update(Long id, RecordUpdateDTO dto) {
        checkAdmin();
        // 不存在则抛 RECORD_NOT_FOUND（updateSelective 在记录缺失时静默返回，需由 Logic 层先校验）
        getRecordOrThrow(id);

        // 部分更新：构建 patch，仅 set 非空字段（MBP updateById 只写非空字段）
        Record patch = new Record();
        if (dto.getRecordType() != null) {
            patch.setRecordType(dto.getRecordType());
        }
        if (StringUtils.hasText(dto.getAthleteName())) {
            patch.setAthleteName(dto.getAthleteName());
        }
        if (StringUtils.hasText(dto.getCollegeName())) {
            patch.setCollegeName(dto.getCollegeName());
        }
        if (dto.getScore() != null) {
            patch.setScore(dto.getScore());
        }
        if (StringUtils.hasText(dto.getScoreDisplay())) {
            patch.setScoreDisplay(dto.getScoreDisplay());
        }
        if (dto.getRecordDate() != null) {
            patch.setRecordDate(dto.getRecordDate());
        }
        if (StringUtils.hasText(dto.getCompetitionName())) {
            patch.setCompetitionName(dto.getCompetitionName());
        }
        if (dto.getStatus() != null) {
            patch.setStatus(dto.getStatus());
        }

        recordDAO.updateSelective(id, patch);
        log.info("修改记录: id={}", id);

        // 重新查库返回最新数据 + 关联项目名称
        Record updated = recordDAO.getById(id);
        Event event = eventDAO.getById(updated.getEventId());
        return RecordDetailVO.from(updated, event != null ? event.getName() : null);
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
     * 获取记录，不存在则抛异常
     *
     * @param id 记录ID
     * @return 记录实体
     */
    private Record getRecordOrThrow(Long id) {
        Record record = recordDAO.getById(id);
        if (record == null) {
            throw new BusinessException(ErrorCode.RECORD_NOT_FOUND);
        }
        return record;
    }

    /**
     * 按项目类型校验记录主体的互斥规则
     * <p>
     * 个人项目（{@link EventConstants#TYPE_INDIVIDUAL}）：必须提供 athleteId + athleteName，禁止 collegeId / collegeName；
     * 团体项目（{@link EventConstants#TYPE_TEAM}）：必须提供 collegeId + collegeName，禁止 athleteId / athleteName。
     * 违反任一规则抛出 PARAM_VALIDATION_FAILED
     * </p>
     *
     * @param dto   新增记录请求数据
     * @param event 关联项目（调用方已校验非空）
     */
    private void validateRecordSubjectByEventType(RecordCreateDTO dto, Event event) {
        if (EventConstants.TYPE_INDIVIDUAL == event.getType()) {
            // 个人项目：必须有 athleteId + athleteName；禁止 collegeId / collegeName
            if (dto.getAthleteId() == null
                    || !StringUtils.hasText(dto.getAthleteName())
                    || dto.getCollegeId() != null
                    || StringUtils.hasText(dto.getCollegeName())) {
                throw new BusinessException(ErrorCode.PARAM_VALIDATION_FAILED);
            }
        } else if (EventConstants.TYPE_TEAM == event.getType()) {
            // 团体项目：必须有 collegeId + collegeName；禁止 athleteId / athleteName
            if (dto.getCollegeId() == null
                    || !StringUtils.hasText(dto.getCollegeName())
                    || dto.getAthleteId() != null
                    || StringUtils.hasText(dto.getAthleteName())) {
                throw new BusinessException(ErrorCode.PARAM_VALIDATION_FAILED);
            }
        }
        // 其他类型（理论上不存在）：不强制校验，由调用方/数据完整性兜底
    }
}
