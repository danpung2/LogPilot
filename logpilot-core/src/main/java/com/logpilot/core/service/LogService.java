package com.logpilot.core.service;

import com.logpilot.core.model.LogEntry;

import java.util.List;

public interface LogService {

    /**
     * Store a single log entry
     */
    void storeLog(LogEntry logEntry);

    /**
     * Store multiple log entries in batch
     */
    void storeLogs(List<LogEntry> logEntries);

    /**
     * Retrieve logs for a specific channel and consumer
     */
    List<LogEntry> getLogsForConsumer(String channel, String consumerId, int limit);

    /**
     * Retrieve logs for a specific channel and consumer with optional auto-commit
     */
    default List<LogEntry> getLogsForConsumer(String channel, String consumerId, int limit, boolean autoCommit) {
        return getLogsForConsumer(channel, consumerId, limit);
    }

    /**
     * Commit offset for a consumer
     */
    void commitLogOffset(String channel, String consumerId, long lastLogId);

    /**
     * Retrieve all logs with a limit
     */
    List<LogEntry> getAllLogs(int limit);
}