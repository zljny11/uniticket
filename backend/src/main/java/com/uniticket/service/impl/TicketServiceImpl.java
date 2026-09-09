package com.uniticket.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.uniticket.dto.Result;
import com.uniticket.entity.Ticket;
import com.uniticket.entity.FlashSaleConfig;
import com.uniticket.mapper.TicketMapper;
import com.uniticket.service.ISeckillVoucherService;
import com.uniticket.service.ITicketService;
import com.uniticket.service.MultiLevelCacheService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.redis.core.StringRedisTemplate;

import javax.annotation.Resource;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static com.uniticket.utils.RedisConstants.*;

/**
 * <p>
 * 门票服务实现类
 * 使用多级缓存：L1(Caffeine) + L2(Redis)
 * 注意：秒杀库存操作仍直接使用 Redis 以保证强一致性
 * </p>
 *
 * @author UniTicket Team
 * @since 2021-12-22
 */
@Slf4j
@Service
public class TicketServiceImpl extends ServiceImpl<TicketMapper, Ticket> implements ITicketService {

    @Resource
    private ISeckillVoucherService seckillVoucherService;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private MultiLevelCacheService multiLevelCacheService;

    /**
     * 定义门票缓存 Key 前缀
     * Format: uniticket:ticket:cache:{ticketId}
     */
    private static final String CACHE_TICKET_KEY = "uniticket:ticket:cache:";

    @Override
    public Result queryTicketOfVenue(Long venueId) {
        // 查询门票信息（列表查询暂不使用本地缓存）
        List<Ticket> tickets = getBaseMapper().queryTicketOfVenue(venueId);
        return Result.ok(tickets);
    }

    @Override
    public Result queryTicketById(Long ticketId) {
        // 使用多级缓存查询单个门票详情
        String redisKey = CACHE_TICKET_KEY + ticketId;
        Ticket ticket = multiLevelCacheService.get(
                LOCAL_CACHE_TICKET,        // 本地缓存名称
                redisKey,                  // Redis 完整 Key
                ticketId,                  // 本地缓存 Key
                Ticket.class,              // 返回类型
                this::getById,             // 数据库查询函数
                ticketId,                  // 查询参数
                REDIS_CACHE_TICKET_TTL,    // Redis 缓存 TTL (10分钟)
                TimeUnit.MINUTES           // 时间单位
        );
        if (ticket == null) {
            return Result.fail("Ticket not found");
        }
        return Result.ok(ticket);
    }

    @Override
    @Transactional
    public void addFlashSaleTicket(Ticket ticket) {
        // 1. 保存门票
        save(ticket);
        // 2. 保存秒杀配置
        FlashSaleConfig flashSaleConfig = new FlashSaleConfig();
        flashSaleConfig.setTicketId(ticket.getId());
        flashSaleConfig.setStock(ticket.getStock());
        flashSaleConfig.setBeginTime(ticket.getBeginTime());
        flashSaleConfig.setEndTime(ticket.getEndTime());
        seckillVoucherService.save(flashSaleConfig);
        // 3. 保存秒杀库存到 Redis（直接操作 Redis 以保证强一致性）
        stringRedisTemplate.opsForValue().set(TICKET_STOCK_KEY + ticket.getId(), ticket.getStock().toString());
        log.info("添加秒杀门票成功 - ticketId: {}, stock: {}", ticket.getId(), ticket.getStock());
    }

    @Override
    @Transactional
    public Result updateTicket(Ticket ticket) {
        Long ticketId = ticket.getId();
        if (ticketId == null) {
            return Result.fail("Invalid ticket ID");
        }
        // 1. 更新数据库
        updateById(ticket);
        // 2. 删除多级缓存
        String redisKey = CACHE_TICKET_KEY + ticketId;
        multiLevelCacheService.evictLocal(LOCAL_CACHE_TICKET, ticketId);  // 删除本地缓存 L1
        multiLevelCacheService.evictRedis(redisKey);                       // 删除 Redis 缓存 L2
        log.info("更新门票信息成功 - ticketId: {}", ticketId);
        return Result.ok();
    }
}
