package com.logpilot.server.config;

import com.logpilot.server.grpc.interceptor.ApiKeyGrpcInterceptor;
import net.devh.boot.grpc.server.interceptor.GrpcGlobalServerInterceptor;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GrpcSecurityConfig {

    @GrpcGlobalServerInterceptor
    public ApiKeyGrpcInterceptor apiKeyGrpcInterceptor(ApiKeyGrpcInterceptor interceptor) {
        return interceptor;
    }
}
