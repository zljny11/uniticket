package com.uniticket.service;

import com.uniticket.dto.Result;
import com.uniticket.entity.Ticket;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * <p>
 * 门票服务类
 * 支持多级缓存：L1(Caffeine) + L2(Redis)
 * </p>
 *
 * @author UniTicket Team
 * @since 2021-12-22
 */
public interface ITicketService extends IService<Ticket> {

    /**
     * 查询场馆的所有门票
     */
    Result queryTicketOfVenue(Long venueId);

    /**
     * 根据ID查询门票详情（使用多级缓存）
     */
    Result queryTicketById(Long ticketId);

    /**
     * 添加秒杀门票
     */
    void addFlashSaleTicket(Ticket ticket);

    /**
     * 更新门票信息
     */
    Result updateTicket(Ticket ticket);
}
