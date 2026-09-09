package com.uniticket.mq;

import com.uniticket.entity.TicketOrder;
import com.uniticket.service.IVoucherOrderService;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

@Slf4j
@Component
public class SeckillVoucherConsumer {

    @Resource
    private IVoucherOrderService voucherOrderService;

    @KafkaListener(topics = "${kafka.topic.seckill-order}")
    public void processMessage(ConsumerRecord<String, TicketOrder> record, Acknowledgment ack) {
        try {
            TicketOrder order = record.value();
            voucherOrderService.handleVoucherOrder(order);
            ack.acknowledge(); // 仅业务成功时提交
        } catch (Exception e) {
            log.error("消费异常: topic={}, offset={}, 原因={}",
                    record.topic(), record.offset(), e.getMessage(), e);
            // 不提交！等待重试或进入死信队列
        }
    }
}
