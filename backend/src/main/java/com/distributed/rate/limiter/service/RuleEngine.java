package com.distributed.rate.limiter.service;

import com.distributed.rate.limiter.exceptions.RateLimitExceededException;
import com.distributed.rate.limiter.limiters.RateLimiter;
import com.distributed.rate.limiter.models.RateLimitResult;
import com.distributed.rate.limiter.models.RateLimitRule;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
@Log4j2
public class RuleEngine
{
    private final List<RateLimitRule> rateLimitRules;
    private final Map<String, RateLimiter> rateLimiters;

    public RuleEngine(List<RateLimitRule> rateLimitRules, Map<String, RateLimiter> rateLimiters)
    {
        this.rateLimitRules = rateLimitRules;
        this.rateLimiters = rateLimiters;
    }

    public void applyRules(String userName)
    {
        for (RateLimitRule rule : rateLimitRules)
        {
            RateLimiter rateLimiter = rateLimiters.get(rule.getAlgorithm());
            // Apply rule on the scope key (userName)
            RateLimitResult rateLimitResult = rateLimiter.apply(rule, userName);
            if (!rateLimitResult.isAllowed())
            {
                log.error("User {} is rate limited by rule {} - {}", userName, rule.getName(), rule.getDescription());
                throw new RateLimitExceededException(
                    String.format("Rate limit exceeded. Maximum %d requests per %s allowed. Try again in %d seconds.",
                        rule.getLimit(), rule.getMeasure(), rateLimitResult.getRetryAfterSeconds()),
                    rule.getLimit(), rateLimitResult.getRetryAfterSeconds());
            }
        }
    }
}
