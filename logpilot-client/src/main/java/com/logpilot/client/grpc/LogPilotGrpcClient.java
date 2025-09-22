package com.logpilot.client.grpc;

import com.logpilot.client.LogPilotClient;
import com.logpilot.core.model.LogEntry;
import com.logpilot.core.model.LogLevel;
import com.logpilot.grpc.proto.LogPilotProto.*;
import com.logpilot.grpc.proto.LogPilotServiceGrpc;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.StatusRuntimeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class LogPilotGrpcClient implements LogPilotClient {

    private static final Logger logger = LoggerFactory.getLogger(LogPilotGrpcClient.class);
    private final ManagedChannel channel;
    private final LogPilotServiceGrpc.LogPilotServiceBlockingStub blockingStub;
    private final ExecutorService executorService;
    private final int maxRetries;

    public LogPilotGrpcClient(String serverUrl, int timeout, int maxRetries) {
        this.maxRetries = maxRetries;

        // Parse host and port from serverUrl
        String[] parts = serverUrl.replace("http://", "").replace("https://", "").split(":");
        String host = parts[0];
        int port = parts.length > 1 ? Integer.parseInt(parts[1]) : 9090;

        this.channel = ManagedChannelBuilder.forAddress(host, port)
                .usePlaintext()
                .keepAliveTime(30, TimeUnit.SECONDS)
                .keepAliveTimeout(5, TimeUnit.SECONDS)
                .keepAliveWithoutCalls(true)
                .build();

        this.blockingStub = LogPilotServiceGrpc.newBlockingStub(channel);
        this.executorService = Executors.newCachedThreadPool();
    }

    @Override
    public void log(String channel, LogLevel level, String message) {
        log(new LogEntry(channel, level, message));
    }

    @Override
    public void log(String channel, LogLevel level, String message, Map<String, Object> meta) {
        log(new LogEntry(channel, level, message, meta));
    }

    @Override
    public void log(LogEntry logEntry) {
        try {
            com.logpilot.grpc.proto.LogPilotProto.LogEntry protoLogEntry = convertToProtoLogEntry(logEntry);
            StoreLogRequest request = StoreLogRequest.newBuilder()
                    .setLogEntry(protoLogEntry)
                    .build();

            executeWithRetry(() -> {
                StoreLogResponse response = blockingStub.storeLog(request);
                if (!response.getSuccess()) {
                    throw new RuntimeException("Failed to store log: " + response.getMessage());
                }
                return null;
            });

            logger.debug("Sent log entry via gRPC for channel: {}", logEntry.getChannel());
        } catch (Exception e) {
            logger.error("Failed to send log entry via gRPC", e);
            throw new RuntimeException("Failed to send log entry via gRPC", e);
        }
    }

    @Override
    public void logBatch(List<LogEntry> logEntries) {
        try {
            List<com.logpilot.grpc.proto.LogPilotProto.LogEntry> protoLogEntries = logEntries.stream()
                    .map(this::convertToProtoLogEntry)
                    .collect(Collectors.toList());

            StoreLogsRequest request = StoreLogsRequest.newBuilder()
                    .addAllLogEntries(protoLogEntries)
                    .build();

            executeWithRetry(() -> {
                StoreLogsResponse response = blockingStub.storeLogs(request);
                if (!response.getSuccess()) {
                    throw new RuntimeException("Failed to store logs: " + response.getMessage());
                }
                return null;
            });

            logger.debug("Sent {} log entries via gRPC", logEntries.size());
        } catch (Exception e) {
            logger.error("Failed to send batch log entries via gRPC", e);
            throw new RuntimeException("Failed to send batch log entries via gRPC", e);
        }
    }

    @Override
    public CompletableFuture<Void> logAsync(String channel, LogLevel level, String message) {
        return logAsync(new LogEntry(channel, level, message));
    }

    @Override
    public CompletableFuture<Void> logAsync(String channel, LogLevel level, String message, Map<String, Object> meta) {
        return logAsync(new LogEntry(channel, level, message, meta));
    }

    @Override
    public CompletableFuture<Void> logAsync(LogEntry logEntry) {
        return CompletableFuture.runAsync(() -> log(logEntry), executorService);
    }

    @Override
    public CompletableFuture<Void> logBatchAsync(List<LogEntry> logEntries) {
        return CompletableFuture.runAsync(() -> logBatch(logEntries), executorService);
    }

    @Override
    public List<LogEntry> getLogs(String channel, String consumerId, int limit) {
        try {
            GetLogsRequest request = GetLogsRequest.newBuilder()
                    .setChannel(channel)
                    .setConsumerId(consumerId != null ? consumerId : "")
                    .setLimit(limit)
                    .build();

            return executeWithRetry(() -> {
                GetLogsResponse response = blockingStub.getLogs(request);
                return response.getLogEntriesList().stream()
                        .map(this::convertToLogEntry)
                        .collect(Collectors.toList());
            });
        } catch (Exception e) {
            logger.error("Failed to get logs via gRPC", e);
            throw new RuntimeException("Failed to get logs via gRPC", e);
        }
    }

    @Override
    public List<LogEntry> getAllLogs(int limit) {
        try {
            GetLogsRequest request = GetLogsRequest.newBuilder()
                    .setChannel("")
                    .setConsumerId("")
                    .setLimit(limit)
                    .build();

            return executeWithRetry(() -> {
                GetLogsResponse response = blockingStub.getLogs(request);
                return response.getLogEntriesList().stream()
                        .map(this::convertToLogEntry)
                        .collect(Collectors.toList());
            });
        } catch (Exception e) {
            logger.error("Failed to get all logs via gRPC", e);
            throw new RuntimeException("Failed to get all logs via gRPC", e);
        }
    }

    private com.logpilot.grpc.proto.LogPilotProto.LogEntry convertToProtoLogEntry(LogEntry logEntry) {
        com.logpilot.grpc.proto.LogPilotProto.LogEntry.Builder builder =
            com.logpilot.grpc.proto.LogPilotProto.LogEntry.newBuilder()
                .setChannel(logEntry.getChannel())
                .setLevel(convertToProtoLogLevel(logEntry.getLevel()))
                .setMessage(logEntry.getMessage())
                .setTimestamp(logEntry.getTimestamp().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));

        if (logEntry.getMeta() != null) {
            Map<String, String> stringMeta = logEntry.getMeta().entrySet().stream()
                    .collect(Collectors.toMap(
                            Map.Entry::getKey,
                            entry -> entry.getValue().toString()
                    ));
            builder.putAllMeta(stringMeta);
        }

        return builder.build();
    }

    private LogEntry convertToLogEntry(com.logpilot.grpc.proto.LogPilotProto.LogEntry protoLogEntry) {
        LogEntry logEntry = new LogEntry();
        logEntry.setChannel(protoLogEntry.getChannel());
        logEntry.setLevel(convertToLogLevel(protoLogEntry.getLevel()));
        logEntry.setMessage(protoLogEntry.getMessage());

        if (!protoLogEntry.getMetaMap().isEmpty()) {
            Map<String, Object> meta = new HashMap<>(protoLogEntry.getMetaMap());
            logEntry.setMeta(meta);
        }

        if (!protoLogEntry.getTimestamp().isEmpty()) {
            try {
                LocalDateTime timestamp = LocalDateTime.parse(protoLogEntry.getTimestamp(), DateTimeFormatter.ISO_LOCAL_DATE_TIME);
                logEntry.setTimestamp(timestamp);
            } catch (Exception e) {
                logger.warn("Failed to parse timestamp: {}", protoLogEntry.getTimestamp(), e);
            }
        }

        return logEntry;
    }

    private LogLevel convertToLogLevel(com.logpilot.grpc.proto.LogPilotProto.LogLevel protoLogLevel) {
        switch (protoLogLevel) {
            case DEBUG: return LogLevel.DEBUG;
            case INFO: return LogLevel.INFO;
            case WARN: return LogLevel.WARN;
            case ERROR: return LogLevel.ERROR;
            default: throw new IllegalArgumentException("Unknown log level: " + protoLogLevel);
        }
    }

    private com.logpilot.grpc.proto.LogPilotProto.LogLevel convertToProtoLogLevel(LogLevel logLevel) {
        switch (logLevel) {
            case DEBUG: return com.logpilot.grpc.proto.LogPilotProto.LogLevel.DEBUG;
            case INFO: return com.logpilot.grpc.proto.LogPilotProto.LogLevel.INFO;
            case WARN: return com.logpilot.grpc.proto.LogPilotProto.LogLevel.WARN;
            case ERROR: return com.logpilot.grpc.proto.LogPilotProto.LogLevel.ERROR;
            default: throw new IllegalArgumentException("Unknown log level: " + logLevel);
        }
    }

    private <T> T executeWithRetry(RetryableOperation<T> operation) throws Exception {
        Exception lastException = null;

        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                return operation.execute();
            } catch (StatusRuntimeException e) {
                lastException = e;
                if (attempt < maxRetries) {
                    logger.warn("gRPC attempt {} failed, retrying... Error: {}", attempt, e.getStatus());
                    Thread.sleep(1000 * attempt); // Exponential backoff
                }
            }
        }

        throw new RuntimeException("All gRPC retry attempts failed", lastException);
    }

    @Override
    public void close() {
        try {
            executorService.shutdown();
            channel.shutdown();
            if (!channel.awaitTermination(5, TimeUnit.SECONDS)) {
                channel.shutdownNow();
            }
            logger.info("LogPilotGrpcClient closed");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            channel.shutdownNow();
            logger.warn("Interrupted while closing gRPC client", e);
        }
    }

    @FunctionalInterface
    private interface RetryableOperation<T> {
        T execute() throws StatusRuntimeException;
    }
}