package com.fanfan.apexsports.cache;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateDeserializer;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateSerializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import com.fanfan.apexsports.entities.Record;
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
 * 校记录最优缓存管理
 * <p>
 * 仅负责「指定 eventId + recordType 维度下的最优校记录」缓存生命周期管理。
 * 缓存的是单条 {@link Record}（status=1 有效、按 score 方向取最优），供成绩模块
 * 内部的 {@code RecordDAO} 做 cache-aside 查询。
 * </p>
 * <p>
 * 内部私有化 Redis Key 格式、TTL 策略与序列化方式，对外只暴露业务语义方法，
 * 严格遵守「Cache 层之外不得直接操作缓存」红线；本类属于 Cache 层，
 * 允许操作缓存，禁止出现底层存储引擎相关字样。
 * </p>
 *
 * <h3>设计决策</h3>
 * <ul>
 *   <li>校记录属于「低写高读」数据（创记录频率低、查询频率高），TTL 设为 30 分钟，
 *       兼顾新鲜度与命中率</li>
 *   <li>序列化方式与 {@link CompetitionCache} 一致：{@code RedisTemplate<String, Object>} + Jackson JSON
 *       （配置见 RedisConfig），但因 {@code GenericJackson2JsonRedisSerializer} 未开启默认类型信息，
 *       反序列化结果是 {@code LinkedHashMap}，需用内部 {@link ObjectMapper#convertValue(Object, Class)}
 *       做二次转换还原为 {@link Record}</li>
 *   <li>驱逐采用 SCAN 而非 KEYS，避免在 key 数量增长时阻塞 Redis</li>
 *   <li>Key 后缀统一加 {@code :best}，使 evict 模式 {@code record:best:{eventId}:{recordType}:*}
 *       能精确匹配该维度下的最优记录，避免误伤或漏删</li>
 * </ul>
 *
 * @author fanfan187
 * @version v1.0.0
 * @since v1.0.0
 */
@Component
@RequiredArgsConstructor
public class RecordCache {

    // ==================== Key 格式（私有化，不对外暴露） ====================

    /**
     * 校记录最优缓存 Key 前缀，完整格式：{@code record:best:{eventId}:{recordType}:best}。
     */
    private static final String BEST_KEY_PREFIX = "record:best:";

    /** Key 后缀，固定为 best，便于 evict 模式匹配 */
    private static final String BEST_KEY_SUFFIX = ":best";

    // ==================== TTL 策略（私有化） ====================

    /** 校记录缓存过期时间：30 分钟（低写高读，兼顾新鲜度与命中率） */
    private static final Duration BEST_TTL = Duration.ofMinutes(30);

    /** SCAN 单批次建议扫描数量（与 {@link CompetitionCache} 保持一致） */
    private static final int SCAN_BATCH_SIZE = 100;

    // ==================== 依赖注入 ====================

    /**
     * Redis 操作模板，Value 使用 Jackson JSON 序列化（见 {@code RedisConfig}），
     * 与 {@link CompetitionCache} 共用同一序列化配置
     */
    private final RedisTemplate<String, Object> redisTemplate;

    /**
     * 内部 ObjectMapper，用于 Redis 反序列化后的二次类型转换。
     * <p>
     * 由于 {@code RedisConfig} 的 {@code GenericJackson2JsonRedisSerializer} 未开启默认类型信息，
     * 反序列化结果是 {@code LinkedHashMap} 而非目标 POJO。这里用与 RedisConfig 一致的时间模块配置，
     * 通过 {@link ObjectMapper#convertValue(Object, Class)} 做二次转换还原为 {@link Record}。
     * 同时注册 {@link LocalDate}（recordDate 字段）与 {@link LocalDateTime}（createdAt/updatedAt）序列化器。
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

    // ==================== 校记录最优缓存 ====================

    /**
     * 查询指定 eventId + recordType 维度的最优校记录缓存。
     *
     * @param eventId    项目ID
     * @param recordType 记录类型（1-校记录，2-市记录，3-省记录）
     * @return 命中则返回最优记录实体，未命中返回 {@code null}（由调用方 DAO 回源查库）
     */
    public Record get(Long eventId, Integer recordType) {
        Object cached = redisTemplate.opsForValue().get(buildBestKey(eventId, recordType));
        if (cached == null) {
            return null;
        }
        return objectMapper.convertValue(cached, new TypeReference<Record>() {});
    }

    /**
     * 写入指定 eventId + recordType 维度的最优校记录缓存。
     *
     * @param eventId    项目ID
     * @param recordType 记录类型
     * @param record     待缓存的最优记录实体
     */
    public void set(Long eventId, Integer recordType, Record record) {
        redisTemplate.opsForValue().set(buildBestKey(eventId, recordType), record, BEST_TTL);
    }

    /**
     * 驱逐指定 eventId + recordType 维度的最优校记录缓存。
     * <p>
     * 写操作（新增记录、作废记录）后由 DAO 调用，保证下次查询回源取最新最优。
     * 实现采用 SCAN 游标扫描 {@code record:best:{eventId}:{recordType}:*} 模式，
     * 避免阻塞 Redis。
     * </p>
     *
     * @param eventId    项目ID
     * @param recordType 记录类型
     */
    public void evict(Long eventId, Integer recordType) {
        String pattern = BEST_KEY_PREFIX + eventId + ":" + recordType + ":*";
        scanAndDelete(pattern);
    }

    /**
     * 驱逐所有校记录最优缓存。
     * <p>
     * 供跨维度写操作或运维场景调用，扫描 {@code record:best:*} 模式。
     * </p>
     */
    public void evictAll() {
        scanAndDelete(BEST_KEY_PREFIX + "*");
    }

    // ==================== 内部工具 ====================

    /**
     * 构造最优记录缓存 Key：{@code record:best:{eventId}:{recordType}:best}。
     */
    private String buildBestKey(Long eventId, Integer recordType) {
        return BEST_KEY_PREFIX + eventId + ":" + recordType + BEST_KEY_SUFFIX;
    }

    /**
     * 按模式 SCAN 扫描并删除所有匹配的 key。
     *
     * @param pattern Key 匹配模式（如 {@code record:best:1:1:*}）
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
