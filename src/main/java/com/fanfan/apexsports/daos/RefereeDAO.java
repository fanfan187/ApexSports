package com.fanfan.apexsports.daos;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fanfan.apexsports.entities.Referee;
import com.fanfan.apexsports.mappers.RefereeMapper;
import com.fanfan.apexsports.vos.RefereeScheduleVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 裁判数据调度层
 * <p>
 * 协调 {@link RefereeMapper}，负责裁判数据的存储、查询和调度。
 * 作为普通 Spring Bean（{@code @Component}），不继承 MyBatis-Plus 的 Service 实现类，
 * 内部自动填充 createdAt / updatedAt，并统一通过 {@link LambdaQueryWrapper} 构建条件。
 * </p>
 * <p>
 * 除常规 CRUD（page / getById / insert / update / deleteById）外，
 * 额外提供 {@link #countActiveAssignments(Long)}：删除前活跃分配计数（委派给 Mapper 的 join SQL）。
 * </p>
 *
 * @author fanfan187
 * @version v1.0.0
 * @since v1.0.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RefereeDAO {

    private final RefereeMapper refereeMapper;

    // ==================== 查询操作 ====================

    /**
     * 分页查询裁判列表（支持多条件筛选）
     *
     * @param pageNum  当前页码
     * @param pageSize 每页大小
     * @param level    裁判等级筛选（可选，精确匹配等级字符串）
     * @param status   状态筛选（可选）：0-休息 / 1-可用
     * @param keyword  姓名关键字（可选，模糊匹配）
     * @return 分页结果（按 createdAt 降序）
     */
    public Page<Referee> page(int pageNum, int pageSize, String level, Integer status, String keyword) {
        LambdaQueryWrapper<Referee> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(StringUtils.hasText(level), Referee::getLevel, level)
                .eq(status != null, Referee::getStatus, status)
                .like(StringUtils.hasText(keyword), Referee::getName, keyword)
                .orderByDesc(Referee::getCreatedAt);
        return refereeMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
    }

    /**
     * 根据 ID 查询裁判
     *
     * @param id 裁判ID
     * @return 裁判实体，不存在返回 null
     */
    public Referee getById(Long id) {
        return refereeMapper.selectById(id);
    }

    /**
     * 删除前校验：统计该裁判身上"活跃且比赛未结束"的分配数
     * <p>
     * 委派给 {@link RefereeMapper#countActiveAssignmentsByRefereeId(Long)} 的跨表 join SQL。
     * 用于 DELETE /referees/{id} 接口的前置校验：若返回值 &gt; 0，
     * Logic 层应抛出 {@code BusinessException(REFEREE_HAS_ACTIVE_ASSIGNMENTS)}。
     * </p>
     * <p>本方法仅做数据调度，不抛业务异常。</p>
     *
     * @param refereeId 裁判ID（必填）
     * @return 活跃分配数（0 表示可安全删除）
     */
    public Long countActiveAssignments(Long refereeId) {
        return refereeMapper.countActiveAssignmentsByRefereeId(refereeId);
    }

    /**
     * 查询裁判赛程列表（4 表 join）
     * <p>
     * 委派给 {@link RefereeMapper#selectRefereeSchedule(Long)} 的跨表 join SQL，
     * 返回已是 VO，无需 DAO 层转换。Logic 层应调用本方法，不直接调用 Mapper。
     * </p>
     *
     * @param refereeId 裁判ID（必填）
     * @return 赛程 VO 列表（按比赛开始时间升序）
     */
    public List<RefereeScheduleVO> getSchedule(Long refereeId) {
        return refereeMapper.selectRefereeSchedule(refereeId);
    }

    // ==================== 写入操作 ====================

    /**
     * 插入裁判（自动填充 createdAt / updatedAt）
     * <p>
     * Referee 实体使用 {@code @Accessors(chain = true)}，支持链式 setter。
     * insert 后重新查询以确保返回 DB 自动生成字段（主键等）的完整对象。
     * </p>
     *
     * @param referee 裁判实体（不含主键和审计字段）
     * @return 插入后的裁判实体（含自动生成的主键），若插入失败可能返回 null
     */
    public Referee insert(Referee referee) {
        LocalDateTime now = LocalDateTime.now();
        referee.setCreatedAt(now).setUpdatedAt(now);
        refereeMapper.insert(referee);
        // 从数据库重新提取，确保字段完整
        return refereeMapper.selectById(referee.getId());
    }

    /**
     * 更新裁判（自动填充 updatedAt）
     * <p>
     * 仅刷新 updatedAt，其余字段以传入对象为准。update 后重新查询返回完整对象。
     * </p>
     *
     * @param referee 裁判实体（必须含主键）
     * @return 更新后的裁判实体，若 ID 不存在可能返回 null
     */
    public Referee update(Referee referee) {
        referee.setUpdatedAt(LocalDateTime.now());
        refereeMapper.updateById(referee);
        return refereeMapper.selectById(referee.getId());
    }

    /**
     * 逻辑删除裁判（MyBatis-Plus 自动处理 {@code @TableLogic}）
     *
     * @param id 裁判ID
     */
    public void deleteById(Long id) {
        refereeMapper.deleteById(id);
    }
}
