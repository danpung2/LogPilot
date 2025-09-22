package com.logpilot.server.grpc;

import com.logpilot.core.model.LogEntry;
import com.logpilot.core.model.LogLevel;
import com.logpilot.core.service.LogService;
import com.logpilot.grpc.proto.LogPilotProto;
import com.logpilot.grpc.proto.LogPilotServiceGrpc;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@GrpcService
@ConditionalOnExpression("'${logpilot.server.protocol:all}' == 'grpc' or '${logpilot.server.protocol:all}' == 'all'")
public class LogPilotGrpcService extends LogPilotServiceGrpc.LogPilotServiceImplBase {

    private static final Logger logger = LoggerFactory.getLogger(LogPilotGrpcService.class);
    private final LogService logService;

    @Autowired
    public LogPilotGrpcService(@Qualifier("grpcLogHandler") LogService logService) {
        this.logService = logService;
    }

    @Override
    public void storeLog(LogPilotProto.StoreLogRequest request, StreamObserver<LogPilotProto.StoreLogResponse> responseObserver) {
        try {
            LogEntry logEntry = convertToLogEntry(request.getLogEntry());
            logService.storeLog(logEntry);

            LogPilotProto.StoreLogResponse response = LogPilotProto.StoreLogResponse.newBuilder()
                    .setSuccess(true)
                    .setMessage("Log stored successfully")
                    .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();

            logger.debug("Stored log entry via gRPC for channel: {}", logEntry.getChannel());
        } catch (Exception e) {
            logger.error("Failed to store log entry via gRPC", e);

            LogPilotProto.StoreLogResponse response = LogPilotProto.StoreLogResponse.newBuilder()
                    .setSuccess(false)
                    .setMessage("Failed to store log: " + e.getMessage())
                    .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();
        }
    }

    @Override
    public void storeLogs(LogPilotProto.StoreLogsRequest request, StreamObserver<LogPilotProto.StoreLogsResponse> responseObserver) {
        try {
            List<LogEntry> logEntries = request.getLogEntriesList().stream()
                    .map(this::convertToLogEntry)
                    .toList();

            logService.storeLogs(logEntries);

            LogPilotProto.StoreLogsResponse response = LogPilotProto.StoreLogsResponse.newBuilder()
                    .setSuccess(true)
                    .setMessage("Logs stored successfully")
                    .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();

            logger.debug("Stored {} log entries via gRPC", logEntries.size());
        } catch (Exception e) {
            logger.error("Failed to store log entries via gRPC", e);

            LogPilotProto.StoreLogsResponse response = LogPilotProto.StoreLogsResponse.newBuilder()
                    .setSuccess(false)
                    .setMessage("Failed to store logs: " + e.getMessage())
                    .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();
        }
    }

    @Override
    public void getLogs(LogPilotProto.GetLogsRequest request, StreamObserver<LogPilotProto.GetLogsResponse> responseObserver) {
        try {
            List<LogEntry> logEntries;

            if (!request.getConsumerId().isEmpty()) {
                logEntries = logService.getLogsForConsumer(request.getChannel(), request.getConsumerId(), request.getLimit());
            } else {
                logEntries = logService.getAllLogs(request.getLimit());
            }

            List<LogPilotProto.LogEntry> protoLogEntries = logEntries.stream()
                    .map(this::convertToProtoLogEntry)
                    .collect(Collectors.toList());

            LogPilotProto.GetLogsResponse response = LogPilotProto.GetLogsResponse.newBuilder()
                    .addAllLogEntries(protoLogEntries)
                    .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();

            logger.debug("Retrieved {} log entries via gRPC", logEntries.size());
        } catch (Exception e) {
            logger.error("Failed to retrieve log entries via gRPC", e);
            responseObserver.onError(e);
        }
    }

    private LogEntry convertToLogEntry(LogPilotProto.LogEntry protoLogEntry) {
        LogEntry logEntry = new LogEntry();
        logEntry.setChannel(protoLogEntry.getChannel());
        logEntry.setLevel(convertToLogLevel(protoLogEntry.getLevel()));
        logEntry.setMessage(protoLogEntry.getMessage());

        if (!protoLogEntry.getMetaMap().isEmpty()) {
            Map<String, Object> meta = new HashMap<>(protoLogEntry.getMetaMap());
            logEntry.setMeta(meta);
        }

        if (!protoLogEntry.getTimestamp().isEmpty()) {
            try {
                LocalDateTime timestamp = LocalDateTime.parse(protoLogEntry.getTimestamp(), DateTimeFormatter.ISO_LOCAL_DATE_TIME);
                logEntry.setTimestamp(timestamp);
            } catch (Exception e) {
                logger.warn("Failed to parse timestamp: {}", protoLogEntry.getTimestamp(), e);
            }
        }

        return logEntry;
    }

    private LogPilotProto.LogEntry convertToProtoLogEntry(LogEntry logEntry) {
        LogPilotProto.LogEntry.Builder builder = LogPilotProto.LogEntry.newBuilder()
                .setChannel(logEntry.getChannel())
                .setLevel(convertToProtoLogLevel(logEntry.getLevel()))
                .setMessage(logEntry.getMessage())
                .setTimestamp(logEntry.getTimestamp().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));

        if (logEntry.getMeta() != null) {
            Map<String, String> stringMeta = logEntry.getMeta().entrySet().stream()
                    .collect(Collectors.toMap(
                            Map.Entry::getKey,
                            entry -> entry.getValue().toString()
                    ));
            builder.putAllMeta(stringMeta);
        }

        return builder.build();
    }

    private LogLevel convertToLogLevel(LogPilotProto.LogLevel protoLogLevel) {
        return switch (protoLogLevel) {
            case DEBUG -> LogLevel.DEBUG;
            case INFO -> LogLevel.INFO;
            case WARN -> LogLevel.WARN;
            case ERROR -> LogLevel.ERROR;
            default -> throw new IllegalArgumentException("Unknown log level: " + protoLogLevel);
        };
    }

    private LogPilotProto.LogLevel convertToProtoLogLevel(LogLevel logLevel) {
        return switch (logLevel) {
            case DEBUG -> LogPilotProto.LogLevel.DEBUG;
            case INFO -> LogPilotProto.LogLevel.INFO;
            case WARN -> LogPilotProto.LogLevel.WARN;
            case ERROR -> LogPilotProto.LogLevel.ERROR;
        };
    }
}