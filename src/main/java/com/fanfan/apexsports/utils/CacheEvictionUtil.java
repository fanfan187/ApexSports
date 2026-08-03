package com.fanfan.apexsports.utils;

import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * 缓存驱逐事务工具类
 * <p>
 * 提供「事务提交后驱逐缓存」的统一入口，解决 cache-aside 模式下
 * 「写操作在事务内驱逐缓存」引发的一致性竞态：
 * </p>
 * <pre>
 * 事务内驱逐（旧模式）：
 *   T1 写库（未提交）→ T1 驱逐缓存 → T2 读未命中 → T2 回源查到旧数据并回填缓存
 *   → T1 提交 → 缓存中滞留旧数据直至 TTL 过期
 * </pre>
 * <p>
 * 本工具将驱逐动作推迟到事务成功提交之后执行（{@code afterCommit}），
 * 保证驱逐发生时数据库已落地新值，并发读回源必然取到新数据。
 * 事务回滚时 {@code afterCommit} 不会触发，缓存保持旧值与库一致，无需驱逐。
 * </p>
 * <p>
 * <b>无事务场景兜底：</b>若当前线程不存在活动事务同步
 * （如 DAO 方法被无事务上下文的调用方直接使用），驱逐立即执行，
 * 行为与旧的同步驱逐完全一致，保证语义兼容。
 * </p>
 * <p>
 * <b>异常语义：</b>驱逐在提交后执行，若驱逐本身抛异常（如缓存中间件不可用），
 * 异常会传播给调用方，但此时事务已提交不会回滚；缓存将在 TTL 到期后自愈。
 * </p>
 *
 * @author fanfan187
 * @version v1.0.0
 * @since v1.0.0
 */
public final class CacheEvictionUtil {

    private CacheEvictionUtil() {}

    /**
     * 在事务成功提交后执行缓存驱逐；无活动事务时立即执行。
     * <p>
     * 典型用法：DAO 写操作（增/改/删）完成数据库写入后，
     * 将一组驱逐动作合并为单个 {@link Runnable} 传入：
     * </p>
     * <pre>{@code
     * CacheEvictionUtil.evictAfterCommit(() -> {
     *     scoreCache.evictList();
     *     scoreCache.evictCompetition(competitionId);
     * });
     * }</pre>
     *
     * @param eviction 驱逐动作（可包含多个缓存维度的一次性驱逐）
     */
    public static void evictAfterCommit(Runnable eviction) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    eviction.run();
                }
            });
        } else {
            eviction.run();
        }
    }
}
