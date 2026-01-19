package com.uniticket.job;

import com.uniticket.service.IVoucherOrderService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

@Slf4j
@Component
public class OrderAutoCloseJob {

    @Resource
    private IVoucherOrderService voucherOrderService;

    @Value("${order.payment-timeout-minutes:15}")
    private int orderExpireMinutes;

    @Value("${order.timeout-scan-batch-size:200}")
    private int scanBatchSize;

    @Scheduled(cron = "0 */1 * * * ?")
    public void autoCloseExpiredOrders() {
        log.info("start auto close unpaid orders");
        try {
            voucherOrderService.closeExpiredOrders(orderExpireMinutes, scanBatchSize);
        } catch (Exception e) {
            log.error("auto close unpaid orders failed", e);
        }
    }
}
