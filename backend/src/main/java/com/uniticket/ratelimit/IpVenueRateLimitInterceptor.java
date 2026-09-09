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
public class IpVenueRateLimitInterceptor implements HandlerInterceptor {

    @Resource
    private RateLimiterService rateLimiterService;

    @Resource
    private RateLimitResponseWriter responseWriter;

    @Value("${rate-limit.venue.window-ms:10000}")
    private int windowMs;

    @Value("${rate-limit.venue.max-requests:30}")
    private int maxRequests;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if (!"GET".equalsIgnoreCase(request.getMethod())) {
            return true;
        }
        String ip = resolveClientIp(request);
        String key = "rl:venue:ip:" + ip;
        String seqKey = key + ":seq";
        boolean allowed = rateLimiterService.allowSlidingWindow(key, seqKey, windowMs, maxRequests);
        if (!allowed) {
            log.warn("venue ip rate limit triggered ip={}", ip);
            responseWriter.writeRejected(response, "访问过于频繁，请稍后再试");
            return false;
        }
        return true;
    }

    private String resolveClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isEmpty()) {
            int commaIndex = forwarded.indexOf(',');
            return commaIndex > 0 ? forwarded.substring(0, commaIndex).trim() : forwarded.trim();
        }
        return request.getRemoteAddr();
    }
}
