package com.hmdp.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.hmdp.dto.Result;
import com.hmdp.entity.Venue;

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
}
