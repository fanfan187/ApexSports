package com.fanfan.apexsports.mappers;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fanfan.apexsports.entities.RefereeAssignment;
import org.apache.ibatis.annotations.Mapper;

/**
 * 裁判分配 Mapper
 * <p>
 * 继承 MyBatis-Plus BaseMapper，提供裁判分配表（as_referee_assignment）的 CRUD 基础操作
 * </p>
 *
 * @author fanfan187
 * @version v1.0.0
 * @since v1.0.0
 */
@Mapper
public interface RefereeAssignmentMapper extends BaseMapper<RefereeAssignment> {
}
