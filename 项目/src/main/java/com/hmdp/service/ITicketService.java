package com.hmdp.service;

import com.hmdp.dto.Result;
import com.hmdp.entity.Ticket;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * <p>
 *  服务类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
public interface ITicketService extends IService<Ticket> {

    Result queryTicketOfVenue(Long venueId);

    void addFlashSaleTicket(Ticket ticket);
}
