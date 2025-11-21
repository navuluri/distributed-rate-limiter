package com.distributed.rate.limiter.models;

import lombok.Builder;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

@Data
@Builder
public class RateLimitResult implements Serializable
{
    @Serial
    private static final long serialVersionUID = 5458456616184847744L;
    private String ruleName;
    private String description;
    private boolean allowed;
    private long retryAfterSeconds;
    private long remaining;
    private long resetEpochSeconds;
}
