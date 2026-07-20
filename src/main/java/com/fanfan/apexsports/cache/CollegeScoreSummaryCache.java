package com.fanfan.apexsports.cache;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import com.fanfan.apexsports.vos.CollegeRankingVO;
import lombok.RequiredArgsConstructor;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * 学院积分排名缓存管理
 * <p>
 * 仅负责「学院排名列表」与「单学院排名行」两类缓存的生命周期管理，
 * 并封装「积分刷新」分布式锁语义。排名属于「低写高读」数据（手动全量重算触发写，
 * 列表与详情高频查询），对列表与单行做 cache-aside；刷新互斥锁保证同一时刻只有一个
 * 刷新任务在执行，避免并发重算造成数据不一致。
 * </p>
 * <p>
 * 内部私有化 Key 格式、TTL 策略与序列化方式，对外只暴露业务语义方法，
 * 严格遵守「Cache 层之外不得直接操作缓存」与「Redis 不得出现在 Logic 层」红线。
 * {@link RedissonClient}/{@link RLock} 仅在本类内部使用，Logic 层调用方
 * 仅见到 {@code tryAcquireRefreshLock()} / {@code releaseRefreshLock()}，
 * 不感知底层锁实现。
 * </p>
 *
 * <h3>设计决策</h3>
 * <ul>
 *   <li>排名为「低写高读」数据，TTL 设为 30 分钟，与 {@link ScoreCache} 一致，
 *       兼顾新鲜度与命中率；刷新后由 DAO/Logic 触发 {@link #evictAll()} 立即失效</li>
 *   <li>序列化方式与 {@link ScoreCache} / {@link CompetitionCache} 一致：
 *       {@code RedisTemplate<String, Object>} + Jackson JSON（配置见 RedisConfig），
 *       因 {@code GenericJackson2JsonRedisSerializer} 未开启默认类型信息，
 *       反序列化结果是 {@code LinkedHashMap}，需用内部 {@link ObjectMapper#convertValue(Object, Class)}
 *       做二次转换还原为 {@link CollegeRankingVO} 列表或单行</li>
 *   <li>驱逐采用 SCAN 而非 KEYS，避免在 key 数量增长时阻塞缓存</li>
 *   <li>刷新锁采用 Redisson {@code tryLock(0, -1, SECONDS)}：不等待、不自动过期，
 *       由调用方在 {@code finally} 中调用 {@link #releaseRefreshLock()} 显式释放，
 *       保证刷新原子性</li>
 * </ul>
 *
 * @author fanfan187
 * @version v1.0.0
 * @since v1.0.0
 */
@Component
@RequiredArgsConstructor
public class CollegeScoreSummaryCache {

    // ==================== Key 格式（私有化，不对外暴露） ====================

    /**
     * 排名列表缓存 Key（单 key，因排名不分页）。
     * <p>
     * 排名列表数据量小且常驻首页，无需按分页参数加 suffix，固定单 key。
     * </p>
     */
    private static final String LIST_KEY = "ranking:colleges:list";

    /**
     * 单学院排名行缓存 Key 前缀，完整格式：{@code ranking:colleges:single:{collegeId}}。
     * <p>
     * 详情页按 collegeId 直查排名行，suffix 即 collegeId。
     * </p>
     */
    private static final String SINGLE_KEY_PREFIX = "ranking:colleges:single:";

    /**
     * 积分刷新分布式锁 Key。
     * <p>
     * 锁语义属缓存一致性范畴，由本 Cache 类持有；Logic 层调用
     * {@link #tryAcquireRefreshLock()} / {@link #releaseRefreshLock()}，
     * 不感知底层 Redisson 实现。
     * </p>
     */
    private static final String REFRESH_LOCK_KEY = "ranking:refresh:lock";

    // ==================== TTL 策略（私有化） ====================

    /** 排名缓存过期时间：30 分钟（低写高读，兼顾新鲜度与命中率） */
    private static final Duration CACHE_TTL = Duration.ofMinutes(30);

    /** SCAN 单批次建议扫描数量（与 {@link ScoreCache} 保持一致） */
    private static final int SCAN_BATCH_SIZE = 100;

    // ==================== 依赖注入 ====================

    /**
     * 缓存操作模板，Value 使用 Jackson JSON 序列化（见 {@code RedisConfig}），
     * 与 {@link ScoreCache} / {@link CompetitionCache} 共用同一序列化配置
     */
    private final RedisTemplate<String, Object> redisTemplate;

    /**
     * Redisson 客户端，由 {@code redisson-spring-boot-starter} 自动装配
     * （{@code application.yml} 第52行 {@code spring.data.redis.redisson.config} 配置单机模式）。
     * <p>
     * 仅在本类内部用于获取分布式锁，不对外暴露。
     * </p>
     */
    private final RedissonClient redissonClient;

    /**
     * 内部 ObjectMapper，用于缓存反序列化后的二次类型转换。
     * <p>
     * 由于 {@code RedisConfig} 的 {@code GenericJackson2JsonRedisSerializer} 未开启默认类型信息，
     * 反序列化结果是 {@code LinkedHashMap} 而非目标 POJO。这里用与 RedisConfig 一致的时间模块配置，
     * 通过 {@link ObjectMapper#convertValue(Object, Class)} 做二次转换还原为
     * {@link CollegeRankingVO} 列表或单行。
     * </p>
     */
    private final ObjectMapper objectMapper = buildObjectMapper();

    private static ObjectMapper buildObjectMapper() {
        ObjectMapper om = new ObjectMapper();
        JavaTimeModule javaTimeModule = new JavaTimeModule();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        javaTimeModule.addSerializer(LocalDateTime.class, new LocalDateTimeSerializer(formatter));
        javaTimeModule.addDeserializer(LocalDateTime.class, new LocalDateTimeDeserializer(formatter));
        om.registerModule(javaTimeModule);
        om.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        return om;
    }

    // ==================== 排名列表缓存 ====================

    /**
     * 查询排名列表缓存。
     *
     * @return 命中则返回排名列表，未命中返回 {@code null}（由调用方 DAO 回源查视图）
     */
    public List<CollegeRankingVO> getList() {
        Object cached = redisTemplate.opsForValue().get(LIST_KEY);
        if (cached == null) {
            return null;
        }
        return objectMapper.convertValue(cached, new TypeReference<List<CollegeRankingVO>>() {});
    }

    /**
     * 写入排名列表缓存。
     *
     * @param data 待缓存的排名列表
     */
    public void cacheList(List<CollegeRankingVO> data) {
        redisTemplate.opsForValue().set(LIST_KEY, data, CACHE_TTL);
    }

    // ==================== 单学院排名行缓存 ====================

    /**
     * 查询单学院排名行缓存。
     *
     * @param collegeId 学院ID
     * @return 命中则返回该学院排名行，未命中返回 {@code null}（由调用方 DAO 回源查视图）
     */
    public CollegeRankingVO getSingle(Long collegeId) {
        Object cached = redisTemplate.opsForValue().get(buildSingleKey(collegeId));
        if (cached == null) {
            return null;
        }
        return objectMapper.convertValue(cached, CollegeRankingVO.class);
    }

    /**
     * 写入单学院排名行缓存。
     *
     * @param collegeId 学院ID
     * @param data      待缓存的该学院排名行
     */
    public void cacheSingle(Long collegeId, CollegeRankingVO data) {
        redisTemplate.opsForValue().set(buildSingleKey(collegeId), data, CACHE_TTL);
    }

    // ==================== 驱逐 ====================

    /**
     * 驱逐全部排名缓存（列表 + 单行）。
     * <p>
     * 由「手动全量重算」完成后或跨维度写操作触发，扫描 {@code ranking:colleges:*}
     * 模式，保证下次查询回源取最新。实现采用 SCAN 游标扫描而非 KEYS，避免阻塞缓存。
     * </p>
     */
    public void evictAll() {
        scanAndDelete("ranking:colleges:*");
    }

    // ==================== 刷新分布式锁（封装 Redisson，不向 Logic 暴露实现） ====================

    /**
     * 尝试获取积分刷新锁（不等待）。
     * <p>
     * 内部使用 Redisson {@code tryLock(0, -1, SECONDS)}：等待时间 0（拿不到立即返回 false），
     * 租约时间 -1（不自动过期，由调用方在 {@code finally} 中显式释放）。
     * 调用方必须确保 {@link #releaseRefreshLock()} 在 finally 块中调用，避免锁泄漏。
     * </p>
     *
     * @return 获取成功返回 {@code true}，已被其他线程持有返回 {@code false}
     */
    public boolean tryAcquireRefreshLock() {
        RLock lock = redissonClient.getLock(REFRESH_LOCK_KEY);
        try {
            return lock.tryLock(0, -1, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    /**
     * 释放积分刷新锁。
     * <p>
     * 仅当锁由当前线程持有时才释放，避免 {@code IllegalMonitorStateException}。
     * </p>
     */
    public void releaseRefreshLock() {
        RLock lock = redissonClient.getLock(REFRESH_LOCK_KEY);
        if (lock.isHeldByCurrentThread()) {
            lock.unlock();
        }
    }

    // ==================== 内部工具 ====================

    /**
     * 构造单学院排名行缓存 Key：{@code ranking:colleges:single:{collegeId}}。
     */
    private String buildSingleKey(Long collegeId) {
        return SINGLE_KEY_PREFIX + collegeId;
    }

    /**
     * 按模式 SCAN 扫描并删除所有匹配的 key。
     *
     * @param pattern Key 匹配模式（如 {@code ranking:colleges:*}）
     */
    private void scanAndDelete(String pattern) {
        ScanOptions options = ScanOptions.scanOptions()
                .match(pattern)
                .count(SCAN_BATCH_SIZE)
                .build();

        Set<String> keys = redisTemplate.execute((RedisCallback<Set<String>>) connection -> {
            Set<String> matched = new HashSet<>();
            // Cursor 的 close()/hasNext()/next() 均不声明受检异常，故无需 try-catch IOException
            try (Cursor<byte[]> cursor = connection.scan(options)) {
                while (cursor.hasNext()) {
                    matched.add(new String(cursor.next(), StandardCharsets.UTF_8));
                }
            }
            return matched;
        });

        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
        }
    }
}
