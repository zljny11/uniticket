package com.hmdp.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * <p>
 * VenueCategory Entity - 场馆/活动类型实体
 * Maps to legacy tb_shop_type table
 * Business Context: Categories like "Concert Hall", "Sports Venue", 
 * "Lecture Theater", "Exhibition Space"
 * </p>
 *
 * @author UniTicket Team
 * @since 2024-12-25
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("tb_shop_type")  // Maps to legacy shop_type table
public class VenueCategory implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键 - Category ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 类型名称 (e.g., "Concert", "Sports", "Academic Lecture", "Workshop")
     */
    private String name;

    /**
     * 图标 URL
     */
    private String icon;

    /**
     * 排序权重 (用于前端展示顺序)
     */
    private Integer sort;

    /**
     * 创建时间
     */
    @JsonIgnore
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @JsonIgnore
    private LocalDateTime updateTime;
}
