package com.hmdp.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.dto.Result;
import com.hmdp.entity.Ticket;
import com.hmdp.mapper.TicketMapper;
import com.hmdp.entity.FlashSaleConfig;
import com.hmdp.service.ISeckillVoucherService;
import com.hmdp.service.ITicketService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.List;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
public class TicketServiceImpl extends ServiceImpl<TicketMapper, Ticket> implements ITicketService {

    @Resource
    private ISeckillVoucherService seckillVoucherService;

    @Override
    public Result queryTicketOfVenue(Long venueId) {
        // 查询门票信息
        List<Ticket> tickets = getBaseMapper().queryTicketOfVenue(venueId);
        // 返回结果
        return Result.ok(tickets);
    }

    @Override
    @Transactional
    public void addFlashSaleTicket(Ticket ticket) {
        // 保存门票
        save(ticket);
        // 保存秒杀配置
        FlashSaleConfig flashSaleConfig = new FlashSaleConfig();
        flashSaleConfig.setTicketId(ticket.getId());
        flashSaleConfig.setStock(ticket.getStock());
        flashSaleConfig.setBeginTime(ticket.getBeginTime());
        flashSaleConfig.setEndTime(ticket.getEndTime());
        seckillVoucherService.save(flashSaleConfig);
    }
}
