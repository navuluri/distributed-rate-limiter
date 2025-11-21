package com.distributed.rate.limiter.api;

import com.distributed.rate.limiter.models.TokenResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.Arrays;
import java.util.Collection;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TokenApiTest
{
    @Mock
    private JwtEncoder jwtEncoder;

    @Mock
    private Authentication authentication;

    @Mock
    private Jwt jwt;

    @InjectMocks
    private TokenApi tokenApi;

    private String testUsername;
    private Collection<GrantedAuthority> authorities;

    @BeforeEach
    void setUp()
    {
        testUsername = "testUser";
        authorities = Arrays.asList(
            new SimpleGrantedAuthority("ROLE_USER"),
            new SimpleGrantedAuthority("ROLE_ADMIN")
        );

        // Set the token expiry using reflection
        ReflectionTestUtils.setField(tokenApi, "tokenExpirySeconds", 86400L);
    }

    @Test
    void testToken_ShouldReturnValidTokenResponse()
    {
        // Arrange
        String expectedToken = "eyJhbGciOiJSUzI1NiJ9.test.token";
        when(authentication.getName()).thenReturn(testUsername);
        when(authentication.getAuthorities()).thenReturn((Collection) authorities);
        when(jwtEncoder.encode(any(JwtEncoderParameters.class))).thenReturn(jwt);
        when(jwt.getTokenValue()).thenReturn(expectedToken);

        // Act
        ResponseEntity<TokenResponse> response = tokenApi.token(authentication);

        // Assert
        assertNotNull(response);
        assertEquals(200, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertEquals(expectedToken, response.getBody().getToken());
        assertTrue(response.getBody().getCreatedAt() > 0);
        assertTrue(response.getBody().getExpiresAt() > response.getBody().getCreatedAt());

        verify(jwtEncoder).encode(any(JwtEncoderParameters.class));
        verify(authentication).getName();
        verify(authentication).getAuthorities();
    }

    @Test
    void testToken_ShouldIncludeTokenInHeaders()
    {
        // Arrange
        String expectedToken = "test.jwt.token";
        when(authentication.getName()).thenReturn(testUsername);
        when(authentication.getAuthorities()).thenReturn((Collection) authorities);
        when(jwtEncoder.encode(any(JwtEncoderParameters.class))).thenReturn(jwt);
        when(jwt.getTokenValue()).thenReturn(expectedToken);

        // Act
        ResponseEntity<TokenResponse> response = tokenApi.token(authentication);

        // Assert
        HttpHeaders headers = response.getHeaders();
        assertNotNull(headers);
        assertTrue(headers.containsKey("X-JWT-Token"));
        assertEquals(expectedToken, headers.getFirst("X-JWT-Token"));
    }

    @Test
    void testToken_ExpiryTimeCalculation()
    {
        // Arrange
        long tokenExpirySeconds = 3600L; // 1 hour
        ReflectionTestUtils.setField(tokenApi, "tokenExpirySeconds", tokenExpirySeconds);

        when(authentication.getName()).thenReturn(testUsername);
        when(authentication.getAuthorities()).thenReturn((Collection) authorities);
        when(jwtEncoder.encode(any(JwtEncoderParameters.class))).thenReturn(jwt);
        when(jwt.getTokenValue()).thenReturn("token");

        long beforeCall = Instant.now().getEpochSecond();

        // Act
        ResponseEntity<TokenResponse> response = tokenApi.token(authentication);

        long afterCall = Instant.now().getEpochSecond();

        // Assert
        assertNotNull(response.getBody());
        long createdAt = response.getBody().getCreatedAt() / 1000;
        long expiresAt = response.getBody().getExpiresAt() / 1000;

        assertTrue(createdAt >= beforeCall && createdAt <= afterCall);
        assertTrue(expiresAt >= createdAt + tokenExpirySeconds - 1);
        assertTrue(expiresAt <= createdAt + tokenExpirySeconds + 1);
    }

    @Test
    void testToken_WithSingleAuthority()
    {
        // Arrange
        Collection<GrantedAuthority> singleAuthority = Arrays.asList(
            new SimpleGrantedAuthority("ROLE_USER")
        );

        when(authentication.getName()).thenReturn(testUsername);
        when(authentication.getAuthorities()).thenReturn((Collection) singleAuthority);
        when(jwtEncoder.encode(any(JwtEncoderParameters.class))).thenReturn(jwt);
        when(jwt.getTokenValue()).thenReturn("token");

        // Act
        ResponseEntity<TokenResponse> response = tokenApi.token(authentication);

        // Assert
        assertNotNull(response);
        assertEquals(200, response.getStatusCode().value());
        verify(jwtEncoder).encode(any(JwtEncoderParameters.class));
    }

    @Test
    void testToken_WithNoAuthorities()
    {
        // Arrange
        when(authentication.getName()).thenReturn(testUsername);
        when(authentication.getAuthorities()).thenReturn((Collection) Arrays.asList());
        when(jwtEncoder.encode(any(JwtEncoderParameters.class))).thenReturn(jwt);
        when(jwt.getTokenValue()).thenReturn("token");

        // Act
        ResponseEntity<TokenResponse> response = tokenApi.token(authentication);

        // Assert
        assertNotNull(response);
        assertEquals(200, response.getStatusCode().value());
    }

    @Test
    void testToken_VerifyJwtEncoderCalled()
    {
        // Arrange
        when(authentication.getName()).thenReturn(testUsername);
        when(authentication.getAuthorities()).thenReturn((Collection) authorities);
        when(jwtEncoder.encode(any(JwtEncoderParameters.class))).thenReturn(jwt);
        when(jwt.getTokenValue()).thenReturn("token");

        // Act
        tokenApi.token(authentication);

        // Assert
        verify(jwtEncoder, times(1)).encode(any(JwtEncoderParameters.class));
    }

    @Test
    void testToken_DifferentUsers_ShouldGenerateDifferentTokens()
    {
        // Arrange
        String user1 = "user1";
        String user2 = "user2";
        String token1 = "token1";
        String token2 = "token2";

        when(authentication.getAuthorities()).thenReturn((Collection) authorities);
        when(jwtEncoder.encode(any(JwtEncoderParameters.class))).thenReturn(jwt);

        // First user
        when(authentication.getName()).thenReturn(user1);
        when(jwt.getTokenValue()).thenReturn(token1);
        ResponseEntity<TokenResponse> response1 = tokenApi.token(authentication);

        // Second user
        when(authentication.getName()).thenReturn(user2);
        when(jwt.getTokenValue()).thenReturn(token2);
        ResponseEntity<TokenResponse> response2 = tokenApi.token(authentication);

        // Assert
        assertNotNull(response1.getBody());
        assertNotNull(response2.getBody());
        assertEquals(token1, response1.getBody().getToken());
        assertEquals(token2, response2.getBody().getToken());
        verify(jwtEncoder, times(2)).encode(any(JwtEncoderParameters.class));
    }

    @Test
    void testToken_ResponseBodyContainsAllRequiredFields()
    {
        // Arrange
        when(authentication.getName()).thenReturn(testUsername);
        when(authentication.getAuthorities()).thenReturn((Collection) authorities);
        when(jwtEncoder.encode(any(JwtEncoderParameters.class))).thenReturn(jwt);
        when(jwt.getTokenValue()).thenReturn("token");

        // Act
        ResponseEntity<TokenResponse> response = tokenApi.token(authentication);

        // Assert
        TokenResponse body = response.getBody();
        assertNotNull(body);
        assertNotNull(body.getToken());
        assertNotNull(body.getCreatedAt());
        assertNotNull(body.getExpiresAt());
        assertTrue(body.getToken().length() > 0);
    }
}

