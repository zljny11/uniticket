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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.util.Collections;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

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

    // Lua脚本
    private static final DefaultRedisScript<Long> SECKILL_SCRIPT;
    static {
        SECKILL_SCRIPT = new DefaultRedisScript<>();
        SECKILL_SCRIPT.setLocation(new ClassPathResource("mapper/seckill.lua"));
        SECKILL_SCRIPT.setResultType(Long.class);
    }

    // 存储订单的阻塞队列,参数为队列长度
    private BlockingQueue<TicketOrder> orderTasks = new ArrayBlockingQueue<>(1024 * 1024);

    // 执行任务的线程池, cmd+shift+U 转换大写
    private static final ExecutorService SECKILL_ORDER_EXECUTOR = Executors.newSingleThreadExecutor();

    // 任务
    private class VoucherOrderHandler implements Runnable {
        @Override
        public void run() {
            while (true) { // 并不会对CPU造成负担,因为下面有take
                // 从阻塞队列中获取订单信息，完成库存扣减和订单生成
                try {
                    // take() 获取和删除该队列的头部,如果需要则等待直到元素可用
                    TicketOrder ticketOrder = orderTasks.take();
                    handleVoucherOrder(ticketOrder);
                } catch (Exception e) {
                    log.error("处理订单异常", e);
                }
            }
        }
    }

    // 完成库存扣减和订单生成
    private void handleVoucherOrder(TicketOrder ticketOrder) {
        // 在Redis已经做了库存是否充足和一人一单的校验,能够到这里说明用户已经秒杀成功了,所以这里其实不需要加锁
        // 1.扣减库存
        boolean success = seckillVoucherService.update()
                .setSql("stock = stock - 1")
                .eq("voucher_id", ticketOrder.getVoucherId())
                .gt("stock", 0)
                .update();
        if (!success) {
            // 扣减库存失败
            log.error("库存不足");
            return;
        }
        // 2.创建订单
        save(ticketOrder);
    }

    // 当前类初始化完毕就立马执行该方法
    @PostConstruct
    private void init() {
        // 执行线程任务
        SECKILL_ORDER_EXECUTOR.submit(new VoucherOrderHandler());
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
        // 2.保存信息到阻塞队列,会有一个线程不断从当中取出信息,执行扣库存和生成订单
        TicketOrder ticketOrder = new TicketOrder();
        ticketOrder.setId(orderId);    // 订单ID
        ticketOrder.setUserId(userId); // 用户ID
        ticketOrder.setVoucherId(ticketId); // 优惠券ID
        orderTasks.add(ticketOrder);
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
}