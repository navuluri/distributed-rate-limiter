package com.distributed.rate.limiter.limiters;

import com.distributed.rate.limiter.models.RateLimitResult;
import com.distributed.rate.limiter.models.RateLimitRule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Instant;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FixedWindowLimiterTest
{
    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    @InjectMocks
    private FixedWindowLimiter fixedWindowLimiter;

    private RateLimitRule testRule;
    private String testUsername;

    @BeforeEach
    void setUp()
    {
        testUsername = "testUser";
        testRule = RateLimitRule.builder()
            .name("test-rule")
            .description("Test rate limit rule")
            .limit(5)
            .windowSizeInSeconds(60)
            .measure("minute")
            .algorithm("fixedWindow")
            .build();

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @Test
    void testApply_FirstRequest_ShouldAllowAndSetKey()
    {
        // Arrange
        when(valueOperations.setIfAbsent(anyString(), eq(1))).thenReturn(true);
        when(redisTemplate.expire(anyString(), anyLong(), any(TimeUnit.class))).thenReturn(true);

        // Act
        RateLimitResult result = fixedWindowLimiter.apply(testRule, testUsername);

        // Assert
        assertNotNull(result);
        assertTrue(result.isAllowed());
        assertEquals("test-rule", result.getRuleName());
        assertEquals("Test rate limit rule", result.getDescription());
        assertEquals(4, result.getRemaining()); // 5 - 1 = 4
        verify(valueOperations).setIfAbsent(anyString(), eq(1));
        verify(redisTemplate).expire(anyString(), eq(60L), eq(TimeUnit.SECONDS));
    }

    @Test
    void testApply_SubsequentRequest_ShouldIncrementCounter()
    {
        // Arrange
        when(valueOperations.setIfAbsent(anyString(), eq(1))).thenReturn(false);
        when(valueOperations.increment(anyString(), eq(1L))).thenReturn(3L);

        // Act
        RateLimitResult result = fixedWindowLimiter.apply(testRule, testUsername);

        // Assert
        assertNotNull(result);
        assertTrue(result.isAllowed());
        assertEquals(2, result.getRemaining()); // 5 - 3 = 2
        verify(valueOperations).increment(anyString(), eq(1L));
        verify(redisTemplate, never()).expire(anyString(), anyLong(), any(TimeUnit.class));
    }

    @Test
    void testApply_ExceedLimit_ShouldNotAllow()
    {
        // Arrange
        when(valueOperations.setIfAbsent(anyString(), eq(1))).thenReturn(false);
        when(valueOperations.increment(anyString(), eq(1L))).thenReturn(6L);

        // Act
        RateLimitResult result = fixedWindowLimiter.apply(testRule, testUsername);

        // Assert
        assertNotNull(result);
        assertFalse(result.isAllowed());
        assertEquals(-1, result.getRemaining()); // 5 - 6 = -1
        assertTrue(result.getRetryAfterSeconds() > 0);
    }

    @Test
    void testApply_AtExactLimit_ShouldAllow()
    {
        // Arrange
        when(valueOperations.setIfAbsent(anyString(), eq(1))).thenReturn(false);
        when(valueOperations.increment(anyString(), eq(1L))).thenReturn(5L);

        // Act
        RateLimitResult result = fixedWindowLimiter.apply(testRule, testUsername);

        // Assert
        assertNotNull(result);
        assertTrue(result.isAllowed());
        assertEquals(0, result.getRemaining()); // 5 - 5 = 0
    }

    @Test
    void testApply_DifferentUsers_ShouldHaveSeparateCounters()
    {
        // Arrange
        String user1 = "user1";
        String user2 = "user2";

        when(valueOperations.setIfAbsent(contains(user1), eq(1))).thenReturn(true);
        when(valueOperations.setIfAbsent(contains(user2), eq(1))).thenReturn(true);
        when(redisTemplate.expire(anyString(), anyLong(), any(TimeUnit.class))).thenReturn(true);

        // Act
        RateLimitResult result1 = fixedWindowLimiter.apply(testRule, user1);
        RateLimitResult result2 = fixedWindowLimiter.apply(testRule, user2);

        // Assert
        assertTrue(result1.isAllowed());
        assertTrue(result2.isAllowed());
        verify(valueOperations, times(2)).setIfAbsent(anyString(), eq(1));
    }

    @Test
    void testApply_VerifyResetTimeCalculation()
    {
        // Arrange
        when(valueOperations.setIfAbsent(anyString(), eq(1))).thenReturn(true);
        when(redisTemplate.expire(anyString(), anyLong(), any(TimeUnit.class))).thenReturn(true);

        long beforeTest = Instant.now().getEpochSecond();

        // Act
        RateLimitResult result = fixedWindowLimiter.apply(testRule, testUsername);

        // Assert
        assertNotNull(result);
        assertTrue(result.getResetEpochSeconds() > beforeTest);
        assertTrue(result.getRetryAfterSeconds() <= 60);
        assertTrue(result.getRetryAfterSeconds() > 0);
    }
}

