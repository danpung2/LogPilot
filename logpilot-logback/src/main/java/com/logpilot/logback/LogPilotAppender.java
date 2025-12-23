package com.logpilot.logback;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.UnsynchronizedAppenderBase;
import com.logpilot.client.LogPilotClient;
import com.logpilot.core.model.LogLevel;

import java.util.Map;

public class LogPilotAppender extends UnsynchronizedAppenderBase<ILoggingEvent> {

    private String serverUrl = "http://localhost:8080";
    private String serviceName = "default-service";
    private boolean enableBatching = false;
    private int batchSize = 100;
    private long flushIntervalMillis = 5000;
    private String apiKey;

    private LogPilotClient client;

    @Override
    public void start() {
        if (serverUrl == null || serverUrl.isEmpty()) {
            addError("Server URL is required for LogPilotAppender");
            return;
        }

        try {
            client = LogPilotClient.builder()
                    .serverUrl(serverUrl)
                    .enableBatching(enableBatching)
                    .batchSize(batchSize)
                    .flushIntervalMillis(flushIntervalMillis)
                    .apiKey(apiKey)
                    .build();
            super.start();
        } catch (Exception e) {
            addError("Failed to initialize LogPilotClient", e);
        }
    }

    @Override
    protected void append(ILoggingEvent event) {
        if (client == null) {
            return;
        }

        LogLevel level = mapLogLevel(event.getLevel());
        String message = event.getFormattedMessage();

        Map<String, Object> meta = new java.util.HashMap<>();
        meta.put("logger", event.getLoggerName());
        meta.put("thread", event.getThreadName());

        Map<String, String> mdc = event.getMDCPropertyMap();
        if (mdc != null && !mdc.isEmpty()) {
            meta.putAll(mdc);
        }

        // 배치 활성화 시: 내부 큐에 쌓으며 비동기 처리됨 (Fast)
        // 배치 비활성화 시: HTTP 요청을 동기로 보냄 (Blocking)
        // 현재는 클라이언트 설정에 따라 동작

        client.log(serviceName, level, message, meta);
    }

    @Override
    public void stop() {
        if (client != null) {
            try {
                client.close();
            } catch (Exception e) {
                addError("Failed to close LogPilotClient", e);
            }
        }
        super.stop();
    }

    private LogLevel mapLogLevel(ch.qos.logback.classic.Level level) {
        return switch (level.toInt()) {
            case ch.qos.logback.classic.Level.TRACE_INT, ch.qos.logback.classic.Level.DEBUG_INT -> LogLevel.DEBUG;
            case ch.qos.logback.classic.Level.INFO_INT -> LogLevel.INFO;
            case ch.qos.logback.classic.Level.WARN_INT -> LogLevel.WARN;
            case ch.qos.logback.classic.Level.ERROR_INT -> LogLevel.ERROR;
            default -> LogLevel.INFO;
        };
    }

    // Setters for configuration
    public void setServerUrl(String serverUrl) {
        this.serverUrl = serverUrl;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    public void setEnableBatching(boolean enableBatching) {
        this.enableBatching = enableBatching;
    }

    public void setBatchSize(int batchSize) {
        this.batchSize = batchSize;
    }

    public void setFlushIntervalMillis(long flushIntervalMillis) {
        this.flushIntervalMillis = flushIntervalMillis;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }
}
