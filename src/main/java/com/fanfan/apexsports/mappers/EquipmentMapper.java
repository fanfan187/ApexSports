package com.fanfan.apexsports.mappers;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fanfan.apexsports.entities.Equipment;
import org.apache.ibatis.annotations.Mapper;

/**
 * 器材 Mapper
 * <p>
 * 继承 MyBatis-Plus BaseMapper，提供器材表的 CRUD 基础操作
 * </p>
 *
 * <p><b>说明：</b>器材管理无需自定义 SQL，全部使用 LambdaQueryWrapper 完成条件查询。</p>
 *
 * @author fanfan187
 * @version v1.0.0
 * @since v1.0.0
 */
@Mapper
public interface EquipmentMapper extends BaseMapper<Equipment> {
}
