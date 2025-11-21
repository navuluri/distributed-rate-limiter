package com.distributed.rate.limiter.exceptions;

public class RateLimitExceededException extends RuntimeException
{
    private final int limit;
    private final long resetTime;

    public RateLimitExceededException(String message, int limit, long resetTime)
    {
        super(message);
        this.limit = limit;
        this.resetTime = resetTime;
    }

    public int getLimit()
    {
        return limit;
    }

    public long getResetTime()
    {
        return resetTime;
    }
}

