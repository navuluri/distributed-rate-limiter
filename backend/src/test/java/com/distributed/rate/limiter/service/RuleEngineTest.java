package com.distributed.rate.limiter.service;

import com.distributed.rate.limiter.exceptions.RateLimitExceededException;
import com.distributed.rate.limiter.limiters.RateLimiter;
import com.distributed.rate.limiter.models.RateLimitResult;
import com.distributed.rate.limiter.models.RateLimitRule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RuleEngineTest
{
    @Mock
    private RateLimiter fixedWindowLimiter;

    @Mock
    private RateLimiter slidingWindowLimiter;

    @InjectMocks
    private RuleEngine ruleEngine;

    private List<RateLimitRule> rateLimitRules;
    private Map<String, RateLimiter> rateLimiters;
    private String testUsername;

    @BeforeEach
    void setUp()
    {
        testUsername = "testUser";

        RateLimitRule rule1 = RateLimitRule.builder()
            .name("minute-limit")
            .description("5 requests per minute")
            .limit(5)
            .windowSizeInSeconds(60)
            .measure("minute")
            .algorithm("fixedWindow")
            .build();

        RateLimitRule rule2 = RateLimitRule.builder()
            .name("hour-limit")
            .description("100 requests per hour")
            .limit(100)
            .windowSizeInSeconds(3600)
            .measure("hour")
            .algorithm("slidingWindow")
            .build();

        rateLimitRules = Arrays.asList(rule1, rule2);

        rateLimiters = new HashMap<>();
        rateLimiters.put("fixedWindow", fixedWindowLimiter);
        rateLimiters.put("slidingWindow", slidingWindowLimiter);

        ruleEngine = new RuleEngine(rateLimitRules, rateLimiters);
    }

    @Test
    void testApplyRules_AllRulesPass_ShouldNotThrowException()
    {
        // Arrange
        RateLimitResult allowedResult = RateLimitResult.builder()
            .allowed(true)
            .remaining(3)
            .resetEpochSeconds(System.currentTimeMillis() / 1000 + 60)
            .retryAfterSeconds(0)
            .build();

        when(fixedWindowLimiter.apply(any(RateLimitRule.class), eq(testUsername)))
            .thenReturn(allowedResult);
        when(slidingWindowLimiter.apply(any(RateLimitRule.class), eq(testUsername)))
            .thenReturn(allowedResult);

        // Act & Assert
        assertDoesNotThrow(() -> ruleEngine.applyRules(testUsername));

        verify(fixedWindowLimiter).apply(any(RateLimitRule.class), eq(testUsername));
        verify(slidingWindowLimiter).apply(any(RateLimitRule.class), eq(testUsername));
    }

    @Test
    void testApplyRules_FirstRuleFails_ShouldThrowException()
    {
        // Arrange
        RateLimitResult deniedResult = RateLimitResult.builder()
            .allowed(false)
            .remaining(0)
            .resetEpochSeconds(System.currentTimeMillis() / 1000 + 60)
            .retryAfterSeconds(45)
            .build();

        when(fixedWindowLimiter.apply(any(RateLimitRule.class), eq(testUsername)))
            .thenReturn(deniedResult);

        // Act & Assert
        RateLimitExceededException exception = assertThrows(
            RateLimitExceededException.class,
            () -> ruleEngine.applyRules(testUsername)
        );

        assertNotNull(exception);
        assertTrue(exception.getMessage().contains("Rate limit exceeded"));
        assertTrue(exception.getMessage().contains("5"));
        assertTrue(exception.getMessage().contains("minute"));
        assertEquals(5, exception.getLimit());
        assertEquals(45, exception.getResetTime());

        verify(fixedWindowLimiter).apply(any(RateLimitRule.class), eq(testUsername));
        verify(slidingWindowLimiter, never()).apply(any(RateLimitRule.class), eq(testUsername));
    }

    @Test
    void testApplyRules_SecondRuleFails_ShouldThrowException()
    {
        // Arrange
        RateLimitResult allowedResult = RateLimitResult.builder()
            .allowed(true)
            .remaining(3)
            .resetEpochSeconds(System.currentTimeMillis() / 1000 + 60)
            .retryAfterSeconds(0)
            .build();

        RateLimitResult deniedResult = RateLimitResult.builder()
            .allowed(false)
            .remaining(0)
            .resetEpochSeconds(System.currentTimeMillis() / 1000 + 3600)
            .retryAfterSeconds(3500)
            .build();

        when(fixedWindowLimiter.apply(any(RateLimitRule.class), eq(testUsername)))
            .thenReturn(allowedResult);
        when(slidingWindowLimiter.apply(any(RateLimitRule.class), eq(testUsername)))
            .thenReturn(deniedResult);

        // Act & Assert
        RateLimitExceededException exception = assertThrows(
            RateLimitExceededException.class,
            () -> ruleEngine.applyRules(testUsername)
        );

        assertNotNull(exception);
        assertTrue(exception.getMessage().contains("100"));
        assertTrue(exception.getMessage().contains("hour"));
        assertEquals(100, exception.getLimit());

        verify(fixedWindowLimiter).apply(any(RateLimitRule.class), eq(testUsername));
        verify(slidingWindowLimiter).apply(any(RateLimitRule.class), eq(testUsername));
    }

    @Test
    void testApplyRules_MultipleRules_AllExecutedInOrder()
    {
        // Arrange
        RateLimitResult allowedResult = RateLimitResult.builder()
            .allowed(true)
            .remaining(10)
            .resetEpochSeconds(System.currentTimeMillis() / 1000 + 60)
            .retryAfterSeconds(0)
            .build();

        when(fixedWindowLimiter.apply(any(RateLimitRule.class), eq(testUsername)))
            .thenReturn(allowedResult);
        when(slidingWindowLimiter.apply(any(RateLimitRule.class), eq(testUsername)))
            .thenReturn(allowedResult);

        // Act
        ruleEngine.applyRules(testUsername);

        // Assert
        verify(fixedWindowLimiter).apply(
            argThat(rule -> "minute-limit".equals(rule.getName())),
            eq(testUsername)
        );
        verify(slidingWindowLimiter).apply(
            argThat(rule -> "hour-limit".equals(rule.getName())),
            eq(testUsername)
        );
    }

    @Test
    void testApplyRules_DifferentUsers_ShouldBeIndependent()
    {
        // Arrange
        String user1 = "user1";
        String user2 = "user2";

        RateLimitResult allowedResult = RateLimitResult.builder()
            .allowed(true)
            .remaining(3)
            .resetEpochSeconds(System.currentTimeMillis() / 1000 + 60)
            .retryAfterSeconds(0)
            .build();

        when(fixedWindowLimiter.apply(any(RateLimitRule.class), anyString()))
            .thenReturn(allowedResult);
        when(slidingWindowLimiter.apply(any(RateLimitRule.class), anyString()))
            .thenReturn(allowedResult);

        // Act
        ruleEngine.applyRules(user1);
        ruleEngine.applyRules(user2);

        // Assert
        verify(fixedWindowLimiter).apply(any(RateLimitRule.class), eq(user1));
        verify(fixedWindowLimiter).apply(any(RateLimitRule.class), eq(user2));
        verify(slidingWindowLimiter).apply(any(RateLimitRule.class), eq(user1));
        verify(slidingWindowLimiter).apply(any(RateLimitRule.class), eq(user2));
    }

    @Test
    void testApplyRules_ExceptionMessage_ContainsCorrectDetails()
    {
        // Arrange
        RateLimitResult deniedResult = RateLimitResult.builder()
            .allowed(false)
            .remaining(-1)
            .resetEpochSeconds(System.currentTimeMillis() / 1000 + 60)
            .retryAfterSeconds(45)
            .build();

        when(fixedWindowLimiter.apply(any(RateLimitRule.class), eq(testUsername)))
            .thenReturn(deniedResult);

        // Act
        RateLimitExceededException exception = assertThrows(
            RateLimitExceededException.class,
            () -> ruleEngine.applyRules(testUsername)
        );

        // Assert
        String message = exception.getMessage();
        assertTrue(message.contains("Rate limit exceeded"));
        assertTrue(message.contains("Maximum 5 requests"));
        assertTrue(message.contains("per minute"));
        assertTrue(message.contains("Try again in 45 seconds"));
    }
}

