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
@ConditionalOnExpression("'${logpilot.server.protocol:all}' == 'grpc' or '${logpilot.server.protocol:all}' == 'all'")
public class GrpcLogHandler implements LogService {

    private static final Logger logger = LoggerFactory.getLogger(GrpcLogHandler.class);
    private final LogStorage logStorage;

    @Autowired
    public GrpcLogHandler(LogStorage logStorage) {
        this.logStorage = logStorage;
    }

    @Override
    public void storeLog(LogEntry logEntry) {
        logger.debug("[gRPC] Storing log entry for channel: {}", logEntry.getChannel());
        logStorage.store(logEntry);
    }

    @Override
    public void storeLogs(List<LogEntry> logEntries) {
        logger.debug("[gRPC] Storing {} log entries", logEntries.size());
        for (LogEntry logEntry : logEntries) {
            logStorage.store(logEntry);
        }
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
}