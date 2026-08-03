package com.fanfan.apexsports.cache;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import com.fanfan.apexsports.entities.Score;
import com.fanfan.apexsports.vos.ScoreDetailVO;
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
 * 成绩缓存管理
 * <p>
 * 仅负责「成绩分页结果（含 total）」与「按场次聚合的成绩列表」两类缓存的生命周期管理。
 * 成绩属于「中写高读」数据（录入后多次查询、排名汇总），对分页结果与按场次聚合结果
 * 做 cache-aside，写操作（录入/修改/删除）由 {@code ScoreDAO} 触发驱逐。
 * </p>
 * <p>
 * 内部私有化 Key 格式、TTL 策略与序列化方式，对外只暴露业务语义方法，
 * 严格遵守「Cache 层之外不得直接操作缓存」红线；本类属于 Cache 层，
 * 允许操作缓存，禁止出现底层存储引擎相关字样（db/rdb/database）。
 * </p>
 *
 * <h3>设计决策</h3>
 * <ul>
 *   <li>成绩列表为「中写高读」数据，TTL 设为 30 分钟，兼顾新鲜度与命中率</li>
 *   <li>序列化方式与 {@link CompetitionCache} / {@link RecordCache} 一致：
 *       {@code RedisTemplate<String, Object>} + Jackson JSON（配置见 RedisConfig），
 *       因 {@code GenericJackson2JsonRedisSerializer} 未开启默认类型信息，
 *       反序列化结果是 {@code LinkedHashMap}，需用内部 {@link ObjectMapper#convertValue(Object, Class)}
 *       做二次转换还原为 {@link Page} 分页结果或 {@link Score} 列表</li>
 *   <li>分页结果缓存整个 {@link Page} 对象（records + total + current + size），
 *       命中后无需回源即可还原完整分页语义；因 {@code Page#getPages()} 为只读派生属性
 *       （total/size 向上取整，无 setter），序列化会产生 {@code pages} 字段，
 *       故内部 ObjectMapper 关闭 {@link DeserializationFeature#FAIL_ON_UNKNOWN_PROPERTIES}</li>
 *   <li>驱逐采用 SCAN 而非 KEYS，避免在 key 数量增长时阻塞缓存</li>
 *   <li>写操作（成绩录入/修改/删除）统一驱逐全部列表缓存与该 competitionId 维度的聚合缓存</li>
 * </ul>
 *
 * @author fanfan187
 * @version v1.0.0
 * @since v1.0.0
 */
@Component
@RequiredArgsConstructor
public class ScoreCache {

    // ==================== Key 格式（私有化，不对外暴露） ====================

    /**
     * 成绩分页结果缓存 Key 前缀，完整格式：{@code score:list:{keySuffix}}。
     * <p>
     * keySuffix 由调用方（DAO）按分页参数与过滤条件拼装，如 {@code "1_10_5_null"}
     * 表示 page=1、size=10、competitionId=5、eventId=null
     * （as_score 无 event_id 列，项目维度经连表 as_competition.event_id 过滤）。
     * </p>
     */
    private static final String LIST_KEY_PREFIX = "score:list:";

    /**
     * 按场次聚合的成绩列表缓存 Key 前缀，完整格式：{@code score:competition:{competitionId}:scores}。
     * <p>
     * 后缀 {@code :scores} 保证 evict 模式 {@code score:competition:{competitionId}:*}
     * 能精确匹配该场次维度的缓存项，避免误伤或漏删。
     * </p>
     */
    private static final String COMPETITION_KEY_PREFIX = "score:competition:";

    /** 场次维度缓存 Key 后缀，固定为 scores，便于 SCAN 模式匹配 */
    private static final String COMPETITION_KEY_SUFFIX = ":scores";

    // ==================== TTL 策略（私有化） ====================

    /** 成绩缓存过期时间：30 分钟（中写高读，兼顾新鲜度与命中率） */
    private static final Duration CACHE_TTL = Duration.ofMinutes(30);

    /** SCAN 单批次建议扫描数量（与 {@link CompetitionCache} 保持一致） */
    private static final int SCAN_BATCH_SIZE = 100;

    // ==================== 依赖注入 ====================

    /**
     * 缓存操作模板，Value 使用 Jackson JSON 序列化（见 {@code RedisConfig}），
     * 与 {@link CompetitionCache} / {@link RecordCache} 共用同一序列化配置
     */
    private final RedisTemplate<String, Object> redisTemplate;

    /**
     * 内部 ObjectMapper，用于缓存反序列化后的二次类型转换。
     * <p>
     * 由于 {@code RedisConfig} 的 {@code GenericJackson2JsonRedisSerializer} 未开启默认类型信息，
     * 反序列化结果是 {@code LinkedHashMap} 而非目标 POJO。这里用与 RedisConfig 一致的时间模块配置，
     * 通过 {@link ObjectMapper#convertValue(Object, Class)} 做二次转换还原为
     * {@link Page} 分页结果或 {@link Score} 列表。
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
        // Page#getPages() 为只读派生属性（无 setter），序列化会产生 "pages" 字段，
        // 反序列化需容忍未知字段，否则 convertValue 抛 UnrecognizedPropertyException
        om.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        return om;
    }

    // ==================== 成绩分页结果缓存 ====================

    /**
     * 查询成绩分页结果缓存。
     * <p>
     * 缓存对象为完整 {@link Page}（records + total + current + size），
     * 命中后直接还原分页语义，无需回源补 total。
     * </p>
     *
     * @param keySuffix 分页键后缀（由分页参数与过滤条件拼装）
     * @return 命中则返回分页结果，未命中返回 {@code null}（由调用方 DAO 回源查库）
     */
    public Page<ScoreDetailVO> getPage(String keySuffix) {
        Object cached = redisTemplate.opsForValue().get(LIST_KEY_PREFIX + keySuffix);
        if (cached == null) {
            return null;
        }
        return objectMapper.convertValue(cached, new TypeReference<Page<ScoreDetailVO>>() {});
    }

    /**
     * 写入成绩分页结果缓存（含 total）。
     *
     * @param keySuffix 分页键后缀
     * @param data      待缓存的分页结果（records + total + current + size）
     */
    public void cachePage(String keySuffix, Page<ScoreDetailVO> data) {
        redisTemplate.opsForValue().set(LIST_KEY_PREFIX + keySuffix, data, CACHE_TTL);
    }

    /**
     * 驱逐全部成绩分页结果缓存。
     * <p>
     * 任意成绩写操作（录入/修改/删除）都可能影响任意分页的结果，
     * 因此在写操作时统一调用此方法清空全部分页缓存，保证一致性。
     * 实现采用 SCAN 游标扫描而非 KEYS 命令，避免阻塞缓存。
     * </p>
     */
    public void evictList() {
        scanAndDelete(LIST_KEY_PREFIX + "*");
    }

    // ==================== 按场次聚合的成绩列表缓存 ====================

    /**
     * 查询指定场次的成绩列表缓存。
     *
     * @param competitionId 场次ID
     * @return 命中则返回该场次下的成绩实体列表，未命中返回 {@code null}
     */
    public List<Score> getCompetitionScores(Long competitionId) {
        Object cached = redisTemplate.opsForValue().get(buildCompetitionKey(competitionId));
        if (cached == null) {
            return null;
        }
        return objectMapper.convertValue(cached, new TypeReference<List<Score>>() {});
    }

    /**
     * 写入指定场次的成绩列表缓存。
     *
     * @param competitionId 场次ID
     * @param data          待缓存的成绩实体列表
     */
    public void cacheCompetitionScores(Long competitionId, List<Score> data) {
        redisTemplate.opsForValue().set(buildCompetitionKey(competitionId), data, CACHE_TTL);
    }

    /**
     * 驱逐指定场次维度的成绩聚合缓存。
     * <p>
     * 写操作后由 DAO 调用，保证下次按场次查询回源取最新。
     * 实现采用 SCAN 游标扫描 {@code score:competition:{competitionId}:*} 模式。
     * </p>
     *
     * @param competitionId 场次ID
     */
    public void evictCompetition(Long competitionId) {
        scanAndDelete(COMPETITION_KEY_PREFIX + competitionId + ":*");
    }

    /**
     * 驱逐全部成绩缓存（列表 + 场次聚合）。
     * <p>
     * 供跨维度写操作或运维场景调用，扫描 {@code score:*} 模式。
     * </p>
     */
    public void evictAll() {
        scanAndDelete("score:*");
    }

    // ==================== 内部工具 ====================

    /**
     * 构造场次维度缓存 Key：{@code score:competition:{competitionId}:scores}。
     */
    private String buildCompetitionKey(Long competitionId) {
        return COMPETITION_KEY_PREFIX + competitionId + COMPETITION_KEY_SUFFIX;
    }

    /**
     * 按模式 SCAN 扫描并删除所有匹配的 key。
     *
     * @param pattern Key 匹配模式（如 {@code score:list:*}）
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
