package com.logpilot.server.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import java.time.Duration;
import java.util.function.Supplier;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @org.springframework.beans.factory.annotation.Value("${logpilot.server.api-key:logpilot-secret-key-123}")
    private String apiKeyValue;

    @org.springframework.beans.factory.annotation.Value("${logpilot.server.rate-limit.capacity:100}")
    private int capacity;

    @org.springframework.beans.factory.annotation.Value("${logpilot.server.rate-limit.refill-tokens:100}")
    private int refillTokens;

    @org.springframework.beans.factory.annotation.Value("${logpilot.server.rate-limit.refill-duration-seconds:60}")
    private int refillDurationSeconds;

    private static final String API_KEY_HEADER = "X-API-KEY";

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/logs/**").authenticated()
                .requestMatchers("/actuator/**").permitAll()
                .anyRequest().permitAll()
            )
            .addFilterBefore(new ApiKeyAuthFilter(API_KEY_HEADER, apiKeyValue), UsernamePasswordAuthenticationFilter.class)
            .addFilterAfter(new RateLimitFilter(createBucketSupplier()), ApiKeyAuthFilter.class);

        return http.build();
    }

    private Supplier<Bucket> createBucketSupplier() {
        return () -> Bucket.builder()
                .addLimit(Bandwidth.classic(capacity, Refill.greedy(refillTokens, Duration.ofSeconds(refillDurationSeconds))))
                .build();
    }
}
