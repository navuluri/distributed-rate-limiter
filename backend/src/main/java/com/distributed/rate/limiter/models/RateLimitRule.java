package com.distributed.rate.limiter.models;

import lombok.Builder;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

@Data
@Builder
public class RateLimitRule implements Serializable
{
    /*
      Read the rule like: "user-per-minute" rule allows 100 requests per 60 seconds using "fixedWindow" algorithm
     */
    @Serial
    private static final long serialVersionUID = -6152126159318026026L;
    private String name; // Rule name: Ex: user-per-minute, user-per-hour etc
    private String description; // Rule description
    private int limit; // Ex: 100 requests
    private String measure; // Ex: user, ip, apiKey etc
    private int windowSizeInSeconds; // Ex: 60, 3600 etc where 60 = 1 minute, 3600 = 1 hour
    private String algorithm; // Ex: fixedWindow, slidingWindow etc

    @Override
    public String toString()
    {
        return "RateLimitRule [name=" + name + ", description=" + description + ", limit=" + limit + ", windowSizeInSeconds=" + windowSizeInSeconds + ", algorithm=" + algorithm + ", measure=" + measure + "]";
    }

}

