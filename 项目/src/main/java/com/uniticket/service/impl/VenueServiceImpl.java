package com.uniticket.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.uniticket.dto.Result;
import com.uniticket.entity.Venue;
import com.uniticket.mapper.VenueMapper;
import com.uniticket.service.IVenueService;
import com.uniticket.utils.RedisConstants;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.concurrent.TimeUnit;

import static com.uniticket.utils.RedisConstants.CACHE_NULL_TTL;
import static com.uniticket.utils.RedisConstants.CACHE_VENUE_TTL;

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

    @Override
    public Result queryById(Long id) {
        String key = RedisConstants.CACHE_VENUE_KEY + id;
        //1.从redis查询缓存
        String venueJson = stringRedisTemplate.opsForValue().get(key);
        //2.判断是否存在, 存在就直接返回
        if(StrUtil.isNotBlank(venueJson)){
            Venue venue = JSONUtil.toBean(venueJson, Venue.class);//将venueJson转换为Venue对象反序列化
            return Result.ok(venue);
        }
        //判断venueJson是否为空字符串(缓存穿透保护)
        if(venueJson != null&& venueJson.isEmpty()){
            return Result.fail("Venue not found");
        }
        //3.不存在，根据id查询数据库
        Venue venue = getById(id);
        //4.数据库不存在，返回错误
        if(venue == null){
            //将空值写入redis
            stringRedisTemplate.opsForValue().set(key, "", CACHE_NULL_TTL, TimeUnit.MINUTES);
            return Result.fail("Venue not found");
        }
        //5.数据库当中存在，写入redis
        stringRedisTemplate.opsForValue().set(RedisConstants.CACHE_VENUE_KEY + id, JSONUtil.toJsonStr(venue),
                CACHE_VENUE_TTL, TimeUnit.MINUTES);
        //6.返回
        return Result.ok(venue);
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
