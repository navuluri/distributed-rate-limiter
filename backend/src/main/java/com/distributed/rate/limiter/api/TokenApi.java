package com.distributed.rate.limiter.api;

import com.distributed.rate.limiter.models.TokenResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.stream.Collectors;

@RestController
public class TokenApi
{
    @Value("${jwt.token.expiry.seconds:86400}")
    private long tokenExpirySeconds;

    private final JwtEncoder encoder;

    public TokenApi(JwtEncoder encoder)
    {
        this.encoder = encoder;
    }

    @PostMapping("/token")
    public ResponseEntity<TokenResponse> token(Authentication authentication)
    {
        Instant now = Instant.now();
        // @formatter:off
        String scope = authentication.getAuthorities().stream()
            .map(GrantedAuthority::getAuthority)
            .collect(Collectors.joining(" "));
        JwtClaimsSet claims = JwtClaimsSet.builder()
            .issuer("self")
            .issuedAt(now)
            .expiresAt(now.plusSeconds(tokenExpirySeconds))
            .subject(authentication.getName())
            .claim("scope", scope)
            .build();
        // @formatter:on
        String token = this.encoder.encode(JwtEncoderParameters.from(claims)).getTokenValue();
        HttpHeaders headers = new HttpHeaders();
        headers.add("X-JWT-Token", token);
        TokenResponse response = TokenResponse.builder()
                                              .token(token)
                                              .createdAt(now.getEpochSecond() * 1000)
                                              .expiresAt((now.plusSeconds(tokenExpirySeconds).getEpochSecond()) * 1000)
                                              .build();
        return ResponseEntity.ok().headers(headers).body(response);
    }

}
