package com.fanfan.apexsports.mappers;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fanfan.apexsports.entities.TeamParticipation;
import org.apache.ibatis.annotations.Mapper;

/**
 * 团体参赛记录 Mapper
 * <p>
 * 继承 MyBatis-Plus BaseMapper，提供团体参赛记录表（team_participation）的 CRUD 基础操作
 * </p>
 *
 * @author fanfan187
 * @version v1.0.0
 * @since v1.0.0
 */
@Mapper
public interface TeamParticipationMapper extends BaseMapper<TeamParticipation> {
}
