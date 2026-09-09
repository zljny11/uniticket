package com.uniticket.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 二级缓存配置类
 * L1: Caffeine 本地缓存
 * L2: Redis 分布式缓存
 */
@Slf4j
@Configuration
@EnableCaching
public class MultiLevelCacheConfig {

    // ==================== Caffeine 本地缓存配置 ====================

    /**
     * Caffeine 本地缓存管理器
     * 本地缓存特性：
     * - 基于内存，速度极快
     * - 每个 JVM 实例独立存储
     * - 适合存储热 Key 数据
     */
    @Bean("caffeineCacheManager")
    public CacheManager caffeineCacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager();

        cacheManager.setCaffeine(Caffeine.newBuilder()
                // 初始容量
                .initialCapacity(100)
                // 最大缓存数量
                .maximumSize(1000)
                // 写入后过期时间（本地缓存时间较短，避免数据不一致）
                .expireAfterWrite(5, TimeUnit.MINUTES)
                // 开启统计
                .recordStats()
        );

        log.info("[MultiLevelCache] Caffeine本地缓存管理器初始化完成");
        return cacheManager;
    }

    // ==================== Redis 分布式缓存配置 ====================

    /**
     * Redis 缓存管理器
     * 分布式缓存特性：
     * - 跨实例共享
     * - 数据一致性好
     * - 支持持久化
     */
    @Bean("redisCacheManager")
    @Primary
    public CacheManager redisCacheManager(RedisConnectionFactory connectionFactory) {
        // 配置序列化
        RedisSerializationContext.SerializationPair<Object> jsonSerializer =
                RedisSerializationContext.SerializationPair.fromSerializer(
                        new GenericJackson2JsonRedisSerializer());

        // 默认缓存配置
        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(30))
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(
                        new StringRedisSerializer()))
                .serializeValuesWith(jsonSerializer)
                .disableCachingNullValues();

        // 针对不同业务模块的缓存配置
        Map<String, RedisCacheConfiguration> cacheConfigurations = new HashMap<>();

        // 场馆信息缓存 - 30分钟
        cacheConfigurations.put("venue",
                defaultConfig.entryTtl(Duration.ofMinutes(30)));

        // 门票信息缓存 - 10分钟（需要更高实时性）
        cacheConfigurations.put("ticket",
                defaultConfig.entryTtl(Duration.ofMinutes(10)));

        // 用户信息缓存 - 1小时
        cacheConfigurations.put("user",
                defaultConfig.entryTtl(Duration.ofHours(1)));

        // 秒杀配置缓存 - 5分钟（高实时性要求）
        cacheConfigurations.put("seckill",
                defaultConfig.entryTtl(Duration.ofMinutes(5)));

        RedisCacheManager cacheManager = RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(defaultConfig)
                .withInitialCacheConfigurations(cacheConfigurations)
                .transactionAware()
                .build();

        log.info("[MultiLevelCache] Redis分布式缓存管理器初始化完成");
        return cacheManager;
    }
}