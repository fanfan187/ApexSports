package com.fanfan.apexsports.cache;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import com.fanfan.apexsports.constants.CompetitionConstants;
import com.fanfan.apexsports.vos.ScheduleBookletVO;
import lombok.RequiredArgsConstructor;
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

/**
 * 赛程秩序册聚合缓存管理
 * <p>
 * 仅负责「秩序册聚合结果」的缓存生命周期管理。按翻翻决策，赛程详情与赛程列表
 * 均不缓存（详情/列表加缓存收益有限且一致性维护成本高），本类只缓存按日期维度
 * 聚合出的秩序册数据。
 * </p>
 * <p>
 * 内部私有化 Redis Key 格式、TTL 策略与序列化方式，对外只暴露业务语义方法，
 * 严格遵守「Cache 层之外不得直接操作 Redis」「Cache 不得出现 db/rdb 字样」的红线规范。
 * </p>
 *
 * <h3>设计决策</h3>
 * <ul>
 *   <li>秩序册为「高改高读」聚合数据（赛程/参赛队/裁判都可能变动），TTL 设为 60s，
 *       以新鲜度优先，避免脏数据影响现场秩序册打印</li>
 *   <li>序列化方式与 {@link EventCache} 一致：{@code RedisTemplate<String, Object>} + Jackson JSON
 *       （配置见 RedisConfig），支持带 {@link LocalDateTime} 字段的 VO 直接存储</li>
 *   <li>列表驱逐采用 SCAN 而非 KEYS，避免在 key 数量增长时阻塞 Redis</li>
 *   <li>任何写操作（赛程/参赛队/裁判变更）都需调用 {@link #evictAllBooklets()}，
 *       因为秩序册聚合了多源数据，任意源变更都可能影响任意日期的聚合结果</li>
 * </ul>
 *
 * @author fanfan187
 * @version v1.0.0
 * @since v1.0.0
 */
@Component
@RequiredArgsConstructor
public class CompetitionCache {

    // ==================== Key 格式（私有化，不对外暴露） ====================

    /**
     * 秩序册缓存 Key 前缀，完整格式：{@code competition:booklet:{dateKey}}。
     * <p>
     * dateKey 取值：
     * <ul>
     *   <li>{@code "all"} —— 全部日期的秩序册聚合</li>
     *   <li>{@code "2026-05-15"} —— 指定日期的秩序册聚合</li>
     * </ul>
     */
    private static final String BOOKLET_KEY_PREFIX = "competition:booklet:";

    // ==================== TTL 策略（私有化） ====================

    /**
     * 秩序册缓存过期时间：60 秒。
     * <p>
     * 取自 {@link CompetitionConstants#BOOKLET_CACHE_TTL}，避免魔法数字散落，
     * 统一在常量类维护 TTL 策略。
     * </p>
     */
    private static final Duration BOOKLET_TTL = Duration.ofSeconds(CompetitionConstants.BOOKLET_CACHE_TTL);

    /** SCAN 单批次建议扫描数量（与 EventCache 保持一致，兼顾性能与往返次数） */
    private static final int SCAN_BATCH_SIZE = 100;

    // ==================== 依赖注入 ====================

    /**
     * Redis 操作模板，Value 使用 Jackson JSON 序列化（见 {@code RedisConfig}），
     * 与 {@link EventCache} 共用同一序列化配置，支持直接存储带时间字段的 VO 对象
     */
    private final RedisTemplate<String, Object> redisTemplate;

    /**
     * 内部 ObjectMapper，用于 Redis 反序列化后的二次类型转换。
     * <p>
     * 由于 {@code RedisConfig} 的 {@code GenericJackson2JsonRedisSerializer} 未开启默认类型信息，
     * 反序列化结果是 {@code LinkedHashMap} 而非目标 POJO。这里用与 RedisConfig 一致的时间模块配置，
     * 通过 {@link ObjectMapper#convertValue(Object, Class)} 做二次转换还原为 {@link ScheduleBookletVO} 列表。
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

    // ==================== 秩序册聚合缓存 ====================

    /**
     * 查询秩序册聚合缓存。
     *
     * @param dateKey 日期键，{@code "all"} 表示全部日期聚合，或形如 {@code "2026-05-15"} 的指定日期
     * @return 命中则返回秩序册列表，未命中返回 {@code null}（由 Logic 层判空后回源 DAO）
     */
    public List<ScheduleBookletVO> getBooklet(String dateKey) {
        Object cached = redisTemplate.opsForValue().get(BOOKLET_KEY_PREFIX + dateKey);
        if (cached == null) {
            return null;
        }
        return objectMapper.convertValue(cached, new TypeReference<List<ScheduleBookletVO>>() {});
    }

    /**
     * 写入秩序册聚合缓存。
     * <p>
     * 调用方（DAO）应保证传入的是聚合完成后的快照数据，且调用后不再修改该 List。
     * </p>
     *
     * @param dateKey 日期键，与 {@link #getBooklet(String)} 的 key 语义一致
     * @param data    待缓存的秩序册列表
     */
    public void cacheBooklet(String dateKey, List<ScheduleBookletVO> data) {
        redisTemplate.opsForValue().set(BOOKLET_KEY_PREFIX + dateKey, data, BOOKLET_TTL);
    }

    /**
     * 驱逐所有秩序册缓存。
     * <p>
     * 任何写操作（赛程新增/更新/删除、参赛队变更、裁判指派等）都可能影响任意日期的秩序册聚合结果，
     * 因此在写操作时统一调用此方法清空全部秩序册缓存，保证一致性。
     * </p>
     * <p>
     * 实现采用 SCAN 游标扫描而非 KEYS 命令，避免在 key 数量增长时阻塞 Redis
     * （KEYS 是 O(N) 全库阻塞，SCAN 是游标式渐进扫描，生产环境最佳实践）。
     * </p>
     */
    public void evictAllBooklets() {
        ScanOptions options = ScanOptions.scanOptions()
                .match(BOOKLET_KEY_PREFIX + "*")
                .count(SCAN_BATCH_SIZE)
                .build();

        // 通过 RedisCallback 拿到原生 RedisConnection，使用游标式扫描收集所有匹配 key
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
