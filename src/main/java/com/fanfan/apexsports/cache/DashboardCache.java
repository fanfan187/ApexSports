package com.fanfan.apexsports.cache;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateDeserializer;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateSerializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import com.fanfan.apexsports.constants.DashboardConstants;
import com.fanfan.apexsports.vos.BreakingRecordsVO;
import com.fanfan.apexsports.vos.DashboardOverviewVO;
import com.fanfan.apexsports.vos.TodayScheduleVO;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.Set;

/**
 * 首页看板缓存管理
 * <p>
 * 仅负责看板三个只读聚合结果（概览 / 今日赛程 / 破纪录汇总）的缓存生命周期管理。
 * 内部私有化 Redis Key 格式、TTL 策略与序列化方式，对外只暴露业务语义方法，
 * 严格遵守「Cache 层之外不得直接操作 Redis」「Cache 不得出现 db/rdb 字样」的红线规范。
 * </p>
 * <p>
 * 设计决策：
 * <ul>
 *   <li>TTL 取 {@link DashboardConstants#CACHE_TTL_SECONDS}（60 秒），看板为高读低写聚合，
 *       以新鲜度优先；二次请求可命中以降低 DB 压力。</li>
 *   <li>序列化方式与 {@link CompetitionCache} / {@link RecordCache} 一致：
 *       {@code RedisTemplate<String, Object>} + Jackson JSON（配置见 RedisConfig），
 *       但因 {@code GenericJackson2JsonRedisSerializer} 未开启默认类型信息，
 *       反序列化结果是 {@code LinkedHashMap}，故用内部 {@link ObjectMapper#convertValue(Object, Class)}
 *       做二次转换还原为 VO 类型。</li>
 *   <li>三类 Key 前缀统一 {@code dashboard:}，今日赛程 key 携带 {@code yyyy-MM-dd} 日期后缀，
 *       保证不同日期独立缓存；{@link #evictAll()} 采用 SCAN 扫描 {@code dashboard:*} 模式。</li>
 *   <li>{@link #buildObjectMapper()} 完整套用 {@link RecordCache}，同时注册
 *       {@link LocalDateTime}（"yyyy-MM-dd HH:mm:ss"）与 {@link LocalDate}（"yyyy-MM-dd"）的
 *       序列化/反序列化器，支持带时间字段的 VO 直接存储。</li>
 * </ul>
 * </p>
 *
 * @author fanfan187
 * @version v1.0.0
 * @since v1.0.0
 */
@Component
@RequiredArgsConstructor
public class DashboardCache {

    // ==================== Key 格式（私有化，不对外暴露） ====================

    /** 概览缓存 Key（全局唯一，无变量段） */
    private static final String OVERVIEW_KEY = "dashboard:overview";

    /** 今日赛程缓存 Key 前缀，完整格式：{@code dashboard:today:{yyyy-MM-dd}} */
    private static final String TODAY_KEY_PREFIX = "dashboard:today:";

    /** 破纪录汇总缓存 Key（全局唯一，无变量段） */
    private static final String BREAKING_KEY = "dashboard:records:breaking";

    /** 全局看板缓存 Key 前缀，用于 {@link #evictAll()} 的 SCAN 模式 */
    private static final String DASHBOARD_KEY_PREFIX = "dashboard:";

    // ==================== TTL 策略（私有化） ====================

    /** 看板缓存过期时间，取自 {@link DashboardConstants#CACHE_TTL_SECONDS} */
    private static final Duration CACHE_TTL = Duration.ofSeconds(DashboardConstants.CACHE_TTL_SECONDS);

    /** SCAN 单批次建议扫描数量（与 {@link CompetitionCache} 保持一致） */
    private static final int SCAN_BATCH_SIZE = 100;

    // ==================== 依赖注入 ====================

    /**
     * Redis 操作模板，Value 使用 Jackson JSON 序列化（见 {@code RedisConfig}），
     * 与 {@link CompetitionCache} / {@link RecordCache} 共用同一序列化配置
     */
    private final RedisTemplate<String, Object> redisTemplate;

    /**
     * 内部 ObjectMapper，用于 Redis 反序列化后的二次类型转换。
     * <p>
     * 由于 {@code RedisConfig} 的 {@code GenericJackson2JsonRedisSerializer} 未开启默认类型信息，
     * 反序列化结果是 {@code LinkedHashMap} 而非目标 POJO。这里用与 RedisConfig 一致的时间模块配置，
     * 通过 {@link ObjectMapper#convertValue(Object, Class)} 做二次转换还原为看板 VO。
     * </p>
     */
    private final ObjectMapper objectMapper = buildObjectMapper();

    private static ObjectMapper buildObjectMapper() {
        ObjectMapper om = new ObjectMapper();
        JavaTimeModule javaTimeModule = new JavaTimeModule();
        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        javaTimeModule.addSerializer(LocalDateTime.class, new LocalDateTimeSerializer(dateTimeFormatter));
        javaTimeModule.addDeserializer(LocalDateTime.class, new LocalDateTimeDeserializer(dateTimeFormatter));
        javaTimeModule.addSerializer(LocalDate.class, new LocalDateSerializer(dateFormatter));
        javaTimeModule.addDeserializer(LocalDate.class, new LocalDateDeserializer(dateFormatter));
        om.registerModule(javaTimeModule);
        om.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        return om;
    }

    // ==================== 概览缓存 ====================

    /**
     * 查询概览缓存。
     *
     * @return 命中则返回概览 VO，未命中返回 {@code null}（由调用方 DAO 回源）
     */
    public DashboardOverviewVO getOverview() {
        Object cached = redisTemplate.opsForValue().get(OVERVIEW_KEY);
        if (cached == null) {
            return null;
        }
        return objectMapper.convertValue(cached, new TypeReference<DashboardOverviewVO>() {});
    }

    /**
     * 写入概览缓存。
     *
     * @param vo 概览聚合 VO 快照
     */
    public void cacheOverview(DashboardOverviewVO vo) {
        redisTemplate.opsForValue().set(OVERVIEW_KEY, vo, CACHE_TTL);
    }

    /**
     * 驱逐概览缓存。
     */
    public void evictOverview() {
        redisTemplate.delete(OVERVIEW_KEY);
    }

    // ==================== 今日赛程缓存 ====================

    /**
     * 构造今日赛程缓存 Key：{@code dashboard:today:{yyyy-MM-dd}}。
     */
    private String buildTodayKey(LocalDate today) {
        return TODAY_KEY_PREFIX + today.format(DashboardConstants.DATE_FMT);
    }

    /**
     * 查询指定日期的今日赛程缓存。
     *
     * @param today 服务器今日日期
     * @return 命中则返回今日赛程 VO，未命中返回 {@code null}
     */
    public TodayScheduleVO getTodaySchedule(LocalDate today) {
        Object cached = redisTemplate.opsForValue().get(buildTodayKey(today));
        if (cached == null) {
            return null;
        }
        return objectMapper.convertValue(cached, new TypeReference<TodayScheduleVO>() {});
    }

    /**
     * 写入指定日期的今日赛程缓存。
     *
     * @param today 服务器今日日期
     * @param vo    今日赛程聚合 VO 快照
     */
    public void cacheTodaySchedule(LocalDate today, TodayScheduleVO vo) {
        redisTemplate.opsForValue().set(buildTodayKey(today), vo, CACHE_TTL);
    }

    /**
     * 驱逐指定日期的今日赛程缓存。
     *
     * @param today 服务器今日日期
     */
    public void evictTodaySchedule(LocalDate today) {
        redisTemplate.delete(buildTodayKey(today));
    }

    // ==================== 破纪录汇总缓存 ====================

    /**
     * 查询破纪录汇总缓存。
     *
     * @return 命中则返回破纪录 VO，未命中返回 {@code null}
     */
    public BreakingRecordsVO getBreakingRecords() {
        Object cached = redisTemplate.opsForValue().get(BREAKING_KEY);
        if (cached == null) {
            return null;
        }
        return objectMapper.convertValue(cached, new TypeReference<BreakingRecordsVO>() {});
    }

    /**
     * 写入破纪录汇总缓存。
     *
     * @param vo 破纪录聚合 VO 快照
     */
    public void cacheBreakingRecords(BreakingRecordsVO vo) {
        redisTemplate.opsForValue().set(BREAKING_KEY, vo, CACHE_TTL);
    }

    /**
     * 驱逐破纪录汇总缓存。
     */
    public void evictBreakingRecords() {
        redisTemplate.delete(BREAKING_KEY);
    }

    // ==================== 全量驱逐 ====================

    /**
     * 驱逐所有看板缓存（概览 / 今日赛程 / 破纪录汇总）。
     * <p>
     * 实现采用 SCAN 游标扫描 {@code dashboard:*} 模式而非 KEYS，避免在 key 数量增长时阻塞 Redis。
     * 任何写操作（运动员/学院/项目/赛程/记录变更）都可能影响看板聚合结果，
     * 因此在跨模块写路径触发或运维场景下统一调用此方法清空全部看板缓存。
     * </p>
     */
    public void evictAll() {
        ScanOptions options = ScanOptions.scanOptions()
                .match(DASHBOARD_KEY_PREFIX + "*")
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