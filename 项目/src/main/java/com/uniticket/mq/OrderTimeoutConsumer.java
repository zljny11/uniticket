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
public class OrderTimeoutConsumer {

    @Resource
    private IVoucherOrderService voucherOrderService;

    @KafkaListener(topics = "${kafka.topic.order-timeout}", groupId = "order-timeout-group")
    public void processMessage(ConsumerRecord<String, TicketOrder> record, Acknowledgment ack) {
        try {
            TicketOrder order = record.value();
            voucherOrderService.releaseStockForTimeoutOrder(order);
            ack.acknowledge();
        } catch (Exception e) {
            log.error("order-timeout consume failed: topic={}, offset={}, reason={}",
                    record.topic(), record.offset(), e.getMessage(), e);
        }
    }
}
