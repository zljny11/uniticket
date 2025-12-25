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
 * Ticket Entity - 活动门票实体
 * Maps to legacy tb_voucher table
 * Business Context: Represents admission tickets for campus events
 * (Concerts, Lectures, Sports matches, Workshops, etc.)
 * </p>
 *
 * @author UniTicket Team
 * @since 2024-12-25
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("tb_voucher")  // Maps to legacy voucher table
public class Ticket implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键 - Ticket ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 场馆/主办方ID (Foreign key to Venue)
     */
    private Long shopId;  // Keep DB column name for compatibility

    /**
     * 活动标题 (e.g., "NUS Career Fair 2024", "Christmas Concert")
     */
    private String title;

    /**
     * 活动副标题/简介
     */
    private String subTitle;

    /**
     * 购票规则 (e.g., "仅限在校学生", "需携带学生证")
     */
    private String rules;

    /**
     * 票价 (SGD, in cents - e.g., 500 = $5.00)
     */
    private Long payValue;

    /**
     * 原价 (用于展示折扣，in cents)
     */
    private Long actualValue;

    /**
     * 票券类型 (0: 普通票, 1: 秒杀票/Flash Sale)
     */
    private Integer type;

    /**
     * 票券状态 (1: 可用, 2: 已下架)
     */
    private Integer status;

    /**
     * 库存数量 (仅秒杀票使用，非数据库字段，关联 FlashSaleConfig)
     */
    @TableField(exist = false)
    private Integer stock;

    /**
     * 秒杀开始时间 (仅秒杀票使用，非数据库字段)
     */
    @TableField(exist = false)
    private LocalDateTime beginTime;

    /**
     * 秒杀结束时间 (仅秒杀票使用，非数据库字段)
     */
    @TableField(exist = false)
    private LocalDateTime endTime;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;
}
