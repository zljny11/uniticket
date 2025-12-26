package com.hmdp.controller;

import com.hmdp.dto.Result;
import com.hmdp.entity.VenueCategory;
import com.hmdp.service.IVenueCategoryService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.List;

/**
 * <p>
 * VenueCategory Controller - 场馆/活动分类前端控制器
 * Business Context: Manages event categories like "Concert", "Sports", 
 * "Lecture", "Workshop" for campus ticketing system
 * </p>
 *
 * @author UniTicket Team
 * @since 2024-12-26
 */
@RestController
@RequestMapping("/venue-category")  // New RESTful route
public class VenueCategoryController {
    
    @Resource
    private IVenueCategoryService venueCategoryService;

    /**
     * 查询所有场馆分类列表（按排序权重升序）
     * GET /venue-category/list
     * 
     * @return 分类列表
     */
    @GetMapping("/list")
    public Result queryTypeList() {
        List<VenueCategory> typeList = venueCategoryService
                .query()
                .orderByAsc("sort")
                .list();
        return Result.ok(typeList);
    }
}
