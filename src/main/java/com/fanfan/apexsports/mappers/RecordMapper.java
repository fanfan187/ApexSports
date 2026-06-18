package com.fanfan.apexsports.mappers;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fanfan.apexsports.entities.Record;
import org.apache.ibatis.annotations.Mapper;

/**
 * 记录 Mapper
 * <p>
 * 继承 MyBatis-Plus BaseMapper，提供记录表的 CRUD 基础操作，主要供 EventDAO 调用
 * </p>
 *
 * @author fanfan187
 * @version v1.0.0
 * @since v1.0.0
 */
@Mapper
public interface RecordMapper extends BaseMapper<Record> {
}
