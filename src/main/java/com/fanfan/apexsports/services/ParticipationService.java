package com.fanfan.apexsports.services;

import com.fanfan.apexsports.common.result.PageResult;
import com.fanfan.apexsports.dtos.ParticipationCreateDTO;
import com.fanfan.apexsports.vos.ParticipationVO;

/**
 * 参赛记录服务接口
 * <p>
 * 定义个人参赛记录的分页查询、详情查询、创建、删除和检录等业务方法。
 * 所有角色均可执行读操作；创建/删除/检录等写操作需要相应权限（由实现层校验）。
 * </p>
 * <p>
 * 设计说明：page 方法保留 collegeId 参数（Controller 从前端接收），由 Logic 层
 * 负责将 collegeId 转换为 athleteIds 后再调用 DAO（Participation 表无 collegeId 字段）
 * </p>
 *
 * @author fanfan187
 * @version v1.0.0
 * @since v1.0.0
 */
public interface ParticipationService {

    /**
     * 分页查询参赛记录列表
     * <p>
     * 支持按项目、学院、运动员、状态、比赛场次筛选。
     * collegeId 由 Logic 层转换为对应运动员ID集合后筛选
     * </p>
     *
     * @param current       当前页码
     * @param size          每页大小
     * @param eventId       项目ID筛选（可选）
     * @param collegeId     学院ID筛选（可选，Logic 层转换为 athleteIds）
     * @param athleteId     运动员ID筛选（可选）
     * @param status        状态筛选（可选）
     * @param competitionId 比赛场次ID筛选（可选）
     * @return 分页结果
     */
    PageResult<ParticipationVO> page(Integer current, Integer size, Long eventId, Long collegeId,
                                     Long athleteId, Integer status, Long competitionId);

    /**
     * 根据 ID 查询参赛记录详情
     * <p>
     * 返回包含运动员姓名/学号、学院信息、项目名称的详情视图
     * </p>
     *
     * @param id 参赛记录ID
     * @return 参赛记录 VO
     */
    ParticipationVO getById(Long id);

    /**
     * 新增个人参赛记录
     * <p>
     * 校验运动员与项目的存在性、同运动员+项目唯一性，状态默认为 0（已报名），
     * 时间字段由 DAO 填充
     * </p>
     *
     * @param dto 新增参赛记录请求数据
     * @return 新创建的参赛记录 VO
     */
    ParticipationVO create(ParticipationCreateDTO dto);

    /**
     * 删除参赛记录（逻辑删除）
     *
     * @param id 参赛记录ID
     */
    void deleteById(Long id);

    /**
     * 检录（将状态从"已报名"更新为"检录通过"）
     * <p>
     * 需校验参赛记录存在性及当前状态，并校验关联比赛场次是否处于允许检录的状态
     * </p>
     *
     * @param id 参赛记录ID
     */
    void checkIn(Long id);
}
