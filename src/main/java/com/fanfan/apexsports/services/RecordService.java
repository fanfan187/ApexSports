package com.fanfan.apexsports.services;

import com.fanfan.apexsports.dtos.RecordCreateDTO;
import com.fanfan.apexsports.dtos.RecordUpdateDTO;
import com.fanfan.apexsports.vos.RecordDetailVO;
import com.fanfan.apexsports.vos.RecordListVO;

import java.util.List;

/**
 * 记录服务接口
 * <p>
 * 定义校记录 / 市记录 / 省记录的列表查询、详情查询、创建（含个人/团体互斥校验）、
 * 修改（部分更新语义）等业务方法。
 * 读操作（列表 / 详情）对所有角色开放，写操作（创建 / 修改）需要管理员权限（由实现层校验）。
 * eventId / athleteId / collegeId 为不可变属性，修改接口无法变更这三项。
 * </p>
 *
 * @author fanfan187
 * @version v1.0.0
 * @since v1.0.0
 */
public interface RecordService {

    /**
     * 查询记录列表（不分页）
     * <p>
     * 支持按项目ID、记录类型（1-校记录 / 2-市记录 / 3-省记录）、状态（0-已作废 / 1-有效）可选筛选。
     * 所有角色均可查询。返回结果按创造记录日期倒序，并附带项目名称（由实现层批量查询 Event 后填充）
     * </p>
     *
     * @param eventId    项目ID筛选（可选）
     * @param recordType 记录类型筛选（可选）：1-校记录 / 2-市记录 / 3-省记录
     * @param status     状态筛选（可选）：0-已作废 / 1-有效
     * @return 记录列表 VO（按创造记录日期倒序）
     */
    List<RecordListVO> list(Long eventId, Integer recordType, Integer status);

    /**
     * 根据 ID 查询记录详情
     * <p>
     * 所有角色均可查询，记录不存在抛出 {@link com.fanfan.apexsports.exception.BusinessException}
     * （RECORD_NOT_FOUND）
     * </p>
     *
     * @param id 记录ID
     * @return 记录详情 VO（含关联项目名称）
     */
    RecordDetailVO getById(Long id);

    /**
     * 新增记录
     * <p>
     * 需要管理员权限。校验：
     * <ol>
     *   <li>eventId 指向的项目必须存在</li>
     *   <li>个人项目（type=1）：必须提供 athleteId + athleteName，禁止提供 collegeId / collegeName</li>
     *   <li>团体项目（type=2）：必须提供 collegeId + collegeName，禁止提供 athleteId / athleteName</li>
     * </ol>
     * 状态默认为有效（1），由 DAO 层填充。允许同一项目同一类型存在多条记录（业务决策）
     * </p>
     *
     * @param dto 新增记录请求数据
     * @return 新创建的记录详情 VO
     */
    RecordDetailVO create(RecordCreateDTO dto);

    /**
     * 修改记录信息
     * <p>
     * 需要管理员权限，支持部分更新语义（null / 空字符串表示不修改该字段）。
     * 可修改字段：recordType / athleteName / collegeName / score / scoreDisplay / recordDate / competitionName / status；
     * eventId / athleteId / collegeId 为不可变属性，不在此接口暴露。
     * 缓存一致性由 DAO 层 {@code updateSelective} 内部处理（G2 修复）
     * </p>
     *
     * @param id  记录ID
     * @param dto 修改记录请求数据（全部字段可选）
     * @return 修改后的记录详情 VO
     */
    RecordDetailVO update(Long id, RecordUpdateDTO dto);
}
