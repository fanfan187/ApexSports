package com.fanfan.apexsports.mappers;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fanfan.apexsports.entities.Athlete;
import org.apache.ibatis.annotations.Mapper;

/**
 * 运动员 Mapper
 * <p>
 * 继承 MyBatis-Plus BaseMapper，提供运动员表的 CRUD 基础操作
 * </p>
 *
 * @author fanfan187
 * @version v1.0.0
 * @since v1.0.0
 */
@Mapper
public interface AthleteMapper extends BaseMapper<Athlete> {
}
