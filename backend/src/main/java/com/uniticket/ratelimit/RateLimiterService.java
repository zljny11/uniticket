package com.uniticket.ratelimit;

import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;

@Service
public class RateLimiterService {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    private DefaultRedisScript<Long> tokenBucketScript;
    private DefaultRedisScript<Long> slidingWindowScript;

    @PostConstruct
    public void initScripts() {
        tokenBucketScript = new DefaultRedisScript<>();
        tokenBucketScript.setLocation(new ClassPathResource("mapper/rate_limit_token_bucket.lua"));
        tokenBucketScript.setResultType(Long.class);

        slidingWindowScript = new DefaultRedisScript<>();
        slidingWindowScript.setLocation(new ClassPathResource("mapper/rate_limit_sliding_window.lua"));
        slidingWindowScript.setResultType(Long.class);
    }

    public boolean allowTokenBucket(String key, int capacity, int refillPerSecond) {
        long nowMs = Instant.now().toEpochMilli();
        Long allowed = stringRedisTemplate.execute(
                tokenBucketScript,
                Collections.singletonList(key),
                String.valueOf(capacity),
                String.valueOf(refillPerSecond),
                String.valueOf(nowMs),
                "1"
        );
        return allowed != null && allowed == 1L;
    }

    public boolean allowSlidingWindow(String key, String seqKey, int windowMs, int maxRequests) {
        long nowMs = Instant.now().toEpochMilli();
        Long allowed = stringRedisTemplate.execute(
                slidingWindowScript,
                Arrays.asList(key, seqKey),
                String.valueOf(windowMs),
                String.valueOf(maxRequests),
                String.valueOf(nowMs)
        );
        return allowed != null && allowed == 1L;
    }
}
