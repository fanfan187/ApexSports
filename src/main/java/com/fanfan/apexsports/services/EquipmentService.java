package com.fanfan.apexsports.services;

import com.fanfan.apexsports.dtos.EquipmentCreateDTO;
import com.fanfan.apexsports.dtos.EquipmentUpdateDTO;
import com.fanfan.apexsports.vos.EquipmentVO;

import java.util.List;

/**
 * 器材服务接口
 * <p>
 * 定义器材的列表查询、详情查询、创建、修改、删除等业务方法。
 * 读操作（列表 / 详情）对所有角色开放，写操作（创建 / 修改 / 删除）需要管理员权限（由实现层校验）。
 * 库存数量（totalQuantity / availableQuantity）通过原子条件 SQL 更新，避免并发超卖。
 * </p>
 *
 * @author fanfan187
 * @version v1.0.0
 * @since v1.0.0
 */
public interface EquipmentService {

    /**
     * 全量查询器材列表（不分页）
     * <p>
     * 所有角色均可查询，支持按器材类型（字符串）和状态（0-停用 / 1-可用）可选筛选
     * </p>
     *
     * @param type   器材类型筛选（可选）
     * @param status 状态筛选（可选）
     * @return 器材 VO 列表（按创建时间倒序）
     */
    List<EquipmentVO> list(String type, Integer status);

    /**
     * 根据 ID 查询器材详情
     * <p>
     * 所有角色均可查询
     * </p>
     *
     * @param id 器材ID
     * @return 器材 VO
     */
    EquipmentVO getById(Long id);

    /**
     * 新增器材
     * <p>
     * 需要管理员权限，校验 code 唯一性、totalQuantity 非负，状态默认为可用（1），
     * 新增时 availableQuantity 初始化等于 totalQuantity
     * </p>
     *
     * @param dto 新增器材请求数据
     * @return 新创建的器材 VO
     */
    EquipmentVO create(EquipmentCreateDTO dto);

    /**
     * 修改器材信息
     * <p>
     * 需要管理员权限，支持部分更新语义，修改 code 时需排除自身进行唯一性校验，
     * 修改 totalQuantity 时通过原子条件 SQL 同步调整 availableQuantity（差额更新，防超卖）
     * </p>
     *
     * @param id  器材ID
     * @param dto 修改器材请求数据
     * @return 修改后的器材 VO
     */
    EquipmentVO update(Long id, EquipmentUpdateDTO dto);

    /**
     * 删除器材（逻辑删除）
     * <p>
     * 需要管理员权限，若该器材存在未归还的使用记录（as_competition_equipment 中 status != 2）则禁止删除
     * </p>
     *
     * @param id 器材ID
     */
    void deleteById(Long id);
}
