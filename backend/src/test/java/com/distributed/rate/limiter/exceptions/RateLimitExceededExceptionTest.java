package com.distributed.rate.limiter.exceptions;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RateLimitExceededExceptionTest
{
    @Test
    void testConstructor_WithAllParameters()
    {
        // Arrange
        String message = "Rate limit exceeded. Try again later.";
        int limit = 5;
        long resetTime = 60;

        // Act
        RateLimitExceededException exception = new RateLimitExceededException(
            message, limit, resetTime
        );

        // Assert
        assertNotNull(exception);
        assertEquals(message, exception.getMessage());
        assertEquals(limit, exception.getLimit());
        assertEquals(resetTime, exception.getResetTime());
    }

    @Test
    void testGetLimit_ReturnsCorrectValue()
    {
        // Arrange
        int expectedLimit = 10;
        RateLimitExceededException exception = new RateLimitExceededException(
            "Test message", expectedLimit, 30
        );

        // Act
        int actualLimit = exception.getLimit();

        // Assert
        assertEquals(expectedLimit, actualLimit);
    }

    @Test
    void testGetResetTime_ReturnsCorrectValue()
    {
        // Arrange
        long expectedResetTime = 120;
        RateLimitExceededException exception = new RateLimitExceededException(
            "Test message", 5, expectedResetTime
        );

        // Act
        long actualResetTime = exception.getResetTime();

        // Assert
        assertEquals(expectedResetTime, actualResetTime);
    }

    @Test
    void testException_IsRuntimeException()
    {
        // Arrange & Act
        RateLimitExceededException exception = new RateLimitExceededException(
            "Test", 5, 60
        );

        // Assert
        assertTrue(exception instanceof RuntimeException);
    }

    @Test
    void testConstructor_WithZeroValues()
    {
        // Arrange & Act
        RateLimitExceededException exception = new RateLimitExceededException(
            "Zero values", 0, 0
        );

        // Assert
        assertEquals(0, exception.getLimit());
        assertEquals(0, exception.getResetTime());
        assertEquals("Zero values", exception.getMessage());
    }

    @Test
    void testConstructor_WithNegativeValues()
    {
        // Arrange & Act
        RateLimitExceededException exception = new RateLimitExceededException(
            "Negative values", -1, -1
        );

        // Assert
        assertEquals(-1, exception.getLimit());
        assertEquals(-1, exception.getResetTime());
    }

    @Test
    void testConstructor_WithLargeValues()
    {
        // Arrange
        int largeLimit = Integer.MAX_VALUE;
        long largeResetTime = Long.MAX_VALUE;

        // Act
        RateLimitExceededException exception = new RateLimitExceededException(
            "Large values", largeLimit, largeResetTime
        );

        // Assert
        assertEquals(largeLimit, exception.getLimit());
        assertEquals(largeResetTime, exception.getResetTime());
    }

    @Test
    void testConstructor_WithNullMessage()
    {
        // Arrange & Act
        RateLimitExceededException exception = new RateLimitExceededException(
            null, 5, 60
        );

        // Assert
        assertNull(exception.getMessage());
        assertEquals(5, exception.getLimit());
        assertEquals(60, exception.getResetTime());
    }

    @Test
    void testConstructor_WithEmptyMessage()
    {
        // Arrange & Act
        RateLimitExceededException exception = new RateLimitExceededException(
            "", 5, 60
        );

        // Assert
        assertEquals("", exception.getMessage());
        assertEquals(5, exception.getLimit());
        assertEquals(60, exception.getResetTime());
    }

    @Test
    void testException_CanBeCaught()
    {
        // Arrange
        String expectedMessage = "Rate limit exceeded";

        // Act & Assert
        assertThrows(RateLimitExceededException.class, () -> {
            throw new RateLimitExceededException(expectedMessage, 5, 60);
        });
    }

    @Test
    void testException_MessageIsAccessible()
    {
        // Arrange
        String message = "Maximum 100 requests per hour allowed. Try again in 3500 seconds.";
        RateLimitExceededException exception = new RateLimitExceededException(
            message, 100, 3500
        );

        // Act
        String actualMessage = exception.getMessage();

        // Assert
        assertEquals(message, actualMessage);
        assertTrue(actualMessage.contains("100"));
        assertTrue(actualMessage.contains("3500"));
    }
}

