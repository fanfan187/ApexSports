package com.fanfan.apexsports.services;

import com.fanfan.apexsports.common.result.PageResult;
import com.fanfan.apexsports.dtos.AthleteCreateDTO;
import com.fanfan.apexsports.dtos.AthleteUpdateDTO;
import com.fanfan.apexsports.vos.AthleteVO;

/**
 * 运动员服务接口
 * <p>
 * 定义运动员的分页查询、详情查询、创建、修改和删除等业务方法。
 * 所有角色均可执行读操作，写操作需要管理员权限（由实现层校验）
 * </p>
 *
 * @author fanfan187
 * @version v1.0.0
 * @since v1.0.0
 */
public interface AthleteService {

    /**
     * 分页查询运动员列表
     * <p>
     * 所有角色均可查询，支持按学院、姓名、学号、状态、性别筛选
     * </p>
     *
     * @param current   当前页码
     * @param size      每页大小
     * @param collegeId 学院ID筛选（可选）
     * @param name      运动员姓名（可选，模糊匹配）
     * @param studentNo 学号（可选，精确匹配）
     * @param status    状态筛选（可选）
     * @param gender    性别筛选（可选）
     * @return 分页结果
     */
    PageResult<AthleteVO> page(Integer current, Integer size, Long collegeId, String name, String studentNo, Integer status, Integer gender);

    /**
     * 根据 ID 查询运动员详情
     * <p>
     * 所有角色均可查询，返回包含学院名称与学院代码的详情视图
     * </p>
     *
     * @param id 运动员ID
     * @return 运动员 VO
     */
    AthleteVO getById(Long id);

    /**
     * 新增运动员
     * <p>
     * 需要管理员权限，校验所属学院存在性和学号唯一性，状态默认为正常
     * </p>
     *
     * @param dto 新增运动员请求数据
     * @return 新创建的运动员 VO
     */
    AthleteVO create(AthleteCreateDTO dto);

    /**
     * 修改运动员信息
     * <p>
     * 需要管理员权限，支持部分更新语义，修改学号时需排除自身进行唯一性校验
     * </p>
     *
     * @param id  运动员ID
     * @param dto 修改运动员请求数据
     * @return 修改后的运动员 VO
     */
    AthleteVO update(Long id, AthleteUpdateDTO dto);

    /**
     * 删除运动员（逻辑删除）
     * <p>
     * 需要管理员权限
     * </p>
     *
     * @param id 运动员ID
     */
    void deleteById(Long id);
}
