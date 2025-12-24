package com.logpilot.server.grpc.interceptor;

import io.grpc.*;
import net.devh.boot.grpc.server.interceptor.GrpcGlobalServerInterceptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Component
@GrpcGlobalServerInterceptor
public class ApiKeyGrpcInterceptor implements ServerInterceptor {

    private static final Logger logger = LoggerFactory.getLogger(ApiKeyGrpcInterceptor.class);
    private static final String API_KEY_HEADER = "X-API-KEY";
    private static final Metadata.Key<String> API_KEY_METADATA_KEY = Metadata.Key.of(API_KEY_HEADER,
            Metadata.ASCII_STRING_MARSHALLER);

    private final String expectedApiKey;

    public ApiKeyGrpcInterceptor(@Value("${logpilot.server.api-key:logpilot-secret-key-123}") String expectedApiKey) {
        this.expectedApiKey = expectedApiKey;
    }

    @Override
    public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(
            ServerCall<ReqT, RespT> call,
            Metadata headers,
            ServerCallHandler<ReqT, RespT> next) {

        String apiKey = headers.get(API_KEY_METADATA_KEY);

        if (apiKey == null || !Objects.equals(apiKey, expectedApiKey)) {
            logger.warn("gRPC authentication failed. Missing or invalid API Key.");
            call.close(Status.UNAUTHENTICATED.withDescription("Invalid or missing API Key"), headers);
            return new ServerCall.Listener<>() {
            };
        }

        return next.startCall(call, headers);
    }
}
