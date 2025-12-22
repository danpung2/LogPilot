package com.logpilot.server.grpc.handler;

import com.logpilot.core.model.LogEntry;
import com.logpilot.core.service.LogService;
import com.logpilot.core.storage.LogStorage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;

import java.util.List;

@Component("grpcLogHandler")
public class GrpcLogHandler implements LogService {

    private static final Logger logger = LoggerFactory.getLogger(GrpcLogHandler.class);
    private final LogStorage logStorage;

    @Autowired
    public GrpcLogHandler(LogStorage logStorage) {
        if (logStorage == null) {
            throw new NullPointerException("LogStorage cannot be null");
        }
        this.logStorage = logStorage;
    }

    @Override
    public void storeLog(LogEntry logEntry) {
        if (logEntry != null) {
            logger.debug("[gRPC] Storing log entry for channel: {}", logEntry.getChannel());
        } else {
            logger.debug("[gRPC] Storing null log entry");
        }
        logStorage.store(logEntry);
    }

    @Override
    public void storeLogs(List<LogEntry> logEntries) {
        if (logEntries != null) {
            logger.debug("[gRPC] Storing {} log entries", logEntries.size());
        } else {
            logger.debug("[gRPC] Storing null log entries list");
        }
        logStorage.storeLogs(logEntries);
    }

    @Override
    public List<LogEntry> getLogsForConsumer(String channel, String consumerId, int limit) {
        logger.debug("[gRPC] Retrieving logs for channel: {} and consumer: {}", channel, consumerId);
        return logStorage.retrieve(channel, consumerId, limit);
    }

    @Override
    public List<LogEntry> getAllLogs(int limit) {
        logger.debug("[gRPC] Retrieving all logs with limit: {}", limit);
        return logStorage.retrieveAll(limit);
    }

    @Override
    public void commitLogOffset(String channel, String consumerId, long lastLogId) {
        logger.debug("[gRPC] Committing offset for channel: {} and consumer: {} to logId: {}", channel, consumerId, lastLogId);
        logStorage.commitOffset(channel, consumerId, lastLogId);
    }
}