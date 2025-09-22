package com.logpilot.core.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Objects;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class LogEntry {

    @NotNull
    private String channel;

    @NotNull
    private LogLevel level;

    @NotNull
    private String message;

    private Map<String, Object> meta;

    private LocalDateTime timestamp;

    public LogEntry() {
        this.timestamp = LocalDateTime.now();
    }

    public LogEntry(String channel, LogLevel level, String message) {
        this();
        this.channel = channel;
        this.level = level;
        this.message = message;
    }

    public LogEntry(String channel, LogLevel level, String message, Map<String, Object> meta) {
        this(channel, level, message);
        this.meta = meta;
    }

    public String getChannel() {
        return channel;
    }

    public void setChannel(String channel) {
        this.channel = channel;
    }

    public LogLevel getLevel() {
        return level;
    }

    public void setLevel(LogLevel level) {
        this.level = level;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Map<String, Object> getMeta() {
        return meta;
    }

    public void setMeta(Map<String, Object> meta) {
        this.meta = meta;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        LogEntry logEntry = (LogEntry) o;
        return Objects.equals(channel, logEntry.channel) &&
               level == logEntry.level &&
               Objects.equals(message, logEntry.message) &&
               Objects.equals(meta, logEntry.meta) &&
               Objects.equals(timestamp, logEntry.timestamp);
    }

    @Override
    public int hashCode() {
        return Objects.hash(channel, level, message, meta, timestamp);
    }

    @Override
    public String toString() {
        return "LogEntry{" +
               "channel='" + channel + '\'' +
               ", level=" + level +
               ", message='" + message + '\'' +
               ", meta=" + meta +
               ", timestamp=" + timestamp +
               '}';
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String channel;
        private LogLevel level;
        private String message;
        private Map<String, Object> meta;
        private LocalDateTime timestamp;

        public Builder channel(String channel) {
            this.channel = channel;
            return this;
        }

        public Builder level(LogLevel level) {
            this.level = level;
            return this;
        }

        public Builder message(String message) {
            this.message = message;
            return this;
        }

        public Builder meta(Map<String, Object> meta) {
            this.meta = meta;
            return this;
        }

        public Builder timestamp(LocalDateTime timestamp) {
            this.timestamp = timestamp;
            return this;
        }

        public LogEntry build() {
            LogEntry logEntry = new LogEntry();
            logEntry.setChannel(this.channel);
            logEntry.setLevel(this.level);
            logEntry.setMessage(this.message);
            logEntry.setMeta(this.meta);
            if (this.timestamp != null) {
                logEntry.setTimestamp(this.timestamp);
            }
            return logEntry;
        }
    }
}