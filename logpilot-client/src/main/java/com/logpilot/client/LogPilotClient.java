package com.logpilot.client;

import com.logpilot.client.grpc.LogPilotGrpcClient;
import com.logpilot.client.rest.LogPilotRestClient;
import com.logpilot.core.model.LogEntry;
import com.logpilot.core.model.LogLevel;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public interface LogPilotClient extends AutoCloseable {

    void log(String channel, LogLevel level, String message);

    void log(String channel, LogLevel level, String message, Map<String, Object> meta);

    void log(LogEntry logEntry);

    void logBatch(List<LogEntry> logEntries);

    CompletableFuture<Void> logAsync(String channel, LogLevel level, String message);

    CompletableFuture<Void> logAsync(String channel, LogLevel level, String message, Map<String, Object> meta);

    CompletableFuture<Void> logAsync(LogEntry logEntry);

    CompletableFuture<Void> logBatchAsync(List<LogEntry> logEntries);

    List<LogEntry> getLogs(String channel, String consumerId, int limit);

    List<LogEntry> getAllLogs(int limit);

    @Override
    void close();

    static Builder builder() {
        return new Builder();
    }

    class Builder {
        private String serverUrl;
        private ClientType clientType = ClientType.REST;
        private int timeout = 5000;
        private int maxRetries = 3;

        private boolean enableBatching = false;
        private int batchSize = 100;
        private long flushIntervalMillis = 5000;

        public Builder serverUrl(String serverUrl) {
            this.serverUrl = serverUrl;
            return this;
        }

        public Builder clientType(ClientType clientType) {
            this.clientType = clientType;
            return this;
        }

        public Builder timeout(int timeout) {
            this.timeout = timeout;
            return this;
        }

        public Builder maxRetries(int maxRetries) {
            this.maxRetries = maxRetries;
            return this;
        }

        public Builder enableBatching(boolean enableBatching) {
            this.enableBatching = enableBatching;
            return this;
        }

        public Builder batchSize(int batchSize) {
            this.batchSize = batchSize;
            return this;
        }

        public Builder flushIntervalMillis(long flushIntervalMillis) {
            this.flushIntervalMillis = flushIntervalMillis;
            return this;
        }

        public LogPilotClient build() {
            if (serverUrl == null || serverUrl.trim().isEmpty()) {
                throw new IllegalArgumentException("Server URL is required");
            }

            return switch (clientType) {
                case REST -> new LogPilotRestClient(serverUrl, timeout, maxRetries, enableBatching, batchSize, flushIntervalMillis);
                case GRPC -> new LogPilotGrpcClient(serverUrl, timeout, maxRetries);
            };
        }
    }

    enum ClientType {
        REST,
        GRPC
    }
}