package com.distributed.rate.limiter.service;

import org.springframework.stereotype.Service;

@Service
public class RateLimiterService
{
    private final RuleEngine ruleEngine;

    public RateLimiterService(RuleEngine ruleEngine)
    {
        this.ruleEngine = ruleEngine;
    }

    public void checkRateLimits(String userName)
    {
        ruleEngine.applyRules(userName);
    }

}
