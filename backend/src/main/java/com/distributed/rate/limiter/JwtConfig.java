package com.distributed.rate.limiter;

import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.server.resource.web.BearerTokenAuthenticationEntryPoint;
import org.springframework.security.oauth2.server.resource.web.access.BearerTokenAccessDeniedHandler;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;

@Configuration
public class JwtConfig
{
    @Value("${jwt.public.key}")
    RSAPublicKey publicKey;

    @Value("${jwt.private.key}")
    RSAPrivateKey privateKey;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception
    {
        // @formatter:off
        http
            .authorizeHttpRequests((authorize) -> authorize
                .anyRequest().authenticated()
            )
            .csrf((csrf) -> csrf.ignoringRequestMatchers("/token"))
            .httpBasic(Customizer.withDefaults())
            .oauth2ResourceServer((jwt) -> jwt.jwt(Customizer.withDefaults()))
            .sessionManagement((session) -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .exceptionHandling((exceptions) -> exceptions
                .authenticationEntryPoint(new BearerTokenAuthenticationEntryPoint())
                .accessDeniedHandler(new BearerTokenAccessDeniedHandler())
            );
        // @formatter:on
        return http.build();
    }

    @Bean
    UserDetailsService users()
    {
        // @formatter:off
        return new InMemoryUserDetailsManager(
            User.withUsername("user1").password("{noop}password").authorities("app").build(),
            User.withUsername("user2").password("{noop}password").authorities("app").build(),
            User.withUsername("user3").password("{noop}password").authorities("app").build(),
            User.withUsername("user4").password("{noop}password").authorities("app").build(),
            User.withUsername("user5").password("{noop}password").authorities("app").build(),
            User.withUsername("user6").password("{noop}password").authorities("app").build(),
            User.withUsername("user7").password("{noop}password").authorities("app").build(),
            User.withUsername("user8").password("{noop}password").authorities("app").build(),
            User.withUsername("user9").password("{noop}password").authorities("app").build(),
            User.withUsername("user10").password("{noop}password").authorities("app").build()
        );
        // @formatter:on
    }

    @Bean
    JwtDecoder jwtDecoder()
    {
        return NimbusJwtDecoder.withPublicKey(this.publicKey).build();
    }

    @Bean
    JwtEncoder jwtEncoder()
    {
        JWK jwk = new RSAKey.Builder(this.publicKey).privateKey(this.privateKey).build();
        JWKSource<SecurityContext> jwks = new ImmutableJWKSet<>(new JWKSet(jwk));
        return new NimbusJwtEncoder(jwks);
    }

}
