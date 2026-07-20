package com.fanfan.apexsports.services;

import com.fanfan.apexsports.vos.CollegeRankingVO;
import com.fanfan.apexsports.vos.CollegeScoreDetailVO;
import com.fanfan.apexsports.vos.ScoreRefreshResultVO;

import java.util.List;

/**
 * 排名服务接口
 * <p>
 * 定义学院排名相关的查询与积分刷新方法。
 * 读接口（listRankings、getCollegeDetail）对所有角色开放；
 * refreshScores 需要管理员权限（由实现层校验）
 * </p>
 *
 * @author fanfan187
 * @version v1.0.0
 * @since v1.0.0
 */
public interface RankingService {

    /**
     * 查询学院排名列表
     * <p>
     * 所有角色均可查询，按总积分降序返回
     * </p>
     *
     * @return 学院排名列表
     */
    List<CollegeRankingVO> listRankings();

    /**
     * 根据 ID 查询学院积分详情
     * <p>
     * 所有角色均可查询，含排名汇总与各项目明细
     * </p>
     *
     * @param collegeId 学院ID
     * @return 学院积分详情 VO
     */
    CollegeScoreDetailVO getCollegeDetail(Long collegeId);

    /**
     * 刷新学院积分汇总
     * <p>
     * 需要管理员权限，基于全量重算并加刷新锁防止并发
     * </p>
     *
     * @return 刷新结果 VO
     */
    ScoreRefreshResultVO refreshScores();
}
