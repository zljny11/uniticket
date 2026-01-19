package com.uniticket.ratelimit;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Slf4j
@Component
public class GlobalSeckillRateLimitInterceptor implements HandlerInterceptor {

    @Resource
    private RateLimiterService rateLimiterService;

    @Resource
    private RateLimitResponseWriter responseWriter;

    @Value("${rate-limit.seckill.capacity:200}")
    private int capacity;

    @Value("${rate-limit.seckill.refill-per-second:200}")
    private int refillPerSecond;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        boolean allowed = rateLimiterService.allowTokenBucket("rl:seckill:global", capacity, refillPerSecond);
        if (!allowed) {
            log.warn("seckill global rate limit triggered");
            responseWriter.writeRejected(response, "系统繁忙，请稍后再试");
            return false;
        }
        return true;
    }
}
