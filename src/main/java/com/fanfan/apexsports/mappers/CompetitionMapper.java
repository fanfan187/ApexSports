package com.fanfan.apexsports.mappers;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fanfan.apexsports.entities.Competition;
import org.apache.ibatis.annotations.Mapper;

/**
 * 比赛场次 Mapper
 * <p>
 * 继承 MyBatis-Plus BaseMapper，提供比赛场次表的 CRUD 基础操作
 * </p>
 *
 * @author fanfan187
 * @version v1.0.0
 * @since v1.0.0
 */
@Mapper
public interface CompetitionMapper extends BaseMapper<Competition> {
}
