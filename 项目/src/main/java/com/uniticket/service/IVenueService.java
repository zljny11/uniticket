package com.uniticket.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.uniticket.dto.Result;
import com.uniticket.entity.Venue;

/**
 * <p>
 * 场馆服务类
 * </p>
 *
 * @author UniTicket Team
 * @since 2024-12-25
 */
public interface IVenueService extends IService<Venue> {
    Result queryById(Long id);

    Result update(Venue venue);
}
