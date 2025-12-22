package com.logpilot.server.rest.service;

import com.logpilot.core.model.LogEntry;
import com.logpilot.core.service.LogService;
import com.logpilot.core.storage.LogStorage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service("restLogService")
public class RestLogService implements LogService {

    private static final Logger logger = LoggerFactory.getLogger(RestLogService.class);
    private final LogStorage logStorage;

    @Autowired
    public RestLogService(LogStorage logStorage) {
        if (logStorage == null) {
            throw new NullPointerException("LogStorage cannot be null");
        }
        this.logStorage = logStorage;
        logger.info("RestLogService created with LogStorage: {}", logStorage.getClass().getSimpleName());
    }

    @Override
    public void storeLog(LogEntry logEntry) {
        if (logEntry != null) {
            logger.debug("[REST] Storing log entry for channel: {}", logEntry.getChannel());
        } else {
            logger.debug("[REST] Storing null log entry");
        }
        logStorage.store(logEntry);
    }

    @Override
    public void storeLogs(List<LogEntry> logEntries) {
        if (logEntries != null) {
            logger.debug("[REST] Storing {} log entries", logEntries.size());
        } else {
            logger.debug("[REST] Storing null log entries list");
        }
        logStorage.storeLogs(logEntries);
    }

    @Override
    public List<LogEntry> getLogsForConsumer(String channel, String consumerId, int limit) {
        return getLogsForConsumer(channel, consumerId, limit, true);
    }

    @Override
    public List<LogEntry> getLogsForConsumer(String channel, String consumerId, int limit, boolean autoCommit) {
        logger.debug("[REST] Retrieving logs for channel: {} and consumer: {} (autoCommit={})", channel, consumerId, autoCommit);
        return logStorage.retrieve(channel, consumerId, limit, autoCommit);
    }

    @Override
    public void commitLogOffset(String channel, String consumerId, long lastLogId) {
        logger.debug("[REST] Committing offset for channel: {} and consumer: {} to logId: {}", channel, consumerId, lastLogId);
        logStorage.commitOffset(channel, consumerId, lastLogId);
    }

    @Override
    public List<LogEntry> getAllLogs(int limit) {
        logger.debug("[REST] Retrieving all logs with limit: {}", limit);
        return logStorage.retrieveAll(limit);
    }
}