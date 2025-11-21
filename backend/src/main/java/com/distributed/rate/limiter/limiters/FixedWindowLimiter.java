package com.distributed.rate.limiter.limiters;

import com.distributed.rate.limiter.models.RateLimitResult;
import com.distributed.rate.limiter.models.RateLimitRule;
import lombok.extern.log4j.Log4j2;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.concurrent.TimeUnit;

@Service("fixedWindow")
@Log4j2
public class FixedWindowLimiter implements RateLimiter
{
    private final RedisTemplate<String, Object> redisTemplate;

    public FixedWindowLimiter(RedisTemplate<String, Object> redisTemplate)
    {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public RateLimitResult apply(RateLimitRule rule, String username)
    {
        log.debug("Executing the rule {} for user {}", rule::getName, () -> username);
        long timeWindow = Instant.now().getEpochSecond() / rule.getWindowSizeInSeconds();
        String key = "rate:" + username + ":" + timeWindow;
        long totalRequestsInThisWindow;
        boolean flag = Boolean.TRUE.equals(redisTemplate.opsForValue().setIfAbsent(key, 1));
        if (flag)
        {
            redisTemplate.expire(key, rule.getWindowSizeInSeconds(), TimeUnit.SECONDS);
            totalRequestsInThisWindow = 1;
        }
        else
        {
            totalRequestsInThisWindow = redisTemplate.opsForValue().increment(key, 1);
        }
        return RateLimitResult
            .builder()
            .ruleName(rule.getName())
            .description(rule.getDescription())
            .resetEpochSeconds((timeWindow + 1) * rule.getWindowSizeInSeconds())
            .retryAfterSeconds(
                rule.getWindowSizeInSeconds() - (Instant.now().getEpochSecond() % rule.getWindowSizeInSeconds()))
            .remaining(rule.getLimit() - totalRequestsInThisWindow)
            .allowed(totalRequestsInThisWindow <= rule.getLimit())
            .build();
    }
}
