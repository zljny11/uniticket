package com.hmdp.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.dto.Result;
import com.hmdp.entity.Venue;
import com.hmdp.mapper.VenueMapper;
import com.hmdp.service.IVenueService;
import com.hmdp.utils.RedisConstants;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

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
        //1.从redis查询缓存
        String venueJson = stringRedisTemplate.opsForValue().get(RedisConstants.CACHE_VENUE_KEY + id);
        //2.判断是否存在, 存在就直接返回
        if(StrUtil.isNotBlank(venueJson)){
            Venue venue = JSONUtil.toBean(venueJson, Venue.class);//将venueJson转换为Venue对象反序列化
            return Result.ok(venue);
        }
        //3.不存在，根据id查询数据库
        Venue venue = getById(id);
        //4.数据库不存在，返回错误
        if(venue == null){
            return Result.fail("Venue not found");
        }
        //5.数据库当中存在，写入redis
        stringRedisTemplate.opsForValue().set(RedisConstants.CACHE_VENUE_KEY + id, JSONUtil.toJsonStr(venue));
        //6.返回
        return Result.ok(venue);
    }
}
