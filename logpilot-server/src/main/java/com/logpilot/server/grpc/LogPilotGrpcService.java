package com.logpilot.server.grpc;

import com.logpilot.core.model.LogEntry;
import com.logpilot.core.model.LogLevel;
import com.logpilot.core.service.LogService;
import com.logpilot.grpc.proto.LogPilotProto;
import com.logpilot.grpc.proto.LogServiceGrpc;
import io.grpc.stub.StreamObserver;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
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
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@GrpcService
@ConditionalOnExpression("'${logpilot.server.protocol:all}' == 'grpc' or '${logpilot.server.protocol:all}' == 'all'")
public class LogPilotGrpcService extends LogServiceGrpc.LogServiceImplBase {

    private static final Logger logger = LoggerFactory.getLogger(LogPilotGrpcService.class);
    private final LogService logService;
    private final MeterRegistry meterRegistry;
    private final Map<String, Counter> levelCounters = new ConcurrentHashMap<>();
    private final Map<String, Counter> channelCounters = new ConcurrentHashMap<>();

    @Autowired
    public LogPilotGrpcService(@Qualifier("grpcLogHandler") LogService logService, MeterRegistry meterRegistry) {
        this.logService = logService;
        this.meterRegistry = meterRegistry;
    }

    private void recordLogMetrics(LogEntry logEntry) {
        if (logEntry == null)
            return;

        String level = logEntry.getLevel() != null ? logEntry.getLevel().toString() : "UNKNOWN";
        levelCounters.computeIfAbsent(level,
                l -> Counter.builder("logpilot_logs_received_total")
                        .tag("level", l)
                        .description("Number of logs received by level")
                        .register(meterRegistry))
                // 카운터를 증가시킵니다.
                // Increment the counter.
                .increment();

        String channel = logEntry.getChannel() != null ? logEntry.getChannel() : "unknown";
        channelCounters.computeIfAbsent(channel,
                c -> Counter.builder("logpilot_logs_received_total")
                        .tag("channel", c)
                        .description("Number of logs received by channel")
                        .register(meterRegistry))
                .increment();
    }

    @Override
    public void sendLog(LogPilotProto.LogRequest request, StreamObserver<LogPilotProto.LogResponse> responseObserver) {
        try {
            LogEntry logEntry = convertLogRequestToLogEntry(request);
            // 메트릭을 기록하고 로그를 저장합니다.
            // Record metrics and store the log.
            recordLogMetrics(logEntry);
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
    public void sendLogs(LogPilotProto.SendLogsRequest request,
            StreamObserver<LogPilotProto.SendLogsResponse> responseObserver) {
        try {
            List<LogEntry> logEntries = request.getLogRequestsList().stream()
                    .map(this::convertLogRequestToLogEntry)
                    .toList();

            // 배치 단위로 메트릭을 기록하고 저장합니다.
            // Record metrics and store logs in batch.
            logEntries.forEach(this::recordLogMetrics);
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
    public void fetchLogs(LogPilotProto.FetchLogsRequest request,
            StreamObserver<LogPilotProto.FetchLogsResponse> responseObserver) {
        try {
            List<LogEntry> logEntries;

            if (!request.getChannel().isEmpty()) {
                if (request.getSince() != null && !request.getSince().isEmpty()) {
                    // 클라이언트 사이드 오프셋 전략 (timestamp를 consumerId 대신 사용하던 레거시 동작 지원)
                    // Client-side offset strategy (supporting legacy behavior utilizing timestamp
                    // as consumerId fallback)
                    logEntries = logService.getLogsForConsumer(request.getChannel(), request.getSince(),
                            request.getLimit(), true);
                } else if (request.getConsumerId() != null && !request.getConsumerId().isEmpty()) {
                    // 서버 사이드 오프셋 전략
                    // Server-side offset strategy
                    logEntries = logService.getLogsForConsumer(request.getChannel(), request.getConsumerId(),
                            request.getLimit(), true);
                } else {
                    // 채널만 지정된 경우 최신 로그를 조회합니다. (REST와 동일)
                    // If only channel is specified, retrieve latest logs. (Same as REST)
                    logEntries = logService.getLogsByChannel(request.getChannel(), request.getLimit());
                }
            } else {
                // 채널이 지정되지 않은 경우 전체 로그 조회는 더 이상 지원되지 않습니다.
                // If no channel specified, retrieve all logs is no longer supported.
                throw new UnsupportedOperationException("Fetching all logs without channel is not supported");
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

    @Override
    public void seek(LogPilotProto.SeekRequest request,
            StreamObserver<LogPilotProto.SeekResponse> responseObserver) {
        try {
            switch (request.getOperation()) {
                case "EARLIEST" -> logService.seekToBeginning(request.getChannel(), request.getConsumerId());
                case "LATEST" -> logService.seekToEnd(request.getChannel(), request.getConsumerId());
                case "SPECIFIC" ->
                    logService.seekToId(request.getChannel(), request.getConsumerId(), request.getLogId());
                default -> throw new IllegalArgumentException("Unknown seek operation: " + request.getOperation());
            }

            LogPilotProto.SeekResponse response = LogPilotProto.SeekResponse.newBuilder()
                    .setStatus("success")
                    .setMessage("Seek operation successful")
                    .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();

            logger.info("Seek operation {} performed for consumer: {} on channel: {}",
                    request.getOperation(), request.getConsumerId(), request.getChannel());
        } catch (Exception e) {
            logger.error("Failed to perform seek operation", e);
            LogPilotProto.SeekResponse response = LogPilotProto.SeekResponse.newBuilder()
                    .setStatus("error")
                    .setMessage("Failed to seek: " + e.getMessage())
                    .build();
            responseObserver.onNext(response);
            responseObserver.onCompleted();
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

        if (logEntry.getId() != null) {
            builder.setId(logEntry.getId());
        }

        if (logEntry.getMeta() != null) {
            Map<String, String> stringMeta = logEntry.getMeta().entrySet().stream()
                    .collect(Collectors.toMap(
                            Map.Entry::getKey,
                            entry -> entry.getValue().toString()));
            builder.putAllMeta(stringMeta);
        }

        return builder.build();
    }

    private LogLevel convertStringToLogLevel(String levelString) {
        if (levelString == null) {
            logger.warn("Null log level provided, defaulting to INFO");
            return LogLevel.INFO;
        }
        try {
            return LogLevel.valueOf(levelString.toUpperCase());
        } catch (IllegalArgumentException e) {
            logger.warn("Unknown log level: {}, defaulting to INFO", levelString);
            return LogLevel.INFO;
        }
    }
}