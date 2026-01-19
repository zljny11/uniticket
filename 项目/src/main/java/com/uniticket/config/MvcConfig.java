package com.uniticket.config;

import com.uniticket.utils.LoginInterceptor;
import com.uniticket.utils.RefreshTokenInterceptor;
import com.uniticket.ratelimit.GlobalSeckillRateLimitInterceptor;
import com.uniticket.ratelimit.UserCouponRateLimitInterceptor;
import com.uniticket.ratelimit.IpVenueRateLimitInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import javax.annotation.Resource;
@Configuration
public class MvcConfig implements WebMvcConfigurer {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private LoginInterceptor loginInterceptor;

    @Resource
    private RefreshTokenInterceptor refreshTokenInterceptor;

    @Resource
    private GlobalSeckillRateLimitInterceptor globalSeckillRateLimitInterceptor;

    @Resource
    private UserCouponRateLimitInterceptor userCouponRateLimitInterceptor;

    @Resource
    private IpVenueRateLimitInterceptor ipVenueRateLimitInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 添加刷新token的拦截器（优先级最高）
        registry.addInterceptor(refreshTokenInterceptor).addPathPatterns("/**").order(0);
        // 全局秒杀限流
        registry.addInterceptor(globalSeckillRateLimitInterceptor)
                .addPathPatterns("/ticket-order/flash-sale/**")
                .order(1);
        // 用户领券限流
        registry.addInterceptor(userCouponRateLimitInterceptor)
                .addPathPatterns("/ticket-order/flash-sale/**")
                .order(2);
        // 场馆查询 IP 限流
        registry.addInterceptor(ipVenueRateLimitInterceptor)
                .addPathPatterns("/venue/**")
                .order(3);
        // 添加登录拦截器
        registry.addInterceptor(loginInterceptor)
                // 设置放行请求
                .excludePathPatterns(
                        "/user/code",
                        "/user/login",
                        "/campus-post/**",
                        "/blog/hot",
                        "/venue/**",
                        "/venue-category/**",
                        "/ticket/**",
                        "/upload/**",
                        "/voucher/**"
                ).order(1); // 优先级默认都是0，值越大优先级越低
    }
}
