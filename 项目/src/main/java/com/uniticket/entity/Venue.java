package com.uniticket.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * <p>
 * Venue Entity - 校园活动场馆/主办方实体
 * Maps to legacy tb_shop table
 * Business Context: Represents campus venues (e.g., Grand Hall, Sports Center) 
 * or student unions hosting events in Singapore universities
 * </p>
 *
 * @author UniTicket Team
 * @since 2024-12-25
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("tb_shop")  // Maps to legacy shop table
public class Venue implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键 - Venue ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 场馆名称 (e.g., "NUS Grand Hall", "NTU Sports Center")
     */
    private String name;

    /**
     * 场馆类型ID (关联 VenueCategory)
     */
    private Long typeId;

    /**
     * 场馆图片，多个图片以','隔开
     */
    private String images;

    /**
     * 校区/区域 (e.g., "Kent Ridge Campus", "Jurong West")
     */
    private String area;

    /**
     * 详细地址
     */
    private String address;

    /**
     * 经度 (Longitude for map display)
     */
    private Double x;

    /**
     * 纬度 (Latitude for map display)
     */
    private Double y;

    /**
     * 平均票价 (SGD, in cents - e.g., 1500 = $15.00)
     */
    private Long avgPrice;

    /**
     * 已售票数 (Total tickets sold)
     */
    private Integer sold;

    /**
     * 评论数量 (Number of reviews/comments)
     */
    private Integer comments;

    /**
     * 评分 (1~5分，乘10保存，避免小数 - e.g., 45 = 4.5 stars)
     */
    private Integer score;

    /**
     * 开放时间 (e.g., "09:00-22:00")
     */
    private String openHours;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;

    /**
     * 距离当前用户的距离 (km, 非数据库字段，用于地理位置排序)
     */
    @TableField(exist = false)
    private Double distance;
}
