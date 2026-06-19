package com.fanfan.apexsports.services;

import com.fanfan.apexsports.common.result.PageResult;
import com.fanfan.apexsports.dtos.RefereeCreateDTO;
import com.fanfan.apexsports.dtos.RefereeUpdateDTO;
import com.fanfan.apexsports.vos.RefereeScheduleVO;
import com.fanfan.apexsports.vos.RefereeVO;

import java.util.List;

/**
 * 裁判服务接口
 * <p>
 * 定义裁判的分页查询、详情查询、创建、修改、删除和赛程查询等业务方法。
 * 读操作（分页 / 详情 / 赛程）对所有角色开放，写操作（创建 / 修改 / 删除）需要管理员权限（由实现层校验）
 * </p>
 *
 * @author fanfan187
 * @version v1.0.0
 * @since v1.0.0
 */
public interface RefereeService {

    /**
     * 分页查询裁判列表
     * <p>
     * 所有角色均可查询，支持按裁判等级、状态和姓名关键字可选筛选
     * </p>
     *
     * @param current 当前页码（可选，默认 1）
     * @param size    每页大小（可选，默认 10，最大 100）
     * @param level   裁判等级筛选（可选，精确匹配）
     * @param status  状态筛选（可选）：0-休息 / 1-可用
     * @param keyword 姓名关键字（可选，模糊匹配）
     * @return 分页结果（按创建时间倒序）
     */
    PageResult<RefereeVO> page(Integer current, Integer size, String level, Integer status, String keyword);

    /**
     * 根据 ID 查询裁判详情
     * <p>
     * 所有角色均可查询
     * </p>
     *
     * @param id 裁判ID
     * @return 裁判 VO
     */
    RefereeVO getById(Long id);

    /**
     * 新增裁判
     * <p>
     * 需要管理员权限，状态默认设为可用（1），不由前端传入
     * </p>
     *
     * @param dto 新增裁判请求数据
     * @return 新创建的裁判 VO
     */
    RefereeVO create(RefereeCreateDTO dto);

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
    RefereeVO update(Long id, RefereeUpdateDTO dto);

    /**
     * 删除裁判（逻辑删除）
     * <p>
     * 需要管理员权限，若该裁判存在未完成的裁判分配则禁止删除
     * </p>
     *
     * @param id 裁判ID
     */
    void deleteById(Long id);

    /**
     * 查询裁判赛程列表
     * <p>
     * 所有角色均可查询，返回该裁判被分配的所有比赛场次（按开始时间升序）
     * </p>
     *
     * @param id 裁判ID
     * @return 赛程 VO 列表
     */
    List<RefereeScheduleVO> getSchedule(Long id);
}
