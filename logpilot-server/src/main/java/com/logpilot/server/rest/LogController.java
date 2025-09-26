package com.logpilot.server.rest;

import com.logpilot.core.model.LogEntry;
import com.logpilot.core.service.LogService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@ConditionalOnExpression("'${logpilot.server.protocol:all}' == 'rest' or '${logpilot.server.protocol:all}' == 'all'")
public class LogController {

    private final LogService logService;

    @Autowired
    public LogController(@Qualifier("restLogService") LogService logService) {
        this.logService = logService;
    }

    @PostMapping("/logs")
    public ResponseEntity<Void> storeLog(@Valid @RequestBody LogEntry logEntry) {
        try {
            logService.storeLog(logEntry);
            return ResponseEntity.status(HttpStatus.CREATED).build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping("/logs/batch")
    public ResponseEntity<Void> storeLogs(@Valid @RequestBody List<LogEntry> logEntries) {
        try {
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