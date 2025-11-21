package com.distributed.rate.limiter;

import com.distributed.rate.limiter.interceptor.RateLimiterInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class RateLimiterConfiguration implements WebMvcConfigurer
{
    private final RateLimiterInterceptor rateLimiterInterceptor;

    public RateLimiterConfiguration(RateLimiterInterceptor rateLimiterInterceptor)
    {
        this.rateLimiterInterceptor = rateLimiterInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry)
    {
        registry.addInterceptor(rateLimiterInterceptor).excludePathPatterns("/token");
    }
}
