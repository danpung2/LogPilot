package com.logpilot.demo.consume.service;

import com.logpilot.client.LogPilotClient;
import com.logpilot.core.model.LogEntry;
import com.logpilot.core.model.LogLevel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import jakarta.annotation.PreDestroy;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class AnalyticsService {
    private static final Logger log = LoggerFactory.getLogger(AnalyticsService.class);

    private final LogPilotClient logPilotClient;
    private final Map<String, AtomicLong> jobViewCounts = new ConcurrentHashMap<>();
    private final Map<String, AtomicLong> jobApplicationCounts = new ConcurrentHashMap<>();
    private final AtomicLong errorCount = new AtomicLong(0);

    private String lastLogId = "";

    public AnalyticsService(@Value("${logpilot.server-url}") String serverUrl,
            @Value("${logpilot.api-key:logpilot-secret-key-123}") String apiKey) {
        this.logPilotClient = LogPilotClient.builder()
                .serverUrl(serverUrl)
                .apiKey(apiKey)
                .clientType(LogPilotClient.ClientType.GRPC)
                .build();
    }

    @Scheduled(fixedRate = 1000)
    // 1초마다 로그를 폴링합니다.
    // Poll every 1 second.
    public void fetchLogs() {
        try {
            List<LogEntry> logs = logPilotClient.getLogs("demo-app", lastLogId, 100);

            if (logs.isEmpty()) {
                return;
            }

            for (LogEntry entry : logs) {
                processLogEntry(entry);
                if (entry.getId() != null) {
                    // 최신 로그 ID로 오프셋을 업데이트합니다.
                    // Update offset to the latest log ID.
                    lastLogId = String.valueOf(entry.getId());
                }
            }

            // NOTE: gRPC 응답의 로그 ID를 사용하여 페이징 처리합니다.
            // Now we have proper pagination using the Log ID directly from gRPC response.
            // 다음 폴링은 'lastLogId' 이후부터 시작됩니다.
            // The next poll will start after 'lastLogId'.

        } catch (Exception e) {
            log.error("Failed to fetch logs", e);
        }
    }

    private void processLogEntry(LogEntry entry) {
        Map<String, Object> meta = entry.getMeta();

        if (entry.getLevel() == LogLevel.ERROR) {
            errorCount.incrementAndGet();
        }

        if (meta != null && meta.containsKey("action")) {
            String action = (String) meta.get("action");
            String jobId = (String) meta.get("jobId");

            if ("VIEW_JOB".equals(action)) {
                jobViewCounts.computeIfAbsent(jobId, k -> new AtomicLong()).incrementAndGet();
            } else if ("APPLY_JOB".equals(action)) {
                jobApplicationCounts.computeIfAbsent(jobId, k -> new AtomicLong()).incrementAndGet();
            }
        }
    }

    public Map<String, Object> getStats() {
        long totalViews = jobViewCounts.values().stream().mapToLong(AtomicLong::get).sum();
        long totalApplications = jobApplicationCounts.values().stream().mapToLong(AtomicLong::get).sum();
        double conversionRate = totalViews > 0 ? (double) totalApplications / totalViews * 100 : 0;

        return Map.of(
                "totalViews", totalViews,
                "totalApplications", totalApplications,
                "conversionRate", String.format("%.2f%%", conversionRate),
                "errorCount", errorCount.get());
    }

    @PreDestroy
    public void close() {
        logPilotClient.close();
    }
}
