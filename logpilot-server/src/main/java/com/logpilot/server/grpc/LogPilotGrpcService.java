package com.logpilot.server.grpc;

import com.logpilot.core.model.LogEntry;
import com.logpilot.core.model.LogLevel;
import com.logpilot.core.service.LogService;
import com.logpilot.grpc.proto.LogPilotProto;
import com.logpilot.grpc.proto.LogServiceGrpc;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@GrpcService
@ConditionalOnExpression("'${logpilot.server.protocol:all}' == 'grpc' or '${logpilot.server.protocol:all}' == 'all'")
public class LogPilotGrpcService extends LogServiceGrpc.LogServiceImplBase {

    private static final Logger logger = LoggerFactory.getLogger(LogPilotGrpcService.class);
    private final LogService logService;

    @Autowired
    public LogPilotGrpcService(@Qualifier("grpcLogHandler") LogService logService) {
        this.logService = logService;
    }

    @Override
    public void sendLog(LogPilotProto.LogRequest request, StreamObserver<LogPilotProto.LogResponse> responseObserver) {
        try {
            LogEntry logEntry = convertLogRequestToLogEntry(request);
            logService.storeLog(logEntry);

            LogPilotProto.LogResponse response = LogPilotProto.LogResponse.newBuilder()
                    .setStatus("success")
                    .setMessage("Log stored successfully")
                    .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();

            logger.debug("Stored log entry via gRPC for channel: {}", logEntry.getChannel());
        } catch (Exception e) {
            logger.error("Failed to store log entry via gRPC", e);

            LogPilotProto.LogResponse response = LogPilotProto.LogResponse.newBuilder()
                    .setStatus("error")
                    .setMessage("Failed to store log: " + e.getMessage())
                    .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();
        }
    }

    @Override
    public void sendLogs(LogPilotProto.SendLogsRequest request, StreamObserver<LogPilotProto.SendLogsResponse> responseObserver) {
        try {
            List<LogEntry> logEntries = request.getLogRequestsList().stream()
                    .map(this::convertLogRequestToLogEntry)
                    .toList();

            logService.storeLogs(logEntries);

            LogPilotProto.SendLogsResponse response = LogPilotProto.SendLogsResponse.newBuilder()
                    .setStatus("success")
                    .setMessage("Logs stored successfully")
                    .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();

            logger.debug("Stored {} log entries via gRPC", logEntries.size());
        } catch (Exception e) {
            logger.error("Failed to store log entries via gRPC", e);

            LogPilotProto.SendLogsResponse response = LogPilotProto.SendLogsResponse.newBuilder()
                    .setStatus("error")
                    .setMessage("Failed to store logs: " + e.getMessage())
                    .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();
        }
    }

    @Override
    public void listLogs(LogPilotProto.ListLogsRequest request, StreamObserver<LogPilotProto.ListLogsResponse> responseObserver) {
        try {
            List<LogEntry> logEntries = logService.getAllLogs(100);

            List<LogPilotProto.LogEntry> protoLogEntries = logEntries.stream()
                    .map(this::convertToProtoLogEntry)
                    .collect(Collectors.toList());

            LogPilotProto.ListLogsResponse response = LogPilotProto.ListLogsResponse.newBuilder()
                    .addAllLogs(protoLogEntries)
                    .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();

            logger.debug("Retrieved {} log entries via gRPC (listLogs)", logEntries.size());
        } catch (Exception e) {
            logger.error("Failed to list log entries via gRPC", e);
            responseObserver.onError(e);
        }
    }

    @Override
    public void fetchLogs(LogPilotProto.FetchLogsRequest request, StreamObserver<LogPilotProto.FetchLogsResponse> responseObserver) {
        try {
            List<LogEntry> logEntries;

            if (!request.getChannel().isEmpty()) {
                logEntries = logService.getLogsForConsumer(request.getChannel(), request.getSince(), request.getLimit());
            } else {
                logEntries = logService.getAllLogs(request.getLimit());
            }

            List<LogPilotProto.LogEntry> protoLogEntries = logEntries.stream()
                    .map(this::convertToProtoLogEntry)
                    .collect(Collectors.toList());

            LogPilotProto.FetchLogsResponse response = LogPilotProto.FetchLogsResponse.newBuilder()
                    .addAllLogs(protoLogEntries)
                    .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();

            logger.debug("Retrieved {} log entries via gRPC (fetchLogs)", logEntries.size());
        } catch (Exception e) {
            logger.error("Failed to fetch log entries via gRPC", e);
            responseObserver.onError(e);
        }
    }

    private LogEntry convertLogRequestToLogEntry(LogPilotProto.LogRequest logRequest) {
        LogEntry logEntry = new LogEntry();
        logEntry.setChannel(logRequest.getChannel());
        logEntry.setLevel(convertStringToLogLevel(logRequest.getLevel()));
        logEntry.setMessage(logRequest.getMessage());

        if (!logRequest.getMetaMap().isEmpty()) {
            Map<String, Object> meta = new HashMap<>(logRequest.getMetaMap());
            logEntry.setMeta(meta);
        }

        logEntry.setTimestamp(LocalDateTime.now());

        return logEntry;
    }

    private LogPilotProto.LogEntry convertToProtoLogEntry(LogEntry logEntry) {
        LogPilotProto.LogEntry.Builder builder = LogPilotProto.LogEntry.newBuilder()
                .setChannel(logEntry.getChannel())
                .setLevel(logEntry.getLevel().toString())
                .setMessage(logEntry.getMessage())
                .setTimestamp(logEntry.getTimestamp().atZone(java.time.ZoneOffset.UTC).toInstant().toEpochMilli());

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

    private LogLevel convertStringToLogLevel(String levelString) {
        try {
            return LogLevel.valueOf(levelString.toUpperCase());
        } catch (IllegalArgumentException e) {
            logger.warn("Unknown log level: {}, defaulting to INFO", levelString);
            return LogLevel.INFO;
        }
    }
}