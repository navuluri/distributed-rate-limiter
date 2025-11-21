package com.distributed.rate.limiter.api;

import com.distributed.rate.limiter.models.ApiResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RequestLimitsInfoApiTest
{
    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private RequestLimitsInfoApi requestLimitsInfoApi;

    private String testUsername;

    @BeforeEach
    void setUp()
    {
        testUsername = "testUser";
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(authentication.getName()).thenReturn(testUsername);
    }

    @Test
    void testInfo_WithValidData_ShouldReturnApiResponse()
    {
        // Arrange
        List<Object> redisValues = Arrays.asList(5, 50, 500);
        when(valueOperations.get(anyString())).thenReturn(5);
        when(valueOperations.multiGet(anyList())).thenReturn(redisValues);

        // Act
        ResponseEntity<ApiResponse> response = requestLimitsInfoApi.info(authentication);

        // Assert
        assertNotNull(response);
        assertEquals(200, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertEquals(testUsername, response.getBody().getUser());
        assertEquals(5, response.getBody().getTotalRequestsThisMinute());
        assertEquals(50, response.getBody().getTotalRequestsThisHour());
        assertEquals(500, response.getBody().getTotalRequestsToday());
    }

    @Test
    void testInfo_WithNullValues_ShouldReturnZeros()
    {
        // Arrange
        List<Object> redisValues = Arrays.asList(null, null, null);
        when(valueOperations.get(anyString())).thenReturn(null);
        when(valueOperations.multiGet(anyList())).thenReturn(redisValues);

        // Act
        ResponseEntity<ApiResponse> response = requestLimitsInfoApi.info(authentication);

        // Assert
        assertNotNull(response);
        assertNotNull(response.getBody());
        assertEquals(0, response.getBody().getTotalRequestsThisMinute());
        assertEquals(0, response.getBody().getTotalRequestsThisHour());
        assertEquals(0, response.getBody().getTotalRequestsToday());
    }

    @Test
    void testInfo_WithPartialNullValues_ShouldHandleCorrectly()
    {
        // Arrange
        List<Object> redisValues = Arrays.asList(10, null, 200);
        when(valueOperations.get(anyString())).thenReturn(10);
        when(valueOperations.multiGet(anyList())).thenReturn(redisValues);

        // Act
        ResponseEntity<ApiResponse> response = requestLimitsInfoApi.info(authentication);

        // Assert
        assertNotNull(response);
        assertNotNull(response.getBody());
        assertEquals(10, response.getBody().getTotalRequestsThisMinute());
        assertEquals(0, response.getBody().getTotalRequestsThisHour());
        assertEquals(200, response.getBody().getTotalRequestsToday());
    }

    @Test
    void testInfo_VerifyRedisKeysFormat()
    {
        // Arrange
        List<Object> redisValues = Arrays.asList(1, 2, 3);
        when(valueOperations.get(anyString())).thenReturn(1);
        when(valueOperations.multiGet(anyList())).thenReturn(redisValues);

        long minute = Instant.now().getEpochSecond() / 60;
        long hour = Instant.now().getEpochSecond() / 3600;
        long day = Instant.now().getEpochSecond() / 86400;

        // Act
        requestLimitsInfoApi.info(authentication);

        // Assert
        verify(valueOperations).multiGet(argThat(keys -> {
            List<String> keyList = (List<String>) keys;
            return keyList.get(0).contains("rate:" + testUsername + ":" + minute) &&
                   keyList.get(1).contains("rate:" + testUsername + ":" + hour) &&
                   keyList.get(2).contains("rate:" + testUsername + ":" + day);
        }));
    }

    @Test
    void testInfo_MessageContainsUsername()
    {
        // Arrange
        List<Object> redisValues = Arrays.asList(5, 50, 500);
        when(valueOperations.get(anyString())).thenReturn(5);
        when(valueOperations.multiGet(anyList())).thenReturn(redisValues);

        // Act
        ResponseEntity<ApiResponse> response = requestLimitsInfoApi.info(authentication);

        // Assert
        assertNotNull(response.getBody());
        assertTrue(response.getBody().getMessage().contains(testUsername));
    }

    @Test
    void testInfo_MessageContainsRequestCount()
    {
        // Arrange
        List<Object> redisValues = Arrays.asList(7, 50, 500);
        when(valueOperations.get(anyString())).thenReturn(7);
        when(valueOperations.multiGet(anyList())).thenReturn(redisValues);

        // Act
        ResponseEntity<ApiResponse> response = requestLimitsInfoApi.info(authentication);

        // Assert
        assertNotNull(response.getBody());
        assertTrue(response.getBody().getMessage().contains("7"));
    }

    @Test
    void testInfo_DifferentUsers_ShouldReturnDifferentData()
    {
        // Arrange
        String user1 = "user1";
        String user2 = "user2";

        when(valueOperations.multiGet(anyList())).thenReturn(Arrays.asList(5, 50, 500));
        when(valueOperations.get(anyString())).thenReturn(5);

        // First user
        when(authentication.getName()).thenReturn(user1);
        ResponseEntity<ApiResponse> response1 = requestLimitsInfoApi.info(authentication);

        // Second user
        when(authentication.getName()).thenReturn(user2);
        ResponseEntity<ApiResponse> response2 = requestLimitsInfoApi.info(authentication);

        // Assert
        assertNotNull(response1.getBody());
        assertNotNull(response2.getBody());
        assertEquals(user1, response1.getBody().getUser());
        assertEquals(user2, response2.getBody().getUser());
    }

    @Test
    void testInfo_VerifyRedisTemplateInteractions()
    {
        // Arrange
        List<Object> redisValues = Arrays.asList(1, 2, 3);
        when(valueOperations.get(anyString())).thenReturn(1);
        when(valueOperations.multiGet(anyList())).thenReturn(redisValues);

        // Act
        requestLimitsInfoApi.info(authentication);

        // Assert
        verify(redisTemplate, atLeastOnce()).opsForValue();
        verify(valueOperations, times(2)).get(anyString());
        verify(valueOperations).multiGet(anyList());
    }

    @Test
    void testInfo_WithZeroRequests_ShouldReturnZeros()
    {
        // Arrange
        List<Object> redisValues = Arrays.asList(0, 0, 0);
        when(valueOperations.get(anyString())).thenReturn(0);
        when(valueOperations.multiGet(anyList())).thenReturn(redisValues);

        // Act
        ResponseEntity<ApiResponse> response = requestLimitsInfoApi.info(authentication);

        // Assert
        assertNotNull(response.getBody());
        assertEquals(0, response.getBody().getTotalRequestsThisMinute());
        assertEquals(0, response.getBody().getTotalRequestsThisHour());
        assertEquals(0, response.getBody().getTotalRequestsToday());
    }

    @Test
    void testInfo_WithLargeValues_ShouldHandleCorrectly()
    {
        // Arrange
        List<Object> redisValues = Arrays.asList(999, 9999, 99999);
        when(valueOperations.get(anyString())).thenReturn(999);
        when(valueOperations.multiGet(anyList())).thenReturn(redisValues);

        // Act
        ResponseEntity<ApiResponse> response = requestLimitsInfoApi.info(authentication);

        // Assert
        assertNotNull(response.getBody());
        assertEquals(999, response.getBody().getTotalRequestsThisMinute());
        assertEquals(9999, response.getBody().getTotalRequestsThisHour());
        assertEquals(99999, response.getBody().getTotalRequestsToday());
    }

    @Test
    void testInfo_ResponseContainsMessage()
    {
        // Arrange
        List<Object> redisValues = Arrays.asList(1, 2, 3);
        when(valueOperations.get(anyString())).thenReturn(1);
        when(valueOperations.multiGet(anyList())).thenReturn(redisValues);

        // Act
        ResponseEntity<ApiResponse> response = requestLimitsInfoApi.info(authentication);

        // Assert
        assertNotNull(response.getBody());
        assertNotNull(response.getBody().getMessage());
        assertFalse(response.getBody().getMessage().isEmpty());
    }
}

