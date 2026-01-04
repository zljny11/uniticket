package com.uniticket.service;

import com.uniticket.dto.Result;
import com.uniticket.entity.TicketOrder;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * <p>
 * TicketOrder Service - 票务订单服务接口
 * Legacy name retained for compatibility
 * </p>
 *
 * @author UniTicket Team
 * @since 2024-12-26
 */
public interface IVoucherOrderService extends IService<TicketOrder> {

    Result flashSaleTicket(Long ticketId);
}
