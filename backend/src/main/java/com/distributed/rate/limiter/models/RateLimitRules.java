package com.distributed.rate.limiter.models;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class RateLimitRules implements Serializable
{
    @Serial
    private static final long serialVersionUID = -5001881861840980607L;
    /*
    Read it like: A rate limit policy contains multiple rate limit rules like "user-per-minute", "user-per-hour" etc
     */
    private List<RateLimitRule> rules;
}
