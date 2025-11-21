package com.distributed.rate.limiter.exceptions;

import com.distributed.rate.limiter.models.ErrorResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class ApplicationExceptionAdviceTest
{
    @InjectMocks
    private ApplicationExceptionAdvice exceptionAdvice;

    @Test
    void testHandleUnauthorizedException_ShouldReturn401()
    {
        // Arrange
        HttpClientErrorException exception = new HttpClientErrorException(
            HttpStatus.UNAUTHORIZED, "Unauthorized"
        );

        // Act
        ResponseEntity<ErrorResponse> response =
            exceptionAdvice.handleUnauthorizedException(exception);

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(401, response.getBody().getStatus());
        assertTrue(response.getBody().getMessage().contains("Unauthorized"));
    }

    @Test
    void testHandleServerErrorException_ShouldReturn500()
    {
        // Arrange
        String errorMessage = "Feature not implemented";
        HttpServerErrorException exception = new HttpServerErrorException(
            HttpStatus.INTERNAL_SERVER_ERROR, errorMessage
        );

        // Act
        ResponseEntity<ErrorResponse> response =
            exceptionAdvice.handleServerErrorException(exception);

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(500, response.getBody().getStatus());
        assertTrue(response.getBody().getMessage().contains(errorMessage));
    }

    @Test
    void testHandleRateLimitExceededException_ShouldReturn429()
    {
        // Arrange
        String errorMessage = "Rate limit exceeded. Maximum 5 requests per minute allowed.";
        RateLimitExceededException exception = new RateLimitExceededException(
            errorMessage, 5, 45
        );

        // Act
        ResponseEntity<ErrorResponse> response =
            exceptionAdvice.handleRateLimitExceededException(exception);

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.TOO_MANY_REQUESTS, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(429, response.getBody().getStatus());
        assertEquals(errorMessage, response.getBody().getMessage());
    }

    @Test
    void testHandleRateLimitExceededException_VerifyExceptionDetails()
    {
        // Arrange
        int limit = 10;
        long resetTime = 60;
        String message = "Too many requests";
        RateLimitExceededException exception = new RateLimitExceededException(
            message, limit, resetTime
        );

        // Act
        ResponseEntity<ErrorResponse> response =
            exceptionAdvice.handleRateLimitExceededException(exception);

        // Assert
        assertEquals(limit, exception.getLimit());
        assertEquals(resetTime, exception.getResetTime());
        assertNotNull(response.getBody());
        assertEquals(message, response.getBody().getMessage());
    }

    @Test
    void testHandleUnauthorizedException_ResponseBodyNotNull()
    {
        // Arrange
        HttpClientErrorException exception = new HttpClientErrorException(
            HttpStatus.UNAUTHORIZED
        );

        // Act
        ResponseEntity<ErrorResponse> response =
            exceptionAdvice.handleUnauthorizedException(exception);

        // Assert
        assertNotNull(response.getBody());
        assertNotNull(response.getBody().getMessage());
        assertTrue(response.getBody().getStatus() > 0);
    }

    @Test
    void testHandleServerErrorException_WithNullMessage()
    {
        // Arrange
        HttpServerErrorException exception = new HttpServerErrorException(
            HttpStatus.INTERNAL_SERVER_ERROR, ""
        );

        // Act
        ResponseEntity<ErrorResponse> response =
            exceptionAdvice.handleServerErrorException(exception);

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertNotNull(response.getBody());
        // Empty message should trigger default message
        assertTrue(response.getBody().getMessage().contains("Internal server error") ||
                   response.getBody().getMessage().contains("500"));
    }

    @Test
    void testHandleRateLimitExceededException_WithZeroValues()
    {
        // Arrange
        RateLimitExceededException exception = new RateLimitExceededException(
            "Rate limit exceeded", 0, 0
        );

        // Act
        ResponseEntity<ErrorResponse> response =
            exceptionAdvice.handleRateLimitExceededException(exception);

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.TOO_MANY_REQUESTS, response.getStatusCode());
        assertEquals(0, exception.getLimit());
        assertEquals(0, exception.getResetTime());
    }

    @Test
    void testHandleGeneralException_ShouldReturn500()
    {
        // Arrange
        Exception exception = new RuntimeException("Unexpected error occurred");

        // Act
        ResponseEntity<ErrorResponse> response =
            exceptionAdvice.handleGeneralException(exception);

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(500, response.getBody().getStatus());
        assertTrue(response.getBody().getMessage().contains("Unexpected error occurred"));
        assertTrue(response.getBody().getMessage().contains("An unexpected error occurred"));
    }
}

