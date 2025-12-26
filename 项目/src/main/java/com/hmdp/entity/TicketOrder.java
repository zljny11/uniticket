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
 * TicketOrder Entity - 票务订单实体
 * Maps to legacy tb_voucher_order table
 * Business Context: Student ticket booking records for campus events
 * Supports payment, verification, cancellation, and refund workflows
 * </p>
 *
 * @author UniTicket Team
 * @since 2024-12-26
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("tb_voucher_order")  // Maps to legacy voucher_order table
public class TicketOrder implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 订单ID (主键)
     * Generated using distributed ID generator (e.g., Snowflake)
     */
    @TableId(value = "id", type = IdType.INPUT)
    private Long id;

    /**
     * 下单用户ID (关联 User 表)
     */
    private Long userId;

    /**
     * 购买的票务ID (关联 Ticket 表)
     * DB column name: voucher_id (for legacy compatibility)
     */
    private Long voucherId;  // Keep DB column name, represents ticketId in business

    /**
     * 支付方式
     * 1: 余额支付 (University Wallet)
     * 2: 支付宝 (Alipay)
     * 3: 微信支付 (WeChat Pay)
     * 4: PayNow (Singapore local payment)
     */
    private Integer payType;

    /**
     * 订单状态
     * 1: 未支付 (Pending Payment)
     * 2: 已支付 (Paid)
     * 3: 已核销 (Used/Verified at venue)
     * 4: 已取消 (Cancelled)
     * 5: 退款中 (Refunding)
     * 6: 已退款 (Refunded)
     */
    private Integer status;

    /**
     * 下单时间
     */
    private LocalDateTime createTime;

    /**
     * 支付时间
     */
    private LocalDateTime payTime;

    /**
     * 核销时间 (票务在场馆入口扫码核销的时间)
     */
    private LocalDateTime useTime;

    /**
     * 退款时间
     */
    private LocalDateTime refundTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;
}
