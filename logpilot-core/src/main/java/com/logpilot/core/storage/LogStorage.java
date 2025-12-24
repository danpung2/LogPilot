package com.logpilot.core.storage;

import com.logpilot.core.model.LogEntry;

import java.util.List;

public interface LogStorage extends AutoCloseable {

    void store(LogEntry logEntry);

    void storeLogs(List<LogEntry> logEntries);

    List<LogEntry> retrieve(String channel, String consumerId, int limit);

    /**
     * Retrieve logs for a specific channel
     */
    List<LogEntry> retrieve(String channel, int limit);

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

    /**
     * Seek to the beginning (earliest) of the log
     */
    void seekToBeginning(String channel, String consumerId);

    /**
     * Seek to the end (latest) of the log
     */
    void seekToEnd(String channel, String consumerId);

    /**
     * Seek to a specific log ID
     */
    void seekToId(String channel, String consumerId, long logId);

    void initialize();

    @Override
    void close();
}