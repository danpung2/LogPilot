package com.logpilot.client.rest;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.logpilot.client.LogPilotClient;
import com.logpilot.core.model.LogEntry;
import com.logpilot.core.model.LogLevel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

public class LogPilotRestClient implements LogPilotClient {

    private static final Logger logger = LoggerFactory.getLogger(LogPilotRestClient.class);
    private final String serverUrl;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final ExecutorService executorService;
    private final BlockingQueue<LogEntry> logQueue;
    private final ScheduledExecutorService scheduler;
    private final boolean enableBatching;
    private final int batchSize;
    private final int maxRetries;

    public LogPilotRestClient(String serverUrl, int timeout, int maxRetries) {
        this(serverUrl, timeout, maxRetries, false, 100, 5000);
    }

    public LogPilotRestClient(String serverUrl, int timeout, int maxRetries, boolean enableBatching, int batchSize, long flushIntervalMillis) {
        this.serverUrl = serverUrl.endsWith("/") ? serverUrl.substring(0, serverUrl.length() - 1) : serverUrl;
        this.maxRetries = maxRetries;
        this.enableBatching = enableBatching;
        this.batchSize = batchSize;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(timeout))
                .build();
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
        this.executorService = Executors.newCachedThreadPool();

        if (enableBatching) {
            this.logQueue = new LinkedBlockingQueue<>();
            this.scheduler = Executors.newSingleThreadScheduledExecutor();
            this.scheduler.scheduleAtFixedRate(this::flush, flushIntervalMillis, flushIntervalMillis, TimeUnit.MILLISECONDS);
        } else {
            this.logQueue = null;
            this.scheduler = null;
        }
    }

    // 스케줄링 및 플러시 테스트를 위한 생성자
    LogPilotRestClient(String serverUrl, HttpClient httpClient, ScheduledExecutorService scheduler, boolean enableBatching, int batchSize, int maxRetries) {
        this.serverUrl = serverUrl.endsWith("/") ? serverUrl.substring(0, serverUrl.length() - 1) : serverUrl;
        this.httpClient = httpClient;
        this.scheduler = scheduler;
        this.enableBatching = enableBatching;
        this.batchSize = batchSize;
        this.maxRetries = maxRetries;
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
        this.executorService = Executors.newCachedThreadPool();
        this.logQueue = enableBatching ? new LinkedBlockingQueue<>() : null;
        
        if (enableBatching && scheduler != null) {
        }
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
        if (enableBatching) {
            queueLog(logEntry);
        } else {
            try {
                sendLogRequest(logEntry);
            } catch (Exception e) {
                logger.error("Failed to send log entry", e);
                throw new RuntimeException("Failed to send log entry", e);
            }

        }
    }

    @Override
    public void logBatch(List<LogEntry> logEntries) {
        try {
            sendBatchLogRequest(logEntries);
        } catch (Exception e) {
            logger.error("Failed to send batch log entries", e);
            throw new RuntimeException("Failed to send batch log entries", e);
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

    private void queueLog(LogEntry logEntry) {
        try {
            logQueue.put(logEntry);
            if (logQueue.size() >= batchSize) {
                executorService.submit(this::flush);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.error("Failed to queue log entry", e);
        }
    }

    private synchronized void flush() {
        if (logQueue.isEmpty()) {
            return;
        }

        List<LogEntry> batch = new ArrayList<>();
        logQueue.drainTo(batch, batchSize);

        if (!batch.isEmpty()) {
            try {
                sendBatchLogRequest(batch);
            } catch (Exception e) {
                logger.error("Failed to send batch log entries during flush", e);
            }
        }
    }

    @Override
    public CompletableFuture<Void> logBatchAsync(List<LogEntry> logEntries) {
        return CompletableFuture.runAsync(() -> logBatch(logEntries), executorService);
    }

    @Override
    public List<LogEntry> getLogs(String channel, String consumerId, int limit) {
        try {
            String url = String.format("%s/api/v1/logs/%s?consumerId=%s&limit=%d",
                    serverUrl, channel, consumerId, limit);
            return sendGetRequest(url);
        } catch (Exception e) {
            logger.error("Failed to get logs", e);
            throw new RuntimeException("Failed to get logs", e);
        }
    }

    @Override
    public List<LogEntry> getAllLogs(int limit) {
        try {
            String url = String.format("%s/api/v1/logs?limit=%d", serverUrl, limit);
            return sendGetRequest(url);
        } catch (Exception e) {
            logger.error("Failed to get all logs", e);
            throw new RuntimeException("Failed to get all logs", e);
        }
    }

    private void sendLogRequest(LogEntry logEntry) throws Exception {
        String json = objectMapper.writeValueAsString(logEntry);
        String url = serverUrl + "/api/v1/logs";

        makeRequest(json, url);

        logger.debug("Sent log entry to {}", url);
    }

    private void sendBatchLogRequest(List<LogEntry> logEntries) throws Exception {
        String json = objectMapper.writeValueAsString(logEntries);
        String url = serverUrl + "/api/v1/logs/batch";

        makeRequest(json, url);

        logger.debug("Sent {} log entries to {}", logEntries.size(), url);
    }

    private void makeRequest(String json, String url) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();

        executeWithRetry(() -> {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 400) {
                throw new RuntimeException("HTTP " + response.statusCode() + ": " + response.body());
            }
            return null;
        });
    }

    private List<LogEntry> sendGetRequest(String url) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Accept", "application/json")
                .GET()
                .build();

        return executeWithRetry(() -> {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 400) {
                throw new RuntimeException("HTTP " + response.statusCode() + ": " + response.body());
            }
            return objectMapper.readValue(response.body(), new TypeReference<List<LogEntry>>() {});
        });
    }

    private <T> T executeWithRetry(RetryableOperation<T> operation) throws Exception {
        Exception lastException = null;

        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                return operation.execute();
            } catch (Exception e) {
                lastException = e;
                if (attempt < maxRetries) {
                    logger.warn("Attempt {} failed, retrying... Error: {}", attempt, e.getMessage());
                    Thread.sleep(1000L * attempt);
                }
            }
        }

        throw new RuntimeException("All retry attempts failed", lastException);
    }

    @Override
    public void close() {
        if (enableBatching) {
            flush();
            scheduler.shutdown();
            try {
                if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                    scheduler.shutdownNow();
                }
            } catch (InterruptedException e) {
                scheduler.shutdownNow();
            }
        }
        executorService.shutdown();
        logger.info("LogPilotRestClient closed");
    }

    @FunctionalInterface
    private interface RetryableOperation<T> {
        T execute() throws IOException, InterruptedException;
    }
}