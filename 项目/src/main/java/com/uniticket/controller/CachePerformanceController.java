package com.uniticket.controller;

import com.github.benmanes.caffeine.cache.stats.CacheStats;
import com.uniticket.dto.Result;
import com.uniticket.service.MultiLevelCacheService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.Map;

/**
 * <p>
 * 缓存性能监控控制器
 * 用于测试和展示二级缓存的性能指标
 * </p>
 *
 * @author UniTicket Team
 * @since 2024-12-31
 */
@Slf4j
@RestController
@RequestMapping("/cache/performance")
public class CachePerformanceController {

    @Resource
    private MultiLevelCacheService multiLevelCacheService;

    /**
     * 获取所有缓存的统计信息
     * 返回格式：
     * {
     *   "venue": {
     *     "hitRate": 85.5,
     *     "hitCount": 8523,
     *     "missCount": 1447,
     *     "loadCount": 1447,
     *     "evictionCount": 120,
     *     "requestCount": 9970
     *   },
     *   "ticket": { ... }
     * }
     */
    @GetMapping("/stats")
    public Result getCacheStats() {
        Map<String, Object> allStats = new HashMap<>();

        // 获取各个缓存模块的统计信息
        String[] cacheNames = {"venue", "ticket", "user", "seckill"};

        for (String cacheName : cacheNames) {
            try {
                CacheStats stats = multiLevelCacheService.getLocalCacheStats(cacheName);
                Map<String, Object> statMap = new HashMap<>();
                statMap.put("hitRate", stats.hitRate() * 100);
                statMap.put("hitCount", stats.hitCount());
                statMap.put("missCount", stats.missCount());
                statMap.put("loadCount", stats.loadCount());
                statMap.put("evictionCount", stats.evictionCount());
                statMap.put("requestCount", stats.requestCount());
                statMap.put("averageLoadPenalty", stats.averageLoadPenalty() / 1_000_000.0); // 转换为毫秒

                allStats.put(cacheName, statMap);
            } catch (Exception e) {
                // 缓存未初始化或无数据
                Map<String, Object> emptyStat = new HashMap<>();
                emptyStat.put("hitRate", 0.0);
                emptyStat.put("hitCount", 0L);
                emptyStat.put("missCount", 0L);
                emptyStat.put("loadCount", 0L);
                allStats.put(cacheName, emptyStat);
            }
        }

        return Result.ok(allStats);
    }

    /**
     * 重置指定缓存的所有数据
     */
    @PostMapping("/reset/{cacheName}")
    public Result resetCache(@PathVariable String cacheName) {
        multiLevelCacheService.clearLocal(cacheName);
        log.info("[CachePerformance] 已重置缓存 - cacheName: {}", cacheName);
        return Result.ok("Cache reset: " + cacheName);
    }

    /**
     * 获取缓存性能摘要
     * 用于快速查看总体缓存效果
     */
    @GetMapping("/summary")
    public Result getCacheSummary() {
        Map<String, Object> summary = new HashMap<>();

        String[] cacheNames = {"venue", "ticket"};
        long totalHits = 0;
        long totalMisses = 0;
        long totalRequests = 0;

        for (String cacheName : cacheNames) {
            try {
                CacheStats stats = multiLevelCacheService.getLocalCacheStats(cacheName);
                totalHits += stats.hitCount();
                totalMisses += stats.missCount();
                totalRequests += stats.requestCount();
            } catch (Exception ignored) {
            }
        }

        double overallHitRate = totalRequests > 0 ? (double) totalHits / totalRequests * 100 : 0.0;

        summary.put("totalHits", totalHits);
        summary.put("totalMisses", totalMisses);
        summary.put("totalRequests", totalRequests);
        summary.put("overallHitRate", overallHitRate);
        summary.put("redisTrafficSaved", totalHits); // L1 命中次数 = 节省的 Redis 请求数

        return Result.ok(summary);
    }

    /**
     * 打印所有缓存统计到日志
     */
    @PostMapping("/print")
    public Result printStats() {
        multiLevelCacheService.printAllCacheStats();
        return Result.ok("Stats printed to logs");
    }
}
