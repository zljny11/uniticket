package com.uniticket.controller;

import com.uniticket.dto.Result;
import com.uniticket.entity.Ticket;
import com.uniticket.service.ITicketService;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;

/**
 * <p>
 * Ticket Controller - 门票前端控制器
 * Business Context: Manages campus event tickets (normal & flash sale)
 * Supports ticket creation, query, and flash sale configuration
 * </p>
 *
 * @author UniTicket Team
 * @since 2024-12-26
 */
@RestController
@RequestMapping("/ticket")  // New RESTful route
public class TicketController {

    @Resource
    private ITicketService ticketService;

    /**
     * 新增普通票
     * POST /ticket
     * 
     * @param ticket 门票信息
     * @return 门票ID
     */
    @PostMapping
    public Result addTicket(@RequestBody Ticket ticket) {
        ticketService.save(ticket);
        return Result.ok(ticket.getId());
    }

    /**
     * 新增秒杀票（包含库存、开始时间、结束时间）
     * POST /ticket/flash-sale
     * 
     * @param ticket 门票信息（包含 stock, beginTime, endTime）
     * @return 门票ID
     */
    @PostMapping("/flash-sale")
    public Result addFlashSaleTicket(@RequestBody Ticket ticket) {
        ticketService.addFlashSaleTicket(ticket);
        return Result.ok(ticket.getId());
    }

    /**
     * 查询指定场馆的所有门票列表
     * GET /ticket/venue/{venueId}
     * 
     * @param venueId 场馆ID
     * @return 门票列表
     */
    @GetMapping("/venue/{venueId}")
    public Result queryTicketOfVenue(@PathVariable("venueId") Long venueId) {
        return ticketService.queryTicketOfVenue(venueId);
    }
}
