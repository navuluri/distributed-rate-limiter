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
import org.springframework.data.redis.core.script.DefaultRedisScript;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings({"rawtypes", "unchecked"})
class SlidingWindowLimiterTest
{
    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private DefaultRedisScript<List> defaultRedisScript;

    @InjectMocks
    private SlidingWindowLimiter slidingWindowLimiter;

    private RateLimitRule testRule;
    private String testUsername;

    @BeforeEach
    void setUp()
    {
        testUsername = "testUser";
        testRule = RateLimitRule.builder()
            .name("test-sliding-rule")
            .description("Test sliding window rate limit rule")
            .limit(10)
            .windowSizeInSeconds(60)
            .measure("minute")
            .algorithm("slidingWindow")
            .build();
    }

    @Test
    void testApply_WithinLimit_ShouldAllow()
    {
        // Arrange
        List luaResult = Arrays.asList(3L, 7L, System.currentTimeMillis() + 60000);
        when(redisTemplate.execute(any(DefaultRedisScript.class), anyList(), any(), any(), any()))
            .thenReturn(luaResult);

        // Act
        RateLimitResult result = slidingWindowLimiter.apply(testRule, testUsername);

        // Assert
        assertNotNull(result);
        assertTrue(result.isAllowed());
        assertEquals("test-sliding-rule", result.getRuleName());
        assertEquals("Test sliding window rate limit rule", result.getDescription());
        assertEquals(7, result.getRemaining());
        assertTrue(result.getResetEpochSeconds() > 0);
        assertEquals(0, result.getRetryAfterSeconds());
    }

    @Test
    void testApply_ExceedLimit_ShouldNotAllow()
    {
        // Arrange
        long currentTime = System.currentTimeMillis();
        long resetTime = currentTime + 30000; // 30 seconds from now
        List luaResult = Arrays.asList(11L, -1L, resetTime);
        when(redisTemplate.execute(any(DefaultRedisScript.class), anyList(), any(), any(), any()))
            .thenReturn(luaResult);

        // Act
        RateLimitResult result = slidingWindowLimiter.apply(testRule, testUsername);

        // Assert
        assertNotNull(result);
        assertFalse(result.isAllowed());
        assertEquals(-1, result.getRemaining());
        assertTrue(result.getRetryAfterSeconds() >= 29); // approximately 30 seconds
        assertTrue(result.getRetryAfterSeconds() <= 31);
    }

    @Test
    void testApply_AtExactLimit_ShouldAllow()
    {
        // Arrange
        List luaResult = Arrays.asList(10L, 0L, System.currentTimeMillis() + 60000);
        when(redisTemplate.execute(any(DefaultRedisScript.class), anyList(), any(), any(), any()))
            .thenReturn(luaResult);

        // Act
        RateLimitResult result = slidingWindowLimiter.apply(testRule, testUsername);

        // Assert
        assertNotNull(result);
        assertTrue(result.isAllowed());
        assertEquals(0, result.getRemaining());
    }

    @Test
    void testApply_FirstRequest_ShouldAllow()
    {
        // Arrange
        List luaResult = Arrays.asList(1L, 9L, System.currentTimeMillis() + 60000);
        when(redisTemplate.execute(any(DefaultRedisScript.class), anyList(), any(), any(), any()))
            .thenReturn(luaResult);

        // Act
        RateLimitResult result = slidingWindowLimiter.apply(testRule, testUsername);

        // Assert
        assertNotNull(result);
        assertTrue(result.isAllowed());
        assertEquals(9, result.getRemaining());
    }

    @Test
    void testApply_InvalidLuaResponse_ShouldThrowException()
    {
        // Arrange
        List luaResult = Arrays.asList(1L, 9L); // Missing third element
        when(redisTemplate.execute(any(DefaultRedisScript.class), anyList(), any(), any(), any()))
            .thenReturn(luaResult);

        // Act & Assert
        assertThrows(IllegalStateException.class, () ->
            slidingWindowLimiter.apply(testRule, testUsername)
        );
    }

    @Test
    void testApply_VerifyRedisScriptExecution()
    {
        // Arrange
        List luaResult = Arrays.asList(5L, 5L, System.currentTimeMillis() + 60000);
        when(redisTemplate.execute(any(DefaultRedisScript.class), anyList(), any(), any(), any()))
            .thenReturn(luaResult);

        // Act
        slidingWindowLimiter.apply(testRule, testUsername);

        // Assert
        verify(redisTemplate).execute(
            eq(defaultRedisScript),
            anyList(),
            anyLong(), // currentTimeMillis
            eq(60000L), // windowSizeMillis
            eq(10) // limit
        );
    }

    @Test
    void testApply_VerifyKeyFormat()
    {
        // Arrange
        List luaResult = Arrays.asList(1L, 9L, System.currentTimeMillis() + 60000);
        when(redisTemplate.execute(any(DefaultRedisScript.class), anyList(), any(), any(), any()))
            .thenReturn(luaResult);

        // Act
        slidingWindowLimiter.apply(testRule, testUsername);

        // Assert
        verify(redisTemplate).execute(
            any(DefaultRedisScript.class),
            argThat(keys -> {
                String key = (String) ((List) keys).get(0);
                return key.startsWith("rate:sliding:") &&
                       key.contains(testUsername) &&
                       key.contains(testRule.getMeasure());
            }),
            anyLong(),
            anyLong(),
            anyInt()
        );
    }

    @Test
    void testApply_DifferentUsers_ShouldHaveSeparateKeys()
    {
        // Arrange
        String user1 = "user1";
        String user2 = "user2";
        List luaResult = Arrays.asList(1L, 9L, System.currentTimeMillis() + 60000);
        when(redisTemplate.execute(any(DefaultRedisScript.class), anyList(), any(), any(), any()))
            .thenReturn(luaResult);

        // Act
        slidingWindowLimiter.apply(testRule, user1);
        slidingWindowLimiter.apply(testRule, user2);

        // Assert
        verify(redisTemplate, times(2)).execute(
            any(DefaultRedisScript.class),
            anyList(),
            anyLong(),
            anyLong(),
            anyInt()
        );
    }

    @Test
    void testApply_ResetTimeCalculation()
    {
        // Arrange
        long currentTime = System.currentTimeMillis();
        long resetTimeMillis = currentTime + 45000; // 45 seconds from now
        List luaResult = Arrays.asList(5L, 5L, resetTimeMillis);
        when(redisTemplate.execute(any(DefaultRedisScript.class), anyList(), any(), any(), any()))
            .thenReturn(luaResult);

        // Act
        RateLimitResult result = slidingWindowLimiter.apply(testRule, testUsername);

        // Assert
        assertNotNull(result);
        long expectedResetEpochSeconds = resetTimeMillis / 1000;
        assertEquals(expectedResetEpochSeconds, result.getResetEpochSeconds());
    }
}

