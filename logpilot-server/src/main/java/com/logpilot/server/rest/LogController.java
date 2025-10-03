package com.logpilot.server.rest;

import com.logpilot.core.model.LogEntry;
import com.logpilot.core.service.LogService;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.PostConstruct;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@RestController
@RequestMapping("/api")
public class LogController {

    private static final Logger logger = LoggerFactory.getLogger(LogController.class);

    private final LogService logService;
    private final MeterRegistry meterRegistry;
    private final Map<String, Counter> levelCounters = new ConcurrentHashMap<>();
    private final Map<String, Counter> channelCounters = new ConcurrentHashMap<>();

    @Autowired
    public LogController(@Qualifier("restLogService") LogService logService, MeterRegistry meterRegistry) {
        this.logService = logService;
        this.meterRegistry = meterRegistry;
        logger.info("LogController constructor called with LogService: {} and MeterRegistry: {}",
            logService.getClass().getSimpleName(),
            meterRegistry.getClass().getSimpleName());
    }

    @PostConstruct
    public void init() {
        logger.info("LogController initialized with MeterRegistry: {}", meterRegistry.getClass().getSimpleName());
        for (String level : new String[]{"DEBUG", "INFO", "WARN", "ERROR"}) {
            Counter counter = Counter.builder("logpilot_logs_received_total")
                .tag("level", level)
                .description("Number of logs received by level")
                .register(meterRegistry);
            levelCounters.put(level, counter);
            logger.info("Registered counter for level: {}", level);
        }
    }

    private void recordLogMetrics(LogEntry logEntry) {
        if (logEntry == null || meterRegistry == null) {
            logger.warn("Cannot record metrics - logEntry or meterRegistry is null");
            return;
        }

        try {
            String level = logEntry.getLevel() != null ? logEntry.getLevel().toString() : "UNKNOWN";
            Counter levelCounter = levelCounters.computeIfAbsent(level,
                l -> {
                    logger.info("Creating new counter for level: {}", l);
                    return Counter.builder("logpilot_logs_received_total")
                        .tag("level", l)
                        .description("Number of logs received by level")
                        .register(meterRegistry);
                }
            );
            levelCounter.increment();
            logger.debug("Incremented counter for level: {} (current: {})", level, levelCounter.count());

            String channel = logEntry.getChannel() != null ? logEntry.getChannel() : "unknown";
            Counter channelCounter = channelCounters.computeIfAbsent(channel,
                c -> {
                    logger.info("Creating new counter for channel: {}", c);
                    return Counter.builder("logpilot_logs_received_total")
                        .tag("channel", c)
                        .description("Number of logs received by channel")
                        .register(meterRegistry);
                }
            );
            channelCounter.increment();
            logger.debug("Incremented counter for channel: {} (current: {})", channel, channelCounter.count());
        } catch (Exception e) {
            logger.error("Error recording metrics", e);
        }
    }

    @PostMapping("/logs")
    public ResponseEntity<Void> storeLog(@Valid @RequestBody LogEntry logEntry) {
        try {
            recordLogMetrics(logEntry);
            logService.storeLog(logEntry);
            return ResponseEntity.status(HttpStatus.CREATED).build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping("/logs/batch")
    public ResponseEntity<Void> storeLogs(@Valid @RequestBody List<LogEntry> logEntries) {
        try {
            if (logEntries != null) {
                logEntries.forEach(this::recordLogMetrics);
            }
            logService.storeLogs(logEntries);
            return ResponseEntity.status(HttpStatus.CREATED).build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/logs/{channel}")
    public ResponseEntity<List<LogEntry>> getLogs(
            @PathVariable String channel,
            @RequestParam(required = false) String consumerId,
            @RequestParam(defaultValue = "100") int limit) {

        try {
            List<LogEntry> logs;
            if (consumerId != null) {
                logs = logService.getLogsForConsumer(channel, consumerId, limit);
            } else {
                logs = logService.getAllLogs(limit);
            }
            return ResponseEntity.ok(logs);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/logs")
    public ResponseEntity<List<LogEntry>> getAllLogs(
            @RequestParam(defaultValue = "100") int limit) {
        try {
            List<LogEntry> logs = logService.getAllLogs(limit);
            return ResponseEntity.ok(logs);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}