package com.fanfan.apexsports.services;

import com.fanfan.apexsports.common.result.PageResult;
import com.fanfan.apexsports.dtos.TeamParticipationCreateDTO;
import com.fanfan.apexsports.vos.TeamParticipationVO;

/**
 * 团体参赛记录服务接口
 * <p>
 * 定义团体参赛记录的分页查询、详情查询、创建、删除和检录等业务方法。
 * 注意：团体版 page 方法比个人版（ParticipationService）少一个 athleteId 参数，
 * 因为 team_participation 表无 athlete_id 字段。
 * </p>
 *
 * @author fanfan187
 * @version v1.0.0
 * @since v1.0.0
 */
public interface TeamParticipationService {

    /**
     * 分页查询团体参赛记录
     * <p>
     * 支持按项目ID、学院ID、状态、比赛场次ID筛选
     * </p>
     *
     * @param current       当前页码
     * @param size          每页大小
     * @param eventId       项目ID（可空）
     * @param collegeId     学院ID（可空）
     * @param status        状态（可空）
     * @param competitionId 比赛场次ID（可空）
     * @return 分页结果
     */
    PageResult<TeamParticipationVO> page(Integer current, Integer size, Long eventId, Long collegeId,
                                         Integer status, Long competitionId);

    /**
     * 根据 ID 查询团体参赛记录详情
     *
     * @param id 团体参赛记录ID
     * @return 团体参赛记录 VO
     */
    TeamParticipationVO getById(Long id);

    /**
     * 新增团体参赛记录
     * <p>
     * 校验：学院存在性、项目存在性、同学院+项目唯一性，状态默认设为 0（已报名）
     * </p>
     *
     * @param dto 新增团体参赛记录请求数据
     * @return 新创建的团体参赛记录 VO
     */
    TeamParticipationVO create(TeamParticipationCreateDTO dto);

    /**
     * 删除团体参赛记录（逻辑删除）
     *
     * @param id 团体参赛记录ID
     */
    void deleteById(Long id);

    /**
     * 检录（将状态从"已报名"更新为"检录通过"）
     * <p>
     * 校验：记录存在性、当前状态为"已报名"、比赛场次检录时间窗口
     * </p>
     *
     * @param id 团体参赛记录ID
     */
    void checkIn(Long id);
}
