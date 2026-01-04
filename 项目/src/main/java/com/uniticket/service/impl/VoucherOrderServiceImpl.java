package com.uniticket.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.uniticket.dto.Result;
import com.uniticket.entity.FlashSaleConfig;
import com.uniticket.entity.TicketOrder;
import com.uniticket.mapper.TicketOrderMapper;
import com.uniticket.service.ISeckillVoucherService;
import com.uniticket.service.IVoucherOrderService;
import com.uniticket.utils.RedisIdWorker;
import com.uniticket.utils.UserHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.time.LocalDateTime;

/**
 * <p>
 * TicketOrder Service Implementation - 票务订单服务实现类
 * Legacy name retained for compatibility
 * </p>
 *
 * @author UniTicket Team
 * @since 2024-12-26
 */
@Service
public class VoucherOrderServiceImpl extends ServiceImpl<TicketOrderMapper, TicketOrder> implements IVoucherOrderService {

    @Resource
    private ISeckillVoucherService seckillVoucherService;

    @Resource
    private RedisIdWorker redisIdWorker;

    /**
     * 抢购秒杀票
     */
    @Transactional
    @Override
    public Result flashSaleTicket(Long ticketId) {
        // 1、查询秒杀配置
        FlashSaleConfig flashSaleConfig = seckillVoucherService.getById(ticketId);
        if (flashSaleConfig == null) {
            return Result.fail("抢票配置不存在");
        }
        // 2、校验秒杀时间窗口
        if (flashSaleConfig.getBeginTime().isAfter(LocalDateTime.now())) {
            return Result.fail("抢票尚未开始");
        }
        if (flashSaleConfig.getEndTime().isBefore(LocalDateTime.now())) {
            return Result.fail("抢票已结束");
        }
        // 3、判断库存是否充足
        if (flashSaleConfig.getStock() < 1) {
            return Result.fail("抢票已售罄");
        }
        // 4、扣减库存
        boolean success = seckillVoucherService.update()
                .setSql("stock = stock -1")
                .eq("voucher_id", ticketId)
                .gt("stock", 0)
                .update();
        if (!success) {
            throw new RuntimeException("秒杀库存扣减失败");
        }
        // 5、创建订单
        TicketOrder ticketOrder = new TicketOrder();
        long orderId = redisIdWorker.nextId("order");
        ticketOrder.setId(orderId);
        ticketOrder.setUserId(UserHolder.getUser().getId());
        ticketOrder.setVoucherId(ticketId);
        save(ticketOrder);
        // 6、返回订单id
        return Result.ok(orderId);
    }
}
