package com.fanfan.apexsports.services;

import com.fanfan.apexsports.dtos.EventCreateDTO;
import com.fanfan.apexsports.dtos.EventUpdateDTO;
import com.fanfan.apexsports.vos.EventVO;
import com.fanfan.apexsports.vos.RecordVO;

import java.util.List;

/**
 * 比赛项目服务接口
 * <p>
 * 定义比赛项目的列表查询、详情查询、创建、修改、删除以及历史记录查询等业务方法。
 * 所有角色均可执行读操作，写操作需要管理员权限（由实现层校验）
 * </p>
 *
 * @author fanfan187
 * @version v1.0.0
 * @since v1.0.0
 */
public interface EventService {

    /**
     * 查询比赛项目列表
     * <p>
     * 所有角色均可查询，支持按项目类型/类别/性别/状态可选过滤，按 ID 升序返回
     * </p>
     *
     * @param type     项目类型（1-个人，2-集体），可为 null 表示不限制
     * @param category 项目类别（1-径赛，2-田赛），可为 null 表示不限制
     * @param gender   性别限制（0-女子，1-男子，2-混合），可为 null 表示不限制
     * @param status   状态（0-停用，1-启用），可为 null 表示不限制
     * @return 比赛项目列表
     */
    List<EventVO> list(Integer type, Integer category, Integer gender, Integer status);

    /**
     * 根据 ID 查询比赛项目详情
     * <p>
     * 所有角色均可查询
     * </p>
     *
     * @param id 项目ID
     * @return 比赛项目 VO
     */
    EventVO getById(Long id);

    /**
     * 新增比赛项目
     * <p>
     * 需要管理员权限，校验项目代码唯一性；
     * 业务规则：当类型为个人项目（type=1）时，强制 {@code maxParticipants=null}
     * </p>
     *
     * @param dto 新增比赛项目请求数据
     * @return 新创建的比赛项目 VO
     */
    EventVO create(EventCreateDTO dto);

    /**
     * 修改比赛项目信息
     * <p>
     * 需要管理员权限，支持部分更新语义，修改代码时需排除自身进行唯一性校验；
     * 业务规则：当类型变为个人项目（type=1）时，强制 {@code maxParticipants=null}
     * </p>
     *
     * @param id  项目ID
     * @param dto 修改比赛项目请求数据
     * @return 修改后的比赛项目 VO
     */
    EventVO update(Long id, EventUpdateDTO dto);

    /**
     * 删除比赛项目（逻辑删除）
     * <p>
     * 需要管理员权限，删除前校验是否存在关联报名记录（个人/集体）或赛程安排
     * </p>
     *
     * @param id 项目ID
     */
    void deleteById(Long id);

    /**
     * 查询指定比赛项目下的历史记录
     * <p>
     * 所有角色均可查询，仅返回有效（status=1）的记录
     * </p>
     *
     * @param id         项目ID
     * @param recordType 记录类型（1-校记录，2-市记录，3-省记录），可为 null 表示不限制
     * @return 有效记录列表（按记录日期倒序排列）
     */
    List<RecordVO> getRecords(Long id, Integer recordType);
}
