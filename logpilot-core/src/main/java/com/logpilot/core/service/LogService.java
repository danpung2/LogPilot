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
     * Retrieve all logs with a limit
     */
    List<LogEntry> getAllLogs(int limit);
}