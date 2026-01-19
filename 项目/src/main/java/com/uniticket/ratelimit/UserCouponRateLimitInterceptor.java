package com.uniticket.ratelimit;

import com.uniticket.utils.UserHolder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Slf4j
@Component
public class UserCouponRateLimitInterceptor implements HandlerInterceptor {

    @Resource
    private RateLimiterService rateLimiterService;

    @Resource
    private RateLimitResponseWriter responseWriter;

    @Value("${rate-limit.coupon.window-ms:60000}")
    private int windowMs;

    @Value("${rate-limit.coupon.max-requests:5}")
    private int maxRequests;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        Long userId = UserHolder.getUser() != null ? UserHolder.getUser().getId() : null;
        String keySuffix = userId != null ? String.valueOf(userId) : request.getRemoteAddr();
        String key = "rl:coupon:user:" + keySuffix;
        String seqKey = key + ":seq";
        boolean allowed = rateLimiterService.allowSlidingWindow(key, seqKey, windowMs, maxRequests);
        if (!allowed) {
            log.warn("coupon user rate limit triggered userKey={}", keySuffix);
            responseWriter.writeRejected(response, "操作过于频繁，请稍后再试");
            return false;
        }
        return true;
    }
}
