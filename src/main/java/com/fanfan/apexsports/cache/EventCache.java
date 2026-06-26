package com.fanfan.apexsports.cache;

import com.fanfan.apexsports.entities.Event;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
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
 * 比赛项目缓存管理
 * <p>
 * 管理 {@link Event} 实体的缓存生命周期，覆盖「单个项目」与「项目列表」两类缓存。
 * 内部私有化 Redis Key 格式、TTL 策略与序列化方式，对外只暴露业务语义方法，
 * 严格遵守「Cache 层之外不得直接操作 Redis」的红线规范。
 * </p>
 *
 * <h3>设计决策</h3>
 * <ul>
 *   <li>Event 属于「低改高读」数据（学期初定好不动），TTL 设为 1 小时，平衡新鲜度与命中率</li>
 *   <li>使用 {@code RedisTemplate<String, Object>} 存储完整 Event DO（JSON 序列化，配置见 RedisConfig）</li>
 *   <li>列表驱逐采用 SCAN 而非 KEYS，避免在 key 数量增长时阻塞 Redis（生产环境最佳实践）</li>
 *   <li>任何写操作都需调用 {@link #evictAllEventLists()}，因为新增/更新/删除都可能影响任意列表查询结果</li>
 * </ul>
 *
 * @author fanfan187
 * @version v1.0.0
 * @since v1.0.0
 */
@Component
@RequiredArgsConstructor
public class EventCache {

    // ==================== Key 格式（私有化，不对外暴露） ====================

    /** 单个 Event 缓存 Key 前缀，完整格式：{@code event:id:{id}}（如 event:id:42） */
    private static final String EVENT_KEY_PREFIX = "event:id:";

    /** Event 列表缓存 Key 前缀，完整格式：{@code event:list:{filterKey}}（如 event:list:all、event:list:type1_cat2） */
    private static final String EVENT_LIST_KEY_PREFIX = "event:list:";

    // ==================== TTL 策略（私有化） ====================

    /** 单个 Event 缓存过期时间：1 小时（低改数据，宽松 TTL 提升命中率） */
    private static final Duration EVENT_TTL = Duration.ofHours(1);

    /** Event 列表缓存过期时间：1 小时（与单条一致，写操作时还会主动驱逐） */
    private static final Duration EVENT_LIST_TTL = Duration.ofHours(1);

    /** SCAN 单批次建议扫描数量（仅作 hint，Redis 不强制，默认值 100 兼顾性能与往返次数） */
    private static final int SCAN_BATCH_SIZE = 100;

    // ==================== 依赖注入 ====================

    /**
     * Redis 操作模板，Value 使用 Jackson JSON 序列化（见 {@code RedisConfig}），
     * 支持直接存储 {@link Event} 这类带 {@link java.time.LocalDateTime} 字段的 DO 对象
     */
    private final RedisTemplate<String, Object> redisTemplate;

    /**
     * 内部 ObjectMapper，用于 Redis 反序列化后的二次类型转换。
     * <p>
     * 由于 {@code RedisConfig} 的 {@code GenericJackson2JsonRedisSerializer} 未开启默认类型信息，
     * 反序列化结果是 {@code LinkedHashMap} 而非目标 POJO。这里用与 RedisConfig 一致的时间模块配置，
     * 通过 {@link ObjectMapper#convertValue(Object, Class)} 做二次转换还原为 {@link Event}。
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

    // ==================== 单个 Event 缓存 ====================

    /**
     * 查询单个 Event 缓存。
     *
     * @param id 项目 ID
     * @return 命中则返回 Event DO，未命中返回 {@code null}（由 Logic 层判空后回源 DAO）
     */
    public Event getEventById(Long id) {
        Object cached = redisTemplate.opsForValue().get(EVENT_KEY_PREFIX + id);
        if (cached == null) {
            return null;
        }
        return objectMapper.convertValue(cached, Event.class);
    }

    /**
     * 写入单个 Event 缓存。
     * <p>
     * 调用方（DAO）应保证传入的是未删除的有效 DO。
     * </p>
     *
     * @param event 待缓存的 Event DO
     */
    public void cacheEvent(Event event) {
        redisTemplate.opsForValue().set(EVENT_KEY_PREFIX + event.getId(), event, EVENT_TTL);
    }

    /**
     * 驱逐单个 Event 缓存。
     * <p>
     * 在 Event 更新或删除时调用，保证下次读取会回源到 DB。
     * </p>
     *
     * @param id 项目 ID
     */
    public void evictEvent(Long id) {
        redisTemplate.delete(EVENT_KEY_PREFIX + id);
    }

    // ==================== Event 列表缓存 ====================

    /**
     * 查询 Event 列表缓存。
     *
     * @param filterKey 过滤条件的哈希签名（由 DAO 层根据查询参数生成，Cache 不关心其语义，
     *                  只把它当作不透明字符串拼接到 Key 后缀）
     * @return 命中则返回 Event 列表，未命中返回 {@code null}
     */
    @SuppressWarnings("unchecked")
    public List<Event> getEventList(String filterKey) {
        Object cached = redisTemplate.opsForValue().get(EVENT_LIST_KEY_PREFIX + filterKey);
        if (cached == null) {
            return null;
        }
        com.fasterxml.jackson.core.type.TypeReference<List<Event>> typeRef = new com.fasterxml.jackson.core.type.TypeReference<>() {};
        return objectMapper.convertValue(cached, typeRef);
    }

    /**
     * 写入 Event 列表缓存。
     *
     * @param filterKey 过滤条件的哈希签名
     * @param events    待缓存的 Event 列表（应为不可变或调用后不再修改的快照）
     */
    public void cacheEventList(String filterKey, List<Event> events) {
        redisTemplate.opsForValue().set(EVENT_LIST_KEY_PREFIX + filterKey, events, EVENT_LIST_TTL);
    }

    /**
     * 驱逐所有 Event 列表缓存。
     * <p>
     * 任何写操作（新增/更新/删除 Event）都可能影响所有列表查询的结果集，
     * 因此在写操作时统一调用此方法清空全部列表缓存，保证一致性。
     * </p>
     * <p>
     * 实现采用 SCAN 游标扫描而非 KEYS 命令，避免在 key 数量增长时阻塞 Redis
     * （KEYS 是 O(N) 全库阻塞，SCAN 是游标式渐进扫描，生产环境最佳实践）。
     * </p>
     */
    public void evictAllEventLists() {
        ScanOptions options = ScanOptions.scanOptions()
                .match(EVENT_LIST_KEY_PREFIX + "*")
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
