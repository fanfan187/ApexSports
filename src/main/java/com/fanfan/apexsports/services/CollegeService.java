package com.fanfan.apexsports.services;

import com.fanfan.apexsports.dtos.CollegeCreateDTO;
import com.fanfan.apexsports.dtos.CollegeUpdateDTO;
import com.fanfan.apexsports.vos.CollegeVO;

import java.util.List;

/**
 * 学院服务接口
 * <p>
 * 定义学院的列表查询、详情查询、创建、修改和删除等业务方法。
 * 所有角色均可执行读操作，写操作需要管理员权限（由实现层校验）
 * </p>
 *
 * @author fanfan187
 * @version v1.0.0
 * @since v1.0.0
 */
public interface CollegeService {

    /**
     * 查询所有学院列表
     * <p>
     * 所有角色均可查询，按 ID 升序返回
     * </p>
     *
     * @return 学院列表
     */
    List<CollegeVO> list();

    /**
     * 根据 ID 查询学院详情
     * <p>
     * 所有角色均可查询
     * </p>
     *
     * @param id 学院ID
     * @return 学院 VO
     */
    CollegeVO getById(Long id);

    /**
     * 新增学院
     * <p>
     * 需要管理员权限，校验学院代码唯一性
     * </p>
     *
     * @param dto 新增学院请求数据
     * @return 新创建的学院 VO
     */
    CollegeVO create(CollegeCreateDTO dto);

    /**
     * 修改学院信息
     * <p>
     * 需要管理员权限，支持部分更新语义，修改代码时需排除自身进行唯一性校验
     * </p>
     *
     * @param id  学院ID
     * @param dto 修改学院请求数据
     * @return 修改后的学院 VO
     */
    CollegeVO update(Long id, CollegeUpdateDTO dto);

    /**
     * 删除学院（逻辑删除）
     * <p>
     * 需要管理员权限，删除前校验是否存在关联运动员或参赛记录
     * </p>
     *
     * @param id 学院ID
     */
    void deleteById(Long id);
}
