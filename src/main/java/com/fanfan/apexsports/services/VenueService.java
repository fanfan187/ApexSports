package com.fanfan.apexsports.services;

import com.fanfan.apexsports.dtos.VenueCreateDTO;
import com.fanfan.apexsports.dtos.VenueUpdateDTO;
import com.fanfan.apexsports.vos.VenueScheduleVO;
import com.fanfan.apexsports.vos.VenueVO;

import java.time.LocalDate;
import java.util.List;

/**
 * 场地服务接口
 * <p>
 * 定义场地的列表查询、详情查询、创建、修改、删除和赛程占用查询等业务方法。
 * 读操作（列表 / 详情 / 赛程）对所有角色开放，写操作（创建 / 修改 / 删除）需要管理员权限（由实现层校验）
 * </p>
 *
 * @author fanfan187
 * @version v1.0.0
 * @since v1.0.0
 */
public interface VenueService {

    /**
     * 全量查询场地列表（不分页）
     * <p>
     * 所有角色均可查询，支持按类型（1-径赛 / 2-田赛 / 3-综合）和状态（0-不可用 / 1-可用）可选筛选
     * </p>
     *
     * @param type   场地类型筛选（可选）
     * @param status 状态筛选（可选）
     * @return 场地 VO 列表（按创建时间倒序）
     */
    List<VenueVO> list(Integer type, Integer status);

    /**
     * 根据 ID 查询场地详情
     * <p>
     * 所有角色均可查询
     * </p>
     *
     * @param id 场地ID
     * @return 场地 VO
     */
    VenueVO getById(Long id);

    /**
     * 新增场地
     * <p>
     * 需要管理员权限，校验 type 合法值（1/2/3）和 code 唯一性，状态默认为可用（1）
     * </p>
     *
     * @param dto 新增场地请求数据
     * @return 新创建的场地 VO
     */
    VenueVO create(VenueCreateDTO dto);

    /**
     * 修改场地信息
     * <p>
     * 需要管理员权限，支持部分更新语义，修改 code 时需排除自身进行唯一性校验，修改 type 时校验合法值
     * </p>
     *
     * @param id  场地ID
     * @param dto 修改场地请求数据
     * @return 修改后的场地 VO
     */
    VenueVO update(Long id, VenueUpdateDTO dto);

    /**
     * 删除场地（逻辑删除）
     * <p>
     * 需要管理员权限，若该场地存在关联赛程则禁止删除
     * </p>
     *
     * @param id 场地ID
     */
    void deleteById(Long id);

    /**
     * 查询场地赛程占用情况
     * <p>
     * 所有角色均可查询，支持按日期可选筛选，返回该场地被安排的所有比赛场次（按开始时间升序）
     * </p>
     *
     * @param id   场地ID
     * @param date 筛选日期（可选，null 时返回全部赛程）
     * @return 赛程 VO 列表
     */
    List<VenueScheduleVO> getSchedule(Long id, LocalDate date);
}
