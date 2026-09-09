package com.uniticket.service;

import cn.hutool.json.JSONUtil;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.stats.CacheStats;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

/**
 * 二级缓存服务
 * L1: Caffeine 本地缓存
 * L2: Redis 分布式缓存
 *
 * 缓存策略：
 * 1. 先从本地缓存获取数据
 * 2. 本地缓存未命中，从 Redis 获取
 * 3. Redis 未命中，从数据库获取
 * 4. 更新 Redis -> 更新本地缓存 -> 返回数据
 */
@Slf4j
@Service
public class MultiLevelCacheService {

    private final StringRedisTemplate stringRedisTemplate;

    // Caffeine 本地缓存容器
    private final ConcurrentHashMap<String, Cache<Object, Object>> localCacheMap = new ConcurrentHashMap<>();

    public MultiLevelCacheService(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    // ==================== 核心查询方法 ====================

    /**
     * 二级缓存查询
     *
     * @param cacheName   缓存名称（用于隔离不同业务模块的本地缓存）
     * @param key         Redis 完整 Key
     * @param localKey    本地缓存 Key
     * @param type        返回数据类型
     * @param dbFallback  数据库查询函数
     * @param redisTTL    Redis 缓存过期时间
     * @param timeUnit    Redis 缓存时间单位
     * @param <T>         返回数据泛型
     * @param <ID>        ID 泛型
     * @return 缓存数据或数据库数据
     */
    public <T, ID> T get(String cacheName, String key, Object localKey, Class<T> type,
                         Function<ID, T> dbFallback, ID id, Long redisTTL, TimeUnit timeUnit) {
        // 1. 查询本地缓存 (L1)
        T data = getFromLocalCache(cacheName, localKey, type);
        if (data != null) {
            log.debug("[MultiLevelCache] 本地缓存命中 - cacheName: {}, key: {}", cacheName, localKey);
            return data;
        }

        // 2. 本地缓存未命中，查询 Redis (L2)
        data = getFromRedis(key, type);
        if (data != null) {
            log.debug("[MultiLevelCache] Redis缓存命中 - key: {}", key);
            // 更新本地缓存
            putToLocalCache(cacheName, localKey, data);
            return data;
        }

        // 3. Redis 缓存未命中，查询数据库
        log.debug("[MultiLevelCache] 缓存全部未命中，查询数据库 - key: {}", key);
        data = dbFallback.apply(id);

        if (Objects.isNull(data)) {
            // 缓存空值防止缓存穿透
            setNullValue(key);
            return null;
        }

        // 4. 更新 Redis 缓存
        putToRedis(key, data, redisTTL, timeUnit);

        // 5. 更新本地缓存
        putToLocalCache(cacheName, localKey, data);

        return data;
    }

    /**
     * 批量获取（适用于多 Key 查询场景）
     */
    public <T> T getBatch(String cacheName, String keyPattern, Object localKey,
                          Class<T> type, Function<String, T> dbFallback,
                          Long redisTTL, TimeUnit timeUnit) {
        // 1. 查询本地缓存
        T data = getFromLocalCache(cacheName, localKey, type);
        if (data != null) {
            return data;
        }

        // 2. 查询 Redis
        data = getFromRedis(keyPattern, type);
        if (data != null) {
            putToLocalCache(cacheName, localKey, data);
            return data;
        }

        // 3. 查询数据库
        data = dbFallback.apply(keyPattern);
        if (Objects.isNull(data)) {
            return null;
        }

        // 4. 更新缓存
        putToRedis(keyPattern, data, redisTTL, timeUnit);
        putToLocalCache(cacheName, localKey, data);

        return data;
    }

    // ==================== 本地缓存操作 (L1) ====================

    /**
     * 从本地缓存获取数据
     */
    @SuppressWarnings("unchecked")
    private <T> T getFromLocalCache(String cacheName, Object key, Class<T> type) {
        Cache<Object, Object> cache = getLocalCache(cacheName);
        Object value = cache.getIfPresent(key);
        return value != null ? (T) value : null;
    }

    /**
     * 写入本地缓存
     */
    private void putToLocalCache(String cacheName, Object key, Object value) {
        Cache<Object, Object> cache = getLocalCache(cacheName);
        cache.put(key, value);
        log.debug("[MultiLevelCache] 本地缓存已更新 - cacheName: {}, key: {}", cacheName, key);
    }

    /**
     * 删除本地缓存
     */
    public void evictLocal(String cacheName, Object key) {
        Cache<Object, Object> cache = getLocalCache(cacheName);
        cache.invalidate(key);
        log.debug("[MultiLevelCache] 本地缓存已删除 - cacheName: {}, key: {}", cacheName, key);
    }

    /**
     * 清空指定缓存名称的所有本地缓存
     */
    public void clearLocal(String cacheName) {
        Cache<Object, Object> cache = getLocalCache(cacheName);
        cache.invalidateAll();
        log.info("[MultiLevelCache] 本地缓存已清空 - cacheName: {}", cacheName);
    }

    /**
     * 获取或创建本地缓存实例
     */
    private Cache<Object, Object> getLocalCache(String cacheName) {
        return localCacheMap.computeIfAbsent(cacheName, name ->
                Caffeine.newBuilder()
                        .initialCapacity(50)
                        .maximumSize(500)
                        // 写入 5 分钟后过期（本地缓存时间较短）
                        .expireAfterWrite(5, TimeUnit.MINUTES)
                        .recordStats()
                        .build()
        );
    }

    // ==================== Redis 缓存操作 (L2) ====================

    /**
     * 从 Redis 获取数据
     */
    private <T> T getFromRedis(String key, Class<T> type) {
        String json = stringRedisTemplate.opsForValue().get(key);
        if (json == null || json.isEmpty()) {
            return null;
        }
        // 空值判断（防止缓存穿透）
        if (json.equals("")) {
            return null;
        }
        return JSONUtil.toBean(json, type);
    }

    /**
     * 写入 Redis 缓存
     */
    private void putToRedis(String key, Object value, Long timeout, TimeUnit unit) {
        String json = JSONUtil.toJsonStr(value);
        stringRedisTemplate.opsForValue().set(key, json, timeout, unit);
        log.debug("[MultiLevelCache] Redis缓存已更新 - key: {}, ttl: {} {}", key, timeout, unit);
    }

    /**
     * 删除 Redis 缓存
     */
    public void evictRedis(String key) {
        stringRedisTemplate.delete(key);
        log.debug("[MultiLevelCache] Redis缓存已删除 - key: {}", key);
    }

    /**
     * 缓存空值（防止缓存穿透）
     */
    private void setNullValue(String key) {
        stringRedisTemplate.opsForValue().set(key, "", 2, TimeUnit.MINUTES);
        log.debug("[MultiLevelCache] 空值已缓存 - key: {}", key);
    }

    // ==================== 缓存统计 ====================

    /**
     * 获取本地缓存统计信息
     */
    public CacheStats getLocalCacheStats(String cacheName) {
        Cache<Object, Object> cache = getLocalCache(cacheName);
        return cache.stats();
    }

    /**
     * 打印所有缓存统计信息
     */
    public void printAllCacheStats() {
        localCacheMap.forEach((cacheName, cache) -> {
            CacheStats stats = cache.stats();
            log.info("[MultiLevelCache] 缓存统计 - name: {}, 命中率: {:.2f}%, 命中次数: {}, 未命中次数: {}, 加载次数: {}",
                    cacheName,
                    stats.hitRate() * 100,
                    stats.hitCount(),
                    stats.missCount(),
                    stats.loadCount()
            );
        });
    }

    // ==================== 缓存预热 ====================

    /**
     * 手动预热本地缓存
     *
     * @param cacheName 缓存名称
     * @param key       本地缓存 Key
     * @param value     预热数据
     */
    public void warmUp(String cacheName, Object key, Object value) {
        putToLocalCache(cacheName, key, value);
        log.info("[MultiLevelCache] 缓存预热完成 - cacheName: {}, key: {}", cacheName, key);
    }
}