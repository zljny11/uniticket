package com.uniticket.controller;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.uniticket.dto.Result;
import com.uniticket.entity.Venue;
import com.uniticket.service.IVenueService;
import com.uniticket.utils.SystemConstants;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;

/**
 * <p>
 * Venue Controller - 场馆/主办方前端控制器
 * Business Context: Manages campus venue information for event hosting
 * (NUS Grand Hall, NTU Sports Center, Student Union venues, etc.)
 * </p>
 *
 * @author UniTicket Team
 * @since 2024-12-25
 */
@RestController
@RequestMapping("/venue")  // Changed from /shop to /venue
public class VenueController {

    @Resource
    public IVenueService venueService;

    /**
     * 根据ID查询场馆信息
     * GET /venue/{id}
     * 
     * @param id 场馆ID
     * @return 场馆详情数据
     */
    @GetMapping("/{id}")
    public Result queryVenueById(@PathVariable("id") Long id) {
        return venueService.queryById(id);
    }

    /**
     * 新增场馆信息
     * POST /venue
     * 
     * @param venue 场馆数据
     * @return 场馆ID
     */
    @PostMapping
    public Result saveVenue(@RequestBody Venue venue) {
        // 写入数据库
        venueService.save(venue);
        // 返回场馆ID
        return Result.ok(venue.getId());
    }

    /**
     * 更新场馆信息
     * PUT /venue
     * 
     * @param venue 场馆数据
     * @return 无
     */
    @PutMapping
    public Result updateVenue(@RequestBody Venue venue) {
        // 写入数据库
        return venueService.update(venue);
    }

    /**
     * 根据场馆类型分页查询场馆信息
     * GET /venue/of/type?typeId=1&current=1
     * 
     * @param typeId 场馆类型ID (关联 VenueCategory)
     * @param current 页码
     * @return 场馆列表
     */
    @GetMapping("/of/type")
    public Result queryVenueByType(
            @RequestParam("typeId") Integer typeId,
            @RequestParam(value = "current", defaultValue = "1") Integer current
    ) {
        // 根据类型分页查询
        Page<Venue> page = venueService.query()
                .eq("type_id", typeId)
                .page(new Page<>(current, SystemConstants.DEFAULT_PAGE_SIZE));
        // 返回数据
        return Result.ok(page.getRecords());
    }

    /**
     * 根据场馆名称关键字分页查询场馆信息
     * GET /venue/of/name?name=Grand%20Hall&current=1
     * 
     * @param name 场馆名称关键字
     * @param current 页码
     * @return 场馆列表
     */
    @GetMapping("/of/name")
    public Result queryVenueByName(
            @RequestParam(value = "name", required = false) String name,
            @RequestParam(value = "current", defaultValue = "1") Integer current
    ) {
        // 根据名称模糊查询
        Page<Venue> page = venueService.query()
                .like(StrUtil.isNotBlank(name), "name", name)
                .page(new Page<>(current, SystemConstants.MAX_PAGE_SIZE));
        // 返回数据
        return Result.ok(page.getRecords());
    }
}
