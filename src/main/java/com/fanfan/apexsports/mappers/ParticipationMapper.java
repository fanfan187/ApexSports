package com.fanfan.apexsports.mappers;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fanfan.apexsports.entities.Participation;
import org.apache.ibatis.annotations.Mapper;

/**
 * 参赛记录 Mapper
 * <p>
 * 继承 MyBatis-Plus BaseMapper，提供参赛记录表（participation）的 CRUD 基础操作。
 * 软删除通过实体上的 {@code @TableLogic} 自动处理，无需手写 SQL
 * </p>
 *
 * @author fanfan187
 * @version v1.0.0
 * @since v1.0.0
 */
@Mapper
public interface ParticipationMapper extends BaseMapper<Participation> {
}
