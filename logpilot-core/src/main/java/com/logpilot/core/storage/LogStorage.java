package com.logpilot.core.storage;

import com.logpilot.core.model.LogEntry;

import java.util.List;

public interface LogStorage extends AutoCloseable {

    void store(LogEntry logEntry);

    List<LogEntry> retrieve(String channel, String consumerId, int limit);

    List<LogEntry> retrieveAll(int limit);

    void initialize();

    @Override
    void close();
}