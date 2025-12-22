package com.logpilot.server.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

public class ApiKeyAuthFilter extends OncePerRequestFilter {

    private final String headerName;
    private final String validApiKey;

    public ApiKeyAuthFilter(String headerName, String validApiKey) {
        this.headerName = headerName;
        this.validApiKey = validApiKey;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        
        String apiKey = request.getHeader(headerName);

        if (apiKey != null && apiKey.equals(validApiKey)) {
            Authentication auth = new UsernamePasswordAuthenticationToken(
                    "api-user", apiKey, AuthorityUtils.createAuthorityList("ROLE_API_CLIENT"));
            SecurityContextHolder.getContext().setAuthentication(auth);
        }

        filterChain.doFilter(request, response);
    }
}
