package com.hmdp.utils;

/**
 * Redis Key Constants for UniTicket System
 * All keys follow the pattern: uniticket:{module}:{action}:{id}
 * This ensures namespace isolation and prevents key conflicts
 */
public class RedisConstants {
    
    // ==================== User Authentication ====================
    /**
     * 登录验证码缓存 Key
     * Format: uniticket:login:code:{phone}
     * TTL: 2 minutes
     */
    public static final String LOGIN_CODE_KEY = "uniticket:login:code:";
    public static final Long LOGIN_CODE_TTL = 2L;
    
    /**
     * 用户登录 Token 缓存 Key
     * Format: uniticket:login:token:{token}
     * TTL: 36000 minutes (25 days)
     */
    public static final String LOGIN_USER_KEY = "uniticket:login:token:";
    public static final Long LOGIN_USER_TTL = 36000L;

    // ==================== Cache Configuration ====================
    /**
     * 缓存空值的 TTL (防止缓存穿透)
     * TTL: 2 minutes
     */
    public static final Long CACHE_NULL_TTL = 2L;

    // ==================== Venue (场馆) Module ====================
    /**
     * 场馆信息缓存 Key (原 Shop)
     * Format: uniticket:venue:cache:{venueId}
     * TTL: 30 minutes
     */
    public static final Long CACHE_VENUE_TTL = 30L;
    public static final String CACHE_VENUE_KEY = "uniticket:venue:cache:";
    
    /**
     * 场馆更新分布式锁 Key
     * Format: uniticket:venue:lock:{venueId}
     * TTL: 10 seconds
     */
    public static final String LOCK_VENUE_KEY = "uniticket:venue:lock:";
    public static final Long LOCK_VENUE_TTL = 10L;
    
    /**
     * 场馆地理位置缓存 Key (GEO 数据结构)
     * Format: uniticket:venue:geo:{categoryId}
     */
    public static final String VENUE_GEO_KEY = "uniticket:venue:geo:";

    // ==================== Ticket (门票) Module ====================
    /**
     * 秒杀票库存缓存 Key (原 Seckill Voucher)
     * Format: uniticket:ticket:stock:{ticketId}
     * Value: Remaining stock count
     */
    public static final String TICKET_STOCK_KEY = "uniticket:ticket:stock:";
    
    /**
     * 秒杀订单分布式锁 Key
     * Format: uniticket:order:lock:{userId}:{ticketId}
     * TTL: 10 seconds
     */
    public static final String LOCK_ORDER_KEY = "uniticket:order:lock:";
    public static final Long LOCK_ORDER_TTL = 10L;

    // ==================== Social (校园社交) Module ====================
    /**
     * 校园动态点赞集合 Key (原 Blog Liked)
     * Format: uniticket:post:liked:{postId}
     * Data Structure: Set (存储点赞用户的 userId)
     */
    public static final String POST_LIKED_KEY = "uniticket:post:liked:";
    
    /**
     * 用户关注 Feed 流 Key
     * Format: uniticket:feed:{userId}
     * Data Structure: Sorted Set (score = timestamp)
     */
    public static final String FEED_KEY = "uniticket:feed:";
    
    /**
     * 用户签到记录 Key (BitMap)
     * Format: uniticket:user:sign:{userId}:{yyyyMM}
     * Example: uniticket:user:sign:1001:202412
     */
    public static final String USER_SIGN_KEY = "uniticket:user:sign:";

    // ==================== Legacy Compatibility (待删除) ====================
    /**
     * @deprecated Use CACHE_VENUE_KEY instead
     */
    @Deprecated
    public static final String CACHE_SHOP_KEY = "cache:shop:";
    
    /**
     * @deprecated Use LOCK_VENUE_KEY instead
     */
    @Deprecated
    public static final String LOCK_SHOP_KEY = "lock:shop:";
    
    /**
     * @deprecated Use LOCK_VENUE_TTL instead
     */
    @Deprecated
    public static final Long LOCK_SHOP_TTL = 10L;
    
    /**
     * @deprecated Use CACHE_VENUE_TTL instead
     */
    @Deprecated
    public static final Long CACHE_SHOP_TTL = 30L;
    
    /**
     * @deprecated Use TICKET_STOCK_KEY instead
     */
    @Deprecated
    public static final String SECKILL_STOCK_KEY = "seckill:stock:";
    
    /**
     * @deprecated Use POST_LIKED_KEY instead
     */
    @Deprecated
    public static final String BLOG_LIKED_KEY = "blog:liked:";
    
    /**
     * @deprecated Use VENUE_GEO_KEY instead
     */
    @Deprecated
    public static final String SHOP_GEO_KEY = "shop:geo:";
}
