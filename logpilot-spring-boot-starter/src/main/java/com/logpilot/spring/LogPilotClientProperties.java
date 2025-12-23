package com.logpilot.spring;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "logpilot.client")
public class LogPilotClientProperties {
    // LogPilot 서버의 URL (예: http://localhost:8080)
    // URL of the LogPilot Server (e.g., http://localhost:8080)
    private String serverUrl = "http://localhost:8080";

    // 로그의 비동기 배치 전송 활성화 여부
    // Enable asynchronous batching of logs.
    private boolean enableBatching = true;

    // 플러시 전 버퍼링할 로그의 개수
    // Number of logs to buffer before flushing.
    private int batchSize = 100;

    // 버퍼 플러시 전 최대 대기 시간 (밀리초 단위)
    // Max time to wait before flushing buffer (in milliseconds).
    private long flushIntervalMillis = 5000;

    // 인증을 위한 API 키
    // API Key for authentication.
    private String apiKey;

    // Getters and Setters
    public String getServerUrl() {
        return serverUrl;
    }

    public void setServerUrl(String serverUrl) {
        this.serverUrl = serverUrl;
    }

    public boolean isEnableBatching() {
        return enableBatching;
    }

    public void setEnableBatching(boolean enableBatching) {
        this.enableBatching = enableBatching;
    }

    public int getBatchSize() {
        return batchSize;
    }

    public void setBatchSize(int batchSize) {
        this.batchSize = batchSize;
    }

    public long getFlushIntervalMillis() {
        return flushIntervalMillis;
    }

    public void setFlushIntervalMillis(long flushIntervalMillis) {
        this.flushIntervalMillis = flushIntervalMillis;
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }
}
