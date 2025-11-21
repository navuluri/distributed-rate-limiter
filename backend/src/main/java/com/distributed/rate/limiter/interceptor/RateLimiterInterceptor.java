package com.distributed.rate.limiter.interceptor;

import com.distributed.rate.limiter.service.RateLimiterService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class RateLimiterInterceptor implements HandlerInterceptor
{
    private static final int SECONDS_IN_MINUTE = 60;

    private final RateLimiterService rateLimiterService;

    public RateLimiterInterceptor(RateLimiterService rateLimiterService)
    {
        this.rateLimiterService = rateLimiterService;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception
    {
        if (request.getUserPrincipal().getName() == null)
        {
            throw new HttpClientErrorException(HttpStatus.UNAUTHORIZED, "User is not authenticated");
        }
        rateLimiterService.checkRateLimits(request.getUserPrincipal().getName());
        return true;
    }

}
