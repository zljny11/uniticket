package com.hmdp.entity;

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
 * CampusPost Entity - 校园动态/帖子实体
 * Maps to legacy tb_blog table
 * Business Context: Social posts where students share event experiences,
 * trade tickets, discuss campus activities, or review venues
 * (Similar to Instagram posts or event reviews)
 * </p>
 *
 * @author UniTicket Team
 * @since 2024-12-26
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("tb_blog")  // Maps to legacy blog table
public class CampusPost implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键 - Post ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 关联的场馆ID (可选)
     * 用于标记这是关于哪个场馆/活动的帖子
     * DB column name: shop_id (for legacy compatibility)
     */
    private Long shopId;  // Business: venueId, DB column: shop_id

    /**
     * 发帖用户ID
     */
    private Long userId;

    /**
     * 发帖用户头像 (非数据库字段，从 User 表关联查询)
     */
    @TableField(exist = false)
    private String icon;

    /**
     * 发帖用户昵称 (非数据库字段，从 User 表关联查询)
     */
    @TableField(exist = false)
    private String name;

    /**
     * 当前用户是否已点赞 (非数据库字段，运行时判断)
     */
    @TableField(exist = false)
    private Boolean isLike;

    /**
     * 帖子标题
     * Examples: "NUS Career Fair Review", "Selling 2 Concert Tickets"
     */
    private String title;

    /**
     * 帖子图片，最多9张，多张以","隔开
     * Format: "img1.jpg,img2.jpg,img3.jpg"
     */
    private String images;

    /**
     * 帖子正文内容
     * Examples: Event reviews, ticket trading info, venue recommendations
     */
    private String content;

    /**
     * 点赞数量
     * Cached in Redis: uniticket:post:liked:{postId} (Set)
     */
    private Integer liked;

    /**
     * 评论数量
     */
    private Integer comments;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;
}
