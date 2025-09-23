package com.logpilot.client.grpc;

import com.logpilot.client.LogPilotClient;
import com.logpilot.core.model.LogEntry;
import com.logpilot.core.model.LogLevel;
import com.logpilot.grpc.proto.LogPilotProto.*;
import static com.logpilot.grpc.proto.LogPilotProto.*;
import com.logpilot.grpc.proto.LogServiceGrpc;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.StatusRuntimeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
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
    private final LogServiceGrpc.LogServiceBlockingStub blockingStub;
    private final ExecutorService executorService;
    private final int maxRetries;

    public LogPilotGrpcClient(String serverUrl, int timeout, int maxRetries) {
        this.maxRetries = maxRetries;

        String[] parts = serverUrl.replace("http://", "").replace("https://", "").split(":");
        String host = parts[0];
        int port = parts.length > 1 ? Integer.parseInt(parts[1]) : 50051;

        this.channel = ManagedChannelBuilder.forAddress(host, port)
                .usePlaintext()
                .keepAliveTime(30, TimeUnit.SECONDS)
                .keepAliveTimeout(5, TimeUnit.SECONDS)
                .keepAliveWithoutCalls(true)
                .build();

        this.blockingStub = LogServiceGrpc.newBlockingStub(channel);
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
            LogRequest request = LogRequest.newBuilder()
                    .setChannel(logEntry.getChannel())
                    .setLevel(logEntry.getLevel().toString())
                    .setMessage(logEntry.getMessage())
                    .putAllMeta(convertMetaToStringMap(logEntry.getMeta()))
                    .setStorage("sqlite") // Default storage
                    .build();

            executeWithRetry(() -> {
                LogResponse response = blockingStub.sendLog(request);
                if (!"success".equals(response.getStatus())) {
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
            List<LogRequest> logRequests = logEntries.stream()
                    .map(logEntry -> LogRequest.newBuilder()
                            .setChannel(logEntry.getChannel())
                            .setLevel(logEntry.getLevel().toString())
                            .setMessage(logEntry.getMessage())
                            .putAllMeta(convertMetaToStringMap(logEntry.getMeta()))
                            .setStorage("sqlite") // Default storage
                            .build())
                    .collect(Collectors.toList());

            SendLogsRequest request = SendLogsRequest.newBuilder()
                    .addAllLogRequests(logRequests)
                    .build();

            executeWithRetry(() -> {
                SendLogsResponse response = blockingStub.sendLogs(request);
                if (!"success".equals(response.getStatus())) {
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
            FetchLogsRequest request = FetchLogsRequest.newBuilder()
                    .setSince(consumerId != null ? consumerId : "")
                    .setChannel(channel)
                    .setLimit(limit)
                    .setStorage("sqlite") // Default storage
                    .build();

            return executeWithRetry(() -> {
                FetchLogsResponse response = blockingStub.fetchLogs(request);
                return response.getLogsList().stream()
                        .map(this::convertProtoLogEntryToLogEntry)
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
            FetchLogsRequest request = FetchLogsRequest.newBuilder()
                    .setSince("")
                    .setChannel("")
                    .setLimit(limit)
                    .setStorage("sqlite") // Default storage
                    .build();

            return executeWithRetry(() -> {
                FetchLogsResponse response = blockingStub.fetchLogs(request);
                return response.getLogsList().stream()
                        .map(this::convertProtoLogEntryToLogEntry)
                        .collect(Collectors.toList());
            });
        } catch (Exception e) {
            logger.error("Failed to get all logs via gRPC", e);
            throw new RuntimeException("Failed to get all logs via gRPC", e);
        }
    }

    private Map<String, String> convertMetaToStringMap(Map<String, Object> meta) {
        if (meta == null) {
            return new HashMap<>();
        }
        return meta.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> entry.getValue().toString()
                ));
    }

    private LogEntry convertProtoLogEntryToLogEntry(com.logpilot.grpc.proto.LogPilotProto.LogEntry protoLogEntry) {
        LogEntry logEntry = new LogEntry();
        logEntry.setChannel(protoLogEntry.getChannel());
        logEntry.setLevel(convertStringToLogLevel(protoLogEntry.getLevel()));
        logEntry.setMessage(protoLogEntry.getMessage());

        if (!protoLogEntry.getMetaMap().isEmpty()) {
            Map<String, Object> meta = new HashMap<>(protoLogEntry.getMetaMap());
            logEntry.setMeta(meta);
        }

        // Convert timestamp from long to LocalDateTime
        if (protoLogEntry.getTimestamp() > 0) {
            try {
                LocalDateTime timestamp = LocalDateTime.ofInstant(
                    java.time.Instant.ofEpochMilli(protoLogEntry.getTimestamp()),
                    java.time.ZoneOffset.UTC
                );
                logEntry.setTimestamp(timestamp);
            } catch (Exception e) {
                logger.warn("Failed to parse timestamp: {}", protoLogEntry.getTimestamp(), e);
                logEntry.setTimestamp(LocalDateTime.now());
            }
        } else {
            logEntry.setTimestamp(LocalDateTime.now());
        }

        return logEntry;
    }

    private LogLevel convertStringToLogLevel(String levelString) {
        try {
            return LogLevel.valueOf(levelString.toUpperCase());
        } catch (IllegalArgumentException e) {
            logger.warn("Unknown log level: {}, defaulting to INFO", levelString);
            return LogLevel.INFO;
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
                    Thread.sleep(1000L * attempt);
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