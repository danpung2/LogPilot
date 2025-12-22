package com.logpilot.spring;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "logpilot.client")
public class LogPilotClientProperties {
    /**
     * URL of the LogPilot Server (e.g., http://localhost:8080)
     */
    private String serverUrl = "http://localhost:8080";

    /**
     * Enable asynchronous batching of logs
     */
    private boolean enableBatching = true;

    /**
     * Number of logs to buffer before flushing
     */
    private int batchSize = 100;

    /**
     * Max time to wait before flushing buffer (in milliseconds)
     */
    private long flushIntervalMillis = 5000;

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
}
