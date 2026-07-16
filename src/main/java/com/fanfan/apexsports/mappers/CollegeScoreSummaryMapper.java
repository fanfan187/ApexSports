package com.fanfan.apexsports.mappers;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fanfan.apexsports.entities.CollegeScoreSummary;
import org.apache.ibatis.annotations.Mapper;

/**
 * 学院积分汇总 Mapper
 * <p>
 * 继承 MyBatis-Plus BaseMapper，提供 as_college_score_summary 表的 CRUD 基础操作。
 * upsert（全量重算）等业务逻辑由 DAO 层实现，本接口不新增自定义方法。
 * </p>
 *
 * @author fanfan187
 * @version v1.0.0
 * @since v1.0.0
 */
@Mapper
public interface CollegeScoreSummaryMapper extends BaseMapper<CollegeScoreSummary> {
}
