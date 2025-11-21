package com.distributed.rate.limiter.api;

import com.distributed.rate.limiter.models.ApiResponse;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api/v1")
public class RequestLimitsInfoApi
{
    private final RedisTemplate<String, Object> redisTemplate;

    public RequestLimitsInfoApi(RedisTemplate<String, Object> redisTemplate)
    {
        this.redisTemplate = redisTemplate;
    }

    @GetMapping("/info")
    public ResponseEntity<ApiResponse> info(Authentication authentication)
    {
        long minuteWindow = Instant.now().getEpochSecond() / 60;
        var key = "rate:" + authentication.getName() + ":" + minuteWindow;
        redisTemplate.opsForValue().get(key);
        var message =
            String.format("Total number of requests in current minute window (%d) for user %s is %s", minuteWindow,
                authentication.getName(), redisTemplate.opsForValue().get(key));
        ApiResponse response = ApiResponse.builder().message(message).user(authentication.getName()).build();

        long minute = Instant.now().getEpochSecond() / 60;
        long hour = Instant.now().getEpochSecond() / 3600;
        long day = Instant.now().getEpochSecond() / 86400;

        var minuteKey = "rate:" + authentication.getName() + ":" + minute;
        var hourKey = "rate:" + authentication.getName() + ":" + hour;
        var dayKey = "rate:" + authentication.getName() + ":" + day;

        List<Object> values = redisTemplate.opsForValue().multiGet(List.of(minuteKey, hourKey, dayKey));
        response.setTotalRequestsToday(values.get(2) != null ? Integer.parseInt(values.get(2).toString()) : 0);
        response.setTotalRequestsThisHour(values.get(1) != null ? Integer.parseInt(values.get(1).toString()) : 0);
        response.setTotalRequestsThisMinute(values.get(0) != null ? Integer.parseInt(values.get(0).toString()) : 0);
        return ResponseEntity.ok(response);
    }
}
