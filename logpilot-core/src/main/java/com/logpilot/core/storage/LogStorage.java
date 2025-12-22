package com.logpilot.core.storage;

import com.logpilot.core.model.LogEntry;

import java.util.List;

public interface LogStorage extends AutoCloseable {

    void store(LogEntry logEntry);

    void storeLogs(List<LogEntry> logEntries);

    List<LogEntry> retrieve(String channel, String consumerId, int limit);

    /**
     * Retrieve logs for a specific channel and consumer with optional auto-commit
     */
    default List<LogEntry> retrieve(String channel, String consumerId, int limit, boolean autoCommit) {
        return retrieve(channel, consumerId, limit);
    }

    /**
     * Manually commit the offset for a consumer
     */
    void commitOffset(String channel, String consumerId, long lastLogId);

    List<LogEntry> retrieveAll(int limit);

    void initialize();

    @Override
    void close();
}