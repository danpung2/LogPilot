package com.logpilot.server.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @org.springframework.beans.factory.annotation.Value("${logpilot.server.api-key:logpilot-secret-key-123}")
    private String apiKeyValue;

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
            .addFilterBefore(new ApiKeyAuthFilter(API_KEY_HEADER, apiKeyValue), UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
