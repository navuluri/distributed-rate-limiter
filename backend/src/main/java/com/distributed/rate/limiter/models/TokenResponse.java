package com.distributed.rate.limiter.models;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class TokenResponse
{
    private String token;
    private long createdAt;
    private long expiresAt;
}
