package com.fanfan.apexsports.services;

import com.fanfan.apexsports.dtos.AssignRefereeDTO;
import com.fanfan.apexsports.vos.RefereeAssignmentVO;

import java.util.List;

/**
 * 裁判分配服务接口
 * <p>
 * 定义为比赛场次分配裁判、查询裁判分配列表以及移除裁判分配等业务方法。
 * 分配与移除操作需要管理员权限，列表查询对所有角色开放（由实现层校验）
 * </p>
 *
 * @author fanfan187
 * @version v1.0.0
 * @since v1.0.0
 */
public interface RefereeAssignmentService {

    /**
     * 为比赛场次分配裁判
     * <p>
     * 需要管理员权限，校验：
     * <ul>
     *   <li>比赛场次存在</li>
     *   <li>裁判存在且状态可用（status=1）</li>
     *   <li>裁判时间不冲突（同一时间段只能主持一场）</li>
     *   <li>同一裁判不可重复分配到同一场次</li>
     *   <li>主裁判唯一性（每场仅一名主裁判）</li>
     * </ul>
     * </p>
     *
     * @param competitionId 比赛场次ID
     * @param dto           分配裁判请求数据
     * @return 新创建的裁判分配 VO
     */
    RefereeAssignmentVO assign(Long competitionId, AssignRefereeDTO dto);

    /**
     * 查询某场比赛的全部裁判分配列表
     * <p>
     * 所有角色均可查询，按角色升序返回
     * </p>
     *
     * @param competitionId 比赛场次ID
     * @return 裁判分配 VO 列表
     */
    List<RefereeAssignmentVO> listByCompetitionId(Long competitionId);

    /**
     * 移除裁判分配（逻辑删除）
     * <p>
     * 需要管理员权限，比赛已结束（status=2）时禁止移除。
     * 移除后驱逐秩序册缓存（影响 chiefReferee 字段）
     * </p>
     *
     * @param competitionId 比赛场次ID
     * @param assignmentId  裁判分配记录ID
     */
    void remove(Long competitionId, Long assignmentId);
}
