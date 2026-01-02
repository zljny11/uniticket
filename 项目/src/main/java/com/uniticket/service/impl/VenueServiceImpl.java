package com.uniticket.service.impl;

import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.uniticket.dto.Result;
import com.uniticket.entity.Venue;
import com.uniticket.mapper.VenueMapper;
import com.uniticket.service.IVenueService;
import com.uniticket.utils.CacheClient;
import com.uniticket.utils.RedisConstants;
import com.uniticket.utils.RedisData;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

import static com.uniticket.utils.RedisConstants.*;

/**
 * <p>
 * 场馆服务实现类
 * Business Context: Provides CRUD for campus venues/organizers in Singapore universities
 * </p>
 *
 * @author UniTicket Team
 * @since 2024-12-25
 */
@Service
public class VenueServiceImpl extends ServiceImpl<VenueMapper, Venue> implements IVenueService {
    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private CacheClient cacheClient;

    @Override
    public Result queryById(Long id) {
        //防缓存穿透
        Venue venue = cacheClient.queryWithPassThrough(
                CACHE_VENUE_KEY,
                id,
                Venue.class,
                this::getById,
                CACHE_VENUE_TTL,
                TimeUnit.MINUTES
        );
        if (venue == null) {
            return Result.fail("Venue not found");
        }
        //6.返回
        return Result.ok(venue);
    }

    /*
    // 旧实现：互斥锁解决缓存击穿（已弃用，保留备查）
    public Venue queryWithMutex(Long id) {
        String key = CACHE_VENUE_KEY + id;
        // 1.从Redis中查询店铺数据
        String venueJson = stringRedisTemplate.opsForValue().get(key);

        Venue venue = null;
        // 2.判断缓存是否命中 isNotBlank对于空字符串也是false
        if (StrUtil.isNotBlank(venueJson)) {
            // 3.缓存命中，直接返回店铺数据
            return JSONUtil.toBean(venueJson, Venue.class);
        }
        // 判断缓存中查询的数据是否是空字符串
        if (venueJson != null){
            return null;
        }
        // 4.实现缓存重建
        String lockKey = LOCK_VENUE_KEY + id;
        try {
            boolean isLock = tryLock(lockKey);
            if (!isLock) {
                // 获取锁失败，已有线程在重建缓存，则休眠重试
                Thread.sleep(50);
                return queryWithMutex(id);
            }
            // 获取锁成功，根据id查询数据库
            venue = this.getById(id);
            // 模拟重建的延时
            Thread.sleep(200);
            // 5. 判断数据库是否存在店铺数据
            if (Objects.isNull(venue)) {
                // 数据库中不存在，缓存空值（解决缓存穿透）
                stringRedisTemplate.opsForValue().set(key, "", CACHE_NULL_TTL, TimeUnit.MINUTES);
                return null;
            }
            // 6. 数据库中存在，重建缓存，并返回店铺数据
            stringRedisTemplate.opsForValue().set(key, JSONUtil.toJsonStr(venue),
                    CACHE_VENUE_TTL, TimeUnit.MINUTES);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } finally {
            // 7. 释放互斥锁
            unLock(lockKey);
        }
        return venue;
    }

    // 旧实现：缓存穿透（已弃用，保留备查）
    public Venue queryWithPassThrough(Long id) {
        String key = RedisConstants.CACHE_VENUE_KEY + id;
        //1.从redis查询缓存
        String venueJson = stringRedisTemplate.opsForValue().get(key);
        //2.判断是否存在, 存在就直接返回
        if(StrUtil.isNotBlank(venueJson)){
            //将venueJson转换为Venue对象反序列化
            return JSONUtil.toBean(venueJson, Venue.class);
        }
        //判断venueJson是否为空字符串(缓存穿透保护)
        if(venueJson != null && venueJson.isEmpty()){
            return null;
        }
        //3.不存在，根据id查询数据库
        Venue venue = getById(id);
        //4.数据库不存在，返回错误
        if(venue == null){
            //将空值写入redis
            stringRedisTemplate.opsForValue().set(key, "", CACHE_NULL_TTL, TimeUnit.MINUTES);
            return null;
        }
        //5.数据库当中存在，写入redis
        stringRedisTemplate.opsForValue().set(RedisConstants.CACHE_VENUE_KEY + id, JSONUtil.toJsonStr(venue),
                CACHE_VENUE_TTL, TimeUnit.MINUTES);
        return venue;
    }

    private boolean tryLock(String key) {
        Boolean flag = stringRedisTemplate.opsForValue().setIfAbsent(key, "1", 10, TimeUnit.SECONDS);
        return BooleanUtil.isTrue(flag);
    }

    private void unLock(String key) {
        stringRedisTemplate.delete(key);
    }

    // 旧实现：逻辑过期解决缓存击穿（已弃用，保留备查）
    public Venue queryWithLogicExpireOld(Long id) {
        String key = CACHE_VENUE_KEY + id;
        // 1、从Redis中查询店铺数据
        String shopJson = stringRedisTemplate.opsForValue().get(key);
        // 2、判断缓存是否命中
        if (StrUtil.isBlank(shopJson)) {
            // 3.未命中,直接返回
            return null;
        }
        // 4.命中, 判断过期时间, 反序列化
        RedisData redisData = JSONUtil.toBean(shopJson, RedisData.class);
        JSONObject data = (JSONObject) redisData.getData();
        Venue venue = JSONUtil.toBean(data, Venue.class);
        LocalDateTime expireTime = redisData.getExpireTime();
        // 5.判断是否过期
        if (expireTime.isAfter(LocalDateTime.now())) {
            // 5.1.未过期,直接返回店铺信息
            return venue;
        }
        // 5.2.已过期,需要缓存重建
        // 6.1. 获取互斥锁
        String lockKey = LOCK_VENUE_KEY + id;
        boolean isLock = tryLock(lockKey);
        // 6.2. 获取锁成功,开启独立线程，实现缓存重建
        if (isLock) {
            CACHE_REBUILD_EXECUTOR.submit(() -> {
                try {
                    this.saveVenue2Redis(id, 20L);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                } finally {
                    unLock(lockKey);
                }
            });
        }
        // 6.3. 返回过期的店铺信息
        return venue;
    }
    */

    // 逻辑过期解决缓存击穿
    @Override
    public Venue queryWithLogicExpire(Long id) {
        return cacheClient.queryWithLogicExpire(
                CACHE_VENUE_KEY,
                LOCK_VENUE_KEY,
                id,
                Venue.class,
                this::getById,
                CACHE_VENUE_TTL,
                TimeUnit.MINUTES
        );
    }


    /**
     * 预热：将数据保存到缓存中
     * 重建缓存
     */
    @Override
    public void saveVenue2Redis(Long id,Long expireSeconds) {
        //1.查询
        Venue venue = getById(id);
        //2.封装逻辑过期时间
        RedisData redisData = new RedisData();
        redisData.setData(venue);
        redisData.setExpireTime(LocalDateTime.now().plusSeconds(expireSeconds));
        //3.写入redis
        stringRedisTemplate.opsForValue().set(CACHE_VENUE_KEY+id,JSONUtil.toJsonStr(redisData));


    }
    @Override
    public Result update(Venue venue) {
        Long id = venue.getId();
        if (id == null) {
            return Result.fail("Invalid venue ID");
        }
        //1.更新数据库
        updateById(venue);
        //2.删除缓存
        stringRedisTemplate.delete(RedisConstants.CACHE_VENUE_KEY + id);
        return Result.ok();

    }
}
