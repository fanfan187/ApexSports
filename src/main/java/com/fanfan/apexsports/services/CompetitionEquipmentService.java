package com.fanfan.apexsports.services;

import com.fanfan.apexsports.dtos.AssignEquipmentDTO;
import com.fanfan.apexsports.dtos.UpdateEquipmentStatusDTO;
import com.fanfan.apexsports.vos.CompetitionEquipmentVO;

import java.util.List;

/**
 * 赛程器材分配业务接口
 * <p>
 * 定义为比赛场次分配器材、查询器材使用列表、更新器材状态（归还）的业务契约。
 * 写操作需要管理员权限，读操作对所有角色开放。
 * </p>
 *
 * @author fanfan187
 * @version v1.0.0
 * @since v1.0.0
 */
public interface CompetitionEquipmentService {

    /**
     * 为比赛场次分配器材，同步扣减库存
     *
     * @param competitionId 比赛场次ID
     * @param dto           分配器材请求数据
     * @return 新创建的器材分配 VO
     */
    CompetitionEquipmentVO assign(Long competitionId, AssignEquipmentDTO dto);

    /**
     * 查询某场比赛的所有器材使用记录
     *
     * @param competitionId 比赛场次ID
     * @return 器材使用记录列表
     */
    List<CompetitionEquipmentVO> listByCompetitionId(Long competitionId);

    /**
     * 更新器材使用状态（流转为使用中或归还），归还时恢复库存
     *
     * @param competitionId 比赛场次ID
     * @param recordId      器材使用记录ID
     * @param dto           状态更新请求数据
     */
    void updateStatus(Long competitionId, Long recordId, UpdateEquipmentStatusDTO dto);
}
