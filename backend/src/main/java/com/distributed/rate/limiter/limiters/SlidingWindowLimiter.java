package com.distributed.rate.limiter.limiters;

import com.distributed.rate.limiter.models.RateLimitResult;
import com.distributed.rate.limiter.models.RateLimitRule;
import lombok.extern.log4j.Log4j2;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

@Service("slidingWindow")
@Log4j2
@SuppressWarnings({"rawtypes", "unchecked"})
public class SlidingWindowLimiter implements RateLimiter
{
    private final RedisTemplate<String, Object> redisTemplate;
    private final DefaultRedisScript<List> defaultRedisScript;

    public SlidingWindowLimiter(RedisTemplate<String, Object> redisTemplate, DefaultRedisScript<List> defaultRedisScript)
    {
        this.redisTemplate = redisTemplate;
        this.defaultRedisScript = defaultRedisScript;
    }

    @Override
    public RateLimitResult apply(RateLimitRule rule, String username)
    {
        log.debug("Executing the rule {} for user {}", rule::getName, () -> username);

        // Use a unique key prefix for sliding window to avoid collision with fixed window keys
        String key = "rate:sliding:" + username + ":" + rule.getName();

        long currentTimeMillis = System.currentTimeMillis();
        long windowSizeMillis = rule.getWindowSizeInSeconds() * 1000L;
        int limit = rule.getLimit();

        List values = redisTemplate.execute(
            defaultRedisScript,
            Collections.singletonList(key),
            currentTimeMillis,
            windowSizeMillis,
            limit
        );

        log.debug("Lua script returned values: {}", () -> values);

        if (values == null || values.size() < 3) {
            log.error("Unexpected response from Lua script: {}", () -> values);
            throw new IllegalStateException("Invalid response from rate limiter script");
        }

        long currentCount = values.get(0) != null ? ((Number) values.get(0)).longValue() : 0;
        long remainingTokens = values.get(1) != null ? ((Number) values.get(1)).longValue() : 0;
        long resetTimeMillis = values.get(2) != null ? ((Number) values.get(2)).longValue() : 0;

        boolean allowed = currentCount <= limit;

        // Calculate retry after in seconds if rate limit is exceeded
        long retryAfterSeconds = 0;
        if (!allowed) {
            retryAfterSeconds = (resetTimeMillis - currentTimeMillis) / 1000;
        }

        return RateLimitResult.builder()
            .ruleName(rule.getName())
            .description(rule.getDescription())
            .allowed(allowed)
            .remaining(remainingTokens)
            .resetEpochSeconds(resetTimeMillis / 1000)
            .retryAfterSeconds(retryAfterSeconds)
            .build();
    }
}
