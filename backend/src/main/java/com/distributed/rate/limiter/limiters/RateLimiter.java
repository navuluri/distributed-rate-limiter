package com.distributed.rate.limiter.limiters;

import com.distributed.rate.limiter.models.RateLimitResult;
import com.distributed.rate.limiter.models.RateLimitRule;

public interface RateLimiter
{
    /**
     * Apply one rule for a given scope key and return result.
     * key - Redis key for this scope+window or scope
     * limit - rule limit
     * windowSeconds - window length
     * return RateLimitResult (allowed + remaining + reset)
     */
    RateLimitResult apply(RateLimitRule rule, String scopeKey);
}
