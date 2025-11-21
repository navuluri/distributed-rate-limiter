package com.distributed.rate.limiter.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RateLimiterServiceTest
{
    @Mock
    private RuleEngine ruleEngine;

    @InjectMocks
    private RateLimiterService rateLimiterService;

    private String testUsername;

    @BeforeEach
    void setUp()
    {
        testUsername = "testUser";
    }

    @Test
    void testCheckRateLimits_ShouldCallRuleEngine()
    {
        // Arrange
        doNothing().when(ruleEngine).applyRules(testUsername);

        // Act
        rateLimiterService.checkRateLimits(testUsername);

        // Assert
        verify(ruleEngine).applyRules(testUsername);
    }

    @Test
    void testCheckRateLimits_WithDifferentUsers()
    {
        // Arrange
        String user1 = "user1";
        String user2 = "user2";
        doNothing().when(ruleEngine).applyRules(anyString());

        // Act
        rateLimiterService.checkRateLimits(user1);
        rateLimiterService.checkRateLimits(user2);

        // Assert
        verify(ruleEngine).applyRules(user1);
        verify(ruleEngine).applyRules(user2);
        verify(ruleEngine, times(2)).applyRules(anyString());
    }

    @Test
    void testCheckRateLimits_MultipleCallsForSameUser()
    {
        // Arrange
        doNothing().when(ruleEngine).applyRules(testUsername);

        // Act
        rateLimiterService.checkRateLimits(testUsername);
        rateLimiterService.checkRateLimits(testUsername);
        rateLimiterService.checkRateLimits(testUsername);

        // Assert
        verify(ruleEngine, times(3)).applyRules(testUsername);
    }

    @Test
    void testCheckRateLimits_PropagatesException()
    {
        // Arrange
        RuntimeException expectedException = new RuntimeException("Rate limit exceeded");
        doThrow(expectedException).when(ruleEngine).applyRules(testUsername);

        // Act & Assert
        try
        {
            rateLimiterService.checkRateLimits(testUsername);
        }
        catch (RuntimeException e)
        {
            // Expected
        }

        verify(ruleEngine).applyRules(testUsername);
    }
}

