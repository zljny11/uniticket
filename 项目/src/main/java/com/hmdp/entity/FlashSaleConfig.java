package com.hmdp.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * <p>
 * FlashSaleConfig Entity - 秒杀活动配置实体
 * Maps to legacy tb_seckill_voucher table
 * Business Context: High-concurrency flash sale configuration for hot events
 * (e.g., Popular concerts, celebrity lectures, sports finals)
 * Stores stock, time window, and sale rules
 * </p>
 *
 * @author UniTicket Team
 * @since 2024-12-26
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("tb_seckill_voucher")  // Maps to legacy seckill_voucher table
public class FlashSaleConfig implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 关联的票务ID (与 Ticket 表一对一关系)
     * This is the PRIMARY KEY and also a FOREIGN KEY to tb_voucher
     */
    @TableId(value = "voucher_id", type = IdType.INPUT)
    private Long ticketId;  // Business name: ticketId, DB column: voucher_id

    /**
     * 库存数量
     * Critical field for high-concurrency scenarios
     * Will be cached in Redis (key: uniticket:ticket:stock:{ticketId})
     */
    private Integer stock;

    /**
     * 秒杀开始时间
     * Format: 2024-12-25 18:00:00
     */
    private LocalDateTime beginTime;

    /**
     * 秒杀结束时间
     * Format: 2024-12-25 20:00:00
     */
    private LocalDateTime endTime;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间 (库存变更时会更新)
     */
    private LocalDateTime updateTime;
}
