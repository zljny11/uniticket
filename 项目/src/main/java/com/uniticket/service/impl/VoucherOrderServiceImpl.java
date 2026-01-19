package com.uniticket.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.uniticket.dto.Result;
import com.uniticket.entity.TicketOrder;
import com.uniticket.mapper.TicketOrderMapper;
import com.uniticket.service.ISeckillVoucherService;
import com.uniticket.service.IVoucherOrderService;
import com.uniticket.utils.RedisIdWorker;
import com.uniticket.utils.UserHolder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.util.concurrent.ListenableFutureCallback;
import org.springframework.beans.factory.annotation.Value;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static com.uniticket.utils.RedisConstants.TICKET_ORDER_KEY;
import static com.uniticket.utils.RedisConstants.TICKET_STOCK_KEY;

/**
 * <p>
 * TicketOrder Service Implementation - 票务订单服务实现类（异步秒杀）
 * Legacy name retained for compatibility
 * </p>
 *
 * @author UniTicket Team
 * @since 2024-12-26
 */
@Slf4j
@Service
public class VoucherOrderServiceImpl extends ServiceImpl<TicketOrderMapper, TicketOrder> implements IVoucherOrderService {

    @Resource
    private ISeckillVoucherService seckillVoucherService;

    @Resource
    private RedisIdWorker redisIdWorker;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${kafka.topic.seckill-order}")
    private String seckillOrderTopic;

    @Value("${kafka.topic.order-timeout}")
    private String orderTimeoutTopic;

    // Lua脚本
    private static final DefaultRedisScript<Long> SECKILL_SCRIPT;
    static {
        SECKILL_SCRIPT = new DefaultRedisScript<>();
        SECKILL_SCRIPT.setLocation(new ClassPathResource("mapper/seckill.lua"));
        SECKILL_SCRIPT.setResultType(Long.class);
    }


    /**
     * 抢购秒杀票（异步方式）
     */
    @Override
    public Result flashSaleTicket(Long ticketId) {
        Long userId = UserHolder.getUser().getId();
        // 1.执行lua脚本,判断是否有资格下单
        Long result = stringRedisTemplate.execute(
                SECKILL_SCRIPT,
                Collections.emptyList(),
                ticketId.toString(),
                userId.toString()
        );
        if (result == 1L) {
            return Result.fail("库存不足");
        }
        if (result == 2L) {
            return Result.fail("重复下单");
        }
        // 有购买资格
        long orderId = redisIdWorker.nextId("order");
        TicketOrder ticketOrder = new TicketOrder();
        ticketOrder.setId(orderId);    // 订单ID
        ticketOrder.setUserId(userId); // 用户ID
        ticketOrder.setVoucherId(ticketId); // 优惠券ID
        try {
            ListenableFuture<SendResult<String, Object>> future =
                    kafkaTemplate.send(seckillOrderTopic, ticketId.toString(), ticketOrder);
            future.addCallback(new ListenableFutureCallback<SendResult<String, Object>>() {
                @Override
                public void onFailure(Throwable ex) {
                    log.error("kafka sendMessage error, topic={}, data={}", seckillOrderTopic, ticketOrder, ex);
                }

                @Override
                public void onSuccess(SendResult<String, Object> result) {
                    log.info("kafka sendMessage success topic={}, data={}", seckillOrderTopic, ticketOrder);
                }
            });
        } catch (Exception e) {
            log.error("kafka sendMessage failed, topic={}, data={}", seckillOrderTopic, ticketOrder, e);
            return Result.fail("下单失败");
        }
        return Result.ok(orderId);
    }

    /**
     * 老版本创建订单（同步方式，保留用于兼容）
     */
    @Transactional
    @Override
    public Result createTicketOrder(Long ticketId) {
        Long userId = UserHolder.getUser().getId();
        // 4、一人一单校验
        int count = query().eq("user_id", userId).eq("voucher_id", ticketId).count();
        if (count > 0) {
            return Result.fail("用户已购买过该票");
        }
        // 5、扣减库存
        boolean success = seckillVoucherService.update()
                .setSql("stock = stock - 1")
                .eq("voucher_id", ticketId)
                .gt("stock", 0)
                .update();
        if (!success) {
            throw new RuntimeException("秒杀库存扣减失败");
        }
        // 6、创建订单
        TicketOrder ticketOrder = new TicketOrder();
        long orderId = redisIdWorker.nextId("order");
        ticketOrder.setId(orderId);
        ticketOrder.setUserId(userId);
        ticketOrder.setVoucherId(ticketId);
        save(ticketOrder);
        // 7、返回订单id
        return Result.ok(orderId);
    }

    @Transactional
    @Override
    public void handleVoucherOrder(TicketOrder ticketOrder) {
        if (ticketOrder == null || ticketOrder.getUserId() == null || ticketOrder.getVoucherId() == null) {
            log.warn("订单消息缺少关键字段，忽略处理");
            return;
        }
        // 幂等性保证：数据库唯一索引 (user_id, voucher_id) 防止重复订单
        // 通过捕获 DuplicateKeyException 处理 Kafka 消息重复投递场景
        try {
            boolean success = seckillVoucherService.update()
                    .setSql("stock = stock - 1")
                    .eq("voucher_id", ticketOrder.getVoucherId())
                    .gt("stock", 0)
                    .update();
            if (!success) {
                log.warn("库存不足，订单创建失败 userId={}, voucherId={}",
                        ticketOrder.getUserId(), ticketOrder.getVoucherId());
                return;
            }
            save(ticketOrder);
            log.info("订单创建成功 orderId={}, userId={}, voucherId={}",
                    ticketOrder.getId(), ticketOrder.getUserId(), ticketOrder.getVoucherId());
        } catch (org.springframework.dao.DuplicateKeyException e) {
            // 幂等性生效：Kafka 重复投递导致的唯一键冲突
            log.info("幂等性拦截：重复订单已忽略 orderId={}, userId={}, voucherId={}",
                    ticketOrder.getId(), ticketOrder.getUserId(), ticketOrder.getVoucherId());
        }
    }

    @Override
    public void closeExpiredOrders(int expireMinutes, int batchSize) {
        LocalDateTime expireTime = LocalDateTime.now().minusMinutes(expireMinutes);
        List<TicketOrder> expiredOrders = list(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<TicketOrder>()
                .eq(TicketOrder::getStatus, 1)
                .isNull(TicketOrder::getPayTime)
                .le(TicketOrder::getCreateTime, expireTime)
                .orderByAsc(TicketOrder::getCreateTime)
                .last("LIMIT " + batchSize));

        if (expiredOrders.isEmpty()) {
            return;
        }

        for (TicketOrder order : expiredOrders) {
            closeSingleOrder(order);
        }
    }

    private void closeSingleOrder(TicketOrder order) {
        int updated = getBaseMapper().update(null,
                new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<TicketOrder>()
                        .eq(TicketOrder::getId, order.getId())
                        .eq(TicketOrder::getStatus, 1)
                        .set(TicketOrder::getStatus, 4));
        if (updated <= 0) {
            return;
        }

        try {
            kafkaTemplate.send(orderTimeoutTopic, String.valueOf(order.getId()), order);
        } catch (Exception e) {
            log.error("kafka send order-timeout failed orderId={}", order.getId(), e);
        }
        log.info("order closed orderId={}, voucherId={}", order.getId(), order.getVoucherId());
    }

    @Override
    public void releaseStockForTimeoutOrder(TicketOrder order) {
        if (order == null || order.getVoucherId() == null || order.getUserId() == null) {
            log.warn("timeout order missing fields, skip release");
            return;
        }

        Long removed = stringRedisTemplate.opsForSet()
                .remove(TICKET_ORDER_KEY + order.getVoucherId(), order.getUserId());
        if (removed == null || removed <= 0) {
            log.info("skip stock release, order already handled orderId={}", order.getId());
            return;
        }

        try {
            stringRedisTemplate.opsForValue().increment(TICKET_STOCK_KEY + order.getVoucherId(), 1);
        } catch (Exception e) {
            log.warn("redis stock rollback failed orderId={}", order.getId(), e);
        }

        boolean stockRollback = seckillVoucherService.update()
                .setSql("stock = stock + 1")
                .eq("voucher_id", order.getVoucherId())
                .update();
        if (!stockRollback) {
            log.warn("db stock rollback failed orderId={}, voucherId={}", order.getId(), order.getVoucherId());
        }
    }

    @Override
    public boolean markOrderPaid(Long orderId, Integer payType, LocalDateTime payTime) {
        int updated = getBaseMapper().update(null,
                new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<TicketOrder>()
                        .eq(TicketOrder::getId, orderId)
                        .eq(TicketOrder::getStatus, 1)
                        .set(TicketOrder::getStatus, 2)
                        .set(TicketOrder::getPayType, payType)
                        .set(TicketOrder::getPayTime, payTime));
        if (updated > 0) {
            log.info("order paid success orderId={}", orderId);
            return true;
        }

        TicketOrder current = getById(orderId);
        if (current != null && Integer.valueOf(4).equals(current.getStatus())) {
            log.warn("order already closed, refund required orderId={}", orderId);
            triggerRefundForClosedOrder(current);
        }
        return false;
    }

    private void triggerRefundForClosedOrder(TicketOrder order) {
        // TODO: integrate with payment gateway refund API
        log.info("trigger refund for closed orderId={}, payType={}", order.getId(), order.getPayType());
    }
}
