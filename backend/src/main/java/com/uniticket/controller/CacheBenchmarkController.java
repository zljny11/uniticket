package com.uniticket.controller;

import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.uniticket.dto.Result;
import com.uniticket.entity.Ticket;
import com.uniticket.entity.Venue;
import com.uniticket.service.IVenueService;
import com.uniticket.service.ITicketService;
import com.uniticket.service.MultiLevelCacheService;
import com.uniticket.utils.CacheClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * <p>
 * 缓存性能对比测试控制器
 * 用于对比单级缓存(Redis) vs 二级缓存(Caffeine+Redis)的性能差异
 * </p>
 *
 * 测试方法：
 * 1. 使用 JMeter 或 wrk 进行压力测试
 * 2. 分别调用 /single-cache 和 /multi-cache 接口
 * 3. 对比响应时间、吞吐量、Redis 流量
 *
 * @author UniTicket Team
 * @since 2024-12-31
 */
@Slf4j
@RestController
@RequestMapping("/cache/benchmark")
public class CacheBenchmarkController {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private CacheClient cacheClient;

    @Resource
    private MultiLevelCacheService multiLevelCacheService;

    @Resource
    private IVenueService venueService;

    @Resource
    private ITicketService ticketService;

    // 测试用的 venue ID
    private static final Long TEST_VENUE_ID = 1L;

    // 缓存 Key 前缀
    private static final String CACHE_VENUE_KEY = "uniticket:venue:cache:";
    private static final Long CACHE_VENUE_TTL = 30L;

    /**
     * 单级缓存查询（仅 Redis）
     * 用于对比测试
     */
    @GetMapping("/single-cache")
    public Result singleCacheQuery(@RequestParam(defaultValue = "1") Long id) {
        long startTime = System.nanoTime();

        // 使用 CacheClient 查询（只有 Redis 缓存）
        Venue venue = cacheClient.queryWithPassThrough(
                CACHE_VENUE_KEY,
                id,
                Venue.class,
                venueService::getById,
                CACHE_VENUE_TTL,
                TimeUnit.MINUTES
        );

        long endTime = System.nanoTime();
        long duration = (endTime - startTime) / 1_000; // 转换为微秒

        if (venue == null) {
            return Result.fail("Venue not found");
        }

        Map<String, Object> result = new HashMap<>();
        result.put("data", venue);
        result.put("cacheType", "Single-Cache(Redis)");
        result.put("durationUs", duration);

        return Result.ok(result);
    }

    /**
     * 二级缓存查询（Caffeine + Redis）
     * 用于对比测试
     */
    @GetMapping("/multi-cache")
    public Result multiCacheQuery(@RequestParam(defaultValue = "1") Long id) {
        long startTime = System.nanoTime();

        // 使用 MultiLevelCacheService 查询（有 Caffeine 本地缓存）
        String redisKey = CACHE_VENUE_KEY + id;
        Venue venue = multiLevelCacheService.get(
                "venue",                  // 本地缓存名称
                redisKey,                 // Redis 完整 Key
                id,                       // 本地缓存 Key
                Venue.class,              // 返回类型
                venueService::getById,    // 数据库查询函数
                id,                       // 查询参数
                CACHE_VENUE_TTL,          // Redis 缓存 TTL
                TimeUnit.MINUTES          // 时间单位
        );

        long endTime = System.nanoTime();
        long duration = (endTime - startTime) / 1_000; // 转换为微秒

        if (venue == null) {
            return Result.fail("Venue not found");
        }

        Map<String, Object> result = new HashMap<>();
        result.put("data", venue);
        result.put("cacheType", "Multi-Cache(Caffeine+Redis)");
        result.put("durationUs", duration);

        return Result.ok(result);
    }

    /**
     * 无缓存查询（直接查数据库）
     * 用于对比测试
     */
    @GetMapping("/no-cache")
    public Result noCacheQuery(@RequestParam(defaultValue = "1") Long id) {
        long startTime = System.nanoTime();

        // 直接查数据库，不使用缓存
        Venue venue = venueService.getById(id);

        long endTime = System.nanoTime();
        long duration = (endTime - startTime) / 1_000; // 转换为微秒

        if (venue == null) {
            return Result.fail("Venue not found");
        }

        Map<String, Object> result = new HashMap<>();
        result.put("data", venue);
        result.put("cacheType", "No-Cache(DB)");
        result.put("durationUs", duration);

        return Result.ok(result);
    }

    /**
     * 批量预热测试数据
     * 用于在测试前预热缓存
     */
    @PostMapping("/warmup")
    public Result warmup(@RequestParam(defaultValue = "10") int count) {
        for (long i = 1; i <= count; i++) {
            // 预热到二级缓存
            Venue venue = venueService.getById(i);
            if (venue != null) {
                multiLevelCacheService.warmUp("venue", i, venue);
            }
        }
        log.info("[CacheBenchmark] 预热完成 - count: {}", count);
        return Result.ok("Warmed up " + count + " venues");
    }

    /**
     * 重置所有测试缓存
     */
    @PostMapping("/reset")
    public Result reset() {
        multiLevelCacheService.clearLocal("venue");
        multiLevelCacheService.clearLocal("ticket");

        // 清除 Redis 缓存
        for (long i = 1; i <= 10; i++) {
            stringRedisTemplate.delete(CACHE_VENUE_KEY + i);
        }

        log.info("[CacheBenchmark] 已重置所有测试缓存");
        return Result.ok("All caches reset");
    }

    /**
     * 获取测试统计摘要
     */
    @GetMapping("/test-stats")
    public Result getTestStats() {
        Map<String, Object> stats = new HashMap<>();

        try {
            com.github.benmanes.caffeine.cache.stats.CacheStats venueStats =
                    multiLevelCacheService.getLocalCacheStats("venue");

            Map<String, Object> venueCacheStats = new HashMap<>();
            venueCacheStats.put("hitRate", venueStats.hitRate() * 100);
            venueCacheStats.put("hitCount", venueStats.hitCount());
            venueCacheStats.put("missCount", venueStats.missCount());
            venueCacheStats.put("requestCount", venueStats.requestCount());
            stats.put("venueCache", venueCacheStats);
        } catch (Exception e) {
            stats.put("venueCache", "No data yet");
        }

        return Result.ok(stats);
    }
}
