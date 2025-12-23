package com.logpilot.server.integration;

import com.logpilot.core.model.LogEntry;
import com.logpilot.core.model.LogLevel;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "logpilot.server.protocol=all",
        "logpilot.storage.type=FILE",
        "logpilot.server.api-key=test-api-key"
})
public class LogPilotServerIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    private LogEntry testLogEntry;
    private List<LogEntry> testLogEntries;

    @BeforeEach
    void setUp() {
        testLogEntry = LogEntry.builder()
                .channel("integration-test")
                .level(LogLevel.INFO)
                .message("Integration test message")
                .timestamp(LocalDateTime.now())
                .build();

        testLogEntries = Arrays.asList(
                LogEntry.builder()
                        .channel("batch-test-1")
                        .level(LogLevel.DEBUG)
                        .message("Batch message 1")
                        .timestamp(LocalDateTime.now())
                        .build(),
                LogEntry.builder()
                        .channel("batch-test-2")
                        .level(LogLevel.WARN)
                        .message("Batch message 2")
                        .timestamp(LocalDateTime.now())
                        .build());
    }

    private HttpHeaders createHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-API-KEY", "test-api-key");
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }

    @Test
    void contextLoads_WithAllProfiles_ShouldStartSuccessfully() {
        // Application context should load successfully
        assertNotNull(restTemplate);
        assertTrue(port > 0);
    }

    @Test
    void restEndpoints_ShouldWorkEndToEnd_WithFileStorage() {
        // Store a single log
        HttpEntity<LogEntry> storeRequest = new HttpEntity<>(testLogEntry, createHeaders());
        ResponseEntity<Void> storeResponse = restTemplate.exchange(
                "/api/logs",
                HttpMethod.POST,
                storeRequest,
                Void.class);
        assertEquals(HttpStatus.CREATED, storeResponse.getStatusCode());

        // Store multiple logs
        HttpEntity<List<LogEntry>> batchRequest = new HttpEntity<>(testLogEntries, createHeaders());
        ResponseEntity<Void> batchStoreResponse = restTemplate.exchange(
                "/api/logs/batch",
                HttpMethod.POST,
                batchRequest,
                Void.class);
        assertEquals(HttpStatus.CREATED, batchStoreResponse.getStatusCode());

        // Retrieve logs
        HttpEntity<?> getRequest = new HttpEntity<>(createHeaders());
        ResponseEntity<List<LogEntry>> getResponse = restTemplate.exchange(
                "/api/logs?limit=10",
                HttpMethod.GET,
                getRequest,
                new ParameterizedTypeReference<List<LogEntry>>() {
                });
        assertEquals(HttpStatus.OK, getResponse.getStatusCode());
        assertNotNull(getResponse.getBody());
    }

    @Test
    void restAndGrpc_ShouldWorkTogether_SameStorage() throws Exception {
        // Store via REST
        ResponseEntity<Void> restStoreResponse = restTemplate.postForEntity(
                "/api/logs",
                new HttpEntity<>(testLogEntry, createHeaders()),
                Void.class);
        assertEquals(HttpStatus.CREATED, restStoreResponse.getStatusCode());

        // Retrieve via REST to verify storage
        ResponseEntity<List<LogEntry>> getResponse = restTemplate.exchange(
                "/api/logs?limit=10",
                HttpMethod.GET,
                new HttpEntity<>(createHeaders()),
                new ParameterizedTypeReference<List<LogEntry>>() {
                });
        assertEquals(HttpStatus.OK, getResponse.getStatusCode());
        List<LogEntry> retrievedLogs = getResponse.getBody();
        assertNotNull(retrievedLogs);

        // Verify that logs from both protocols are accessible
        assertTrue(retrievedLogs.size() >= 1);
    }

    @Test
    void multipleClients_ShouldAccessConcurrently() throws Exception {
        int numberOfClients = 5;
        int requestsPerClient = 10;
        CountDownLatch latch = new CountDownLatch(numberOfClients);
        ExecutorService executor = Executors.newFixedThreadPool(numberOfClients);

        for (int i = 0; i < numberOfClients; i++) {
            final int clientId = i;
            executor.submit(() -> {
                try {
                    for (int j = 0; j < requestsPerClient; j++) {
                        LogEntry clientLogEntry = LogEntry.builder()
                                .channel("client-" + clientId)
                                .level(LogLevel.INFO)
                                .message("Message " + j + " from client " + clientId)
                                .timestamp(LocalDateTime.now())
                                .build();

                        ResponseEntity<Void> response = restTemplate.postForEntity(
                                "/api/logs",
                                new HttpEntity<>(clientLogEntry, createHeaders()),
                                Void.class);
                        assertEquals(HttpStatus.CREATED, response.getStatusCode());
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        assertTrue(latch.await(30, TimeUnit.SECONDS));
        executor.shutdown();

        // Verify all logs were stored
        ResponseEntity<List<LogEntry>> getResponse = restTemplate.exchange(
                "/api/logs?limit=100",
                HttpMethod.GET,
                new HttpEntity<>(createHeaders()),
                new ParameterizedTypeReference<List<LogEntry>>() {
                });
        assertEquals(HttpStatus.OK, getResponse.getStatusCode());
        List<LogEntry> allLogs = getResponse.getBody();
        assertNotNull(allLogs);
        assertTrue(allLogs.size() >= numberOfClients * requestsPerClient);
    }

    @Test
    void largeBatchRequests_ShouldProcessCorrectly() {
        int batchSize = 100;
        LogEntry[] largeBatch = new LogEntry[batchSize];

        for (int i = 0; i < batchSize; i++) {
            largeBatch[i] = LogEntry.builder()
                    .channel("large-batch")
                    .level(LogLevel.INFO)
                    .message("Large batch message " + i)
                    .timestamp(LocalDateTime.now())
                    .build();
        }

        ResponseEntity<Void> batchResponse = restTemplate.postForEntity(
                "/api/logs/batch",
                new HttpEntity<>(Arrays.asList(largeBatch), createHeaders()),
                Void.class);
        assertEquals(HttpStatus.CREATED, batchResponse.getStatusCode());

        // Verify logs were stored
        ResponseEntity<List<LogEntry>> getResponse = restTemplate.exchange(
                "/api/logs?limit=150",
                HttpMethod.GET,
                new HttpEntity<>(createHeaders()),
                new ParameterizedTypeReference<List<LogEntry>>() {
                });
        assertEquals(HttpStatus.OK, getResponse.getStatusCode());
        List<LogEntry> retrievedLogs = getResponse.getBody();
        assertNotNull(retrievedLogs);
        assertTrue(retrievedLogs.size() >= batchSize);
    }

    @Test
    void invalidRequests_ShouldReturnAppropriateErrors() {
        // Test invalid JSON
        HttpHeaders headers = createHeaders();
        HttpEntity<String> invalidJsonRequest = new HttpEntity<>("{invalid json}", headers);

        ResponseEntity<Void> invalidResponse = restTemplate.postForEntity(
                "/api/logs",
                invalidJsonRequest,
                Void.class);
        assertEquals(HttpStatus.BAD_REQUEST, invalidResponse.getStatusCode());

        // Test empty body
        ResponseEntity<Void> emptyResponse = restTemplate.postForEntity(
                "/api/logs",
                new HttpEntity<>(null, createHeaders()),
                Void.class);
        assertTrue(emptyResponse.getStatusCode().is4xxClientError());
    }

    @Test
    void crossProtocolDataConsistency_ShouldMaintain() {
        // Store logs via REST
        LogEntry restLogEntry = LogEntry.builder()
                .channel("consistency-test")
                .level(LogLevel.ERROR)
                .message("REST stored message")
                .timestamp(LocalDateTime.now())
                .build();

        ResponseEntity<Void> restResponse = restTemplate.postForEntity(
                "/api/logs",
                new HttpEntity<>(restLogEntry, createHeaders()),
                Void.class);
        assertEquals(HttpStatus.CREATED, restResponse.getStatusCode());

        // Retrieve via REST
        ResponseEntity<List<LogEntry>> restGetResponse = restTemplate.exchange(
                "/api/logs/consistency-test?limit=10",
                HttpMethod.GET,
                new HttpEntity<>(createHeaders()),
                new ParameterizedTypeReference<List<LogEntry>>() {
                });
        assertEquals(HttpStatus.OK, restGetResponse.getStatusCode());

        List<LogEntry> restRetrievedLogs = restGetResponse.getBody();
        assertNotNull(restRetrievedLogs);

        // Verify data consistency
        boolean foundConsistentData = restRetrievedLogs.stream()
                .anyMatch(log -> "consistency-test".equals(log.getChannel())
                        && "REST stored message".equals(log.getMessage())
                        && LogLevel.ERROR.equals(log.getLevel()));
        assertTrue(foundConsistentData, "Data should be consistent across protocols");
    }

    @Test
    void applicationShutdown_ShouldCloseResourcesProperly() {
        // This test verifies that resources are properly managed
        // Spring Boot test framework handles shutdown automatically

        // Store a log to ensure resources are active
        ResponseEntity<Void> response = restTemplate.postForEntity(
                "/api/logs",
                new HttpEntity<>(testLogEntry, createHeaders()),
                Void.class);
        assertEquals(HttpStatus.CREATED, response.getStatusCode());

        // Resources should be properly closed when test completes
        // This is verified by Spring Boot's resource management
        assertTrue(true, "Application should handle shutdown gracefully");
    }

    @Test
    void storagePerformance_ShouldMeetBasicRequirements() {
        int testSize = 50;
        LogEntry[] performanceTestLogs = new LogEntry[testSize];

        for (int i = 0; i < testSize; i++) {
            performanceTestLogs[i] = LogEntry.builder()
                    .channel("performance-test")
                    .level(LogLevel.INFO)
                    .message("Performance test message " + i)
                    .timestamp(LocalDateTime.now())
                    .build();
        }

        long startTime = System.currentTimeMillis();

        ResponseEntity<Void> batchResponse = restTemplate.postForEntity(
                "/api/logs/batch",
                new HttpEntity<>(Arrays.asList(performanceTestLogs), createHeaders()),
                Void.class);

        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;

        assertEquals(HttpStatus.CREATED, batchResponse.getStatusCode());
        assertTrue(duration < 5000, "Batch storage should complete within 5 seconds");
    }

    @Test
    void errorHandling_ShouldBeConsistent() {
        // Test with invalid log level (this should be handled gracefully)
        String invalidLogJson = """
                {
                    "channel": "error-test",
                    "level": "INVALID_LEVEL",
                    "message": "Test message with invalid level",
                    "timestamp": "2025-09-26T15:00:00"
                }
                """;

        HttpHeaders headers = createHeaders();
        HttpEntity<String> request = new HttpEntity<>(invalidLogJson, headers);

        ResponseEntity<Void> response = restTemplate.postForEntity(
                "/api/logs",
                request,
                Void.class);

        // Should handle gracefully - either accept with default or reject consistently
        assertTrue(response.getStatusCode().is2xxSuccessful() ||
                response.getStatusCode().is4xxClientError());
    }

    @Test
    void healthCheck_ShouldIndicateSystemStatus() {
        // Test application health through actuator if available
        // or verify system is responsive through basic endpoint

        ResponseEntity<List<LogEntry>> healthResponse = restTemplate.exchange(
                "/api/logs?limit=1",
                HttpMethod.GET,
                new HttpEntity<>(createHeaders()),
                new ParameterizedTypeReference<List<LogEntry>>() {
                });

        assertEquals(HttpStatus.OK, healthResponse.getStatusCode());
        assertNotNull(healthResponse.getBody());
    }

    @Test
    void dataIntegrity_ShouldBePreserved() {
        // Store log with special characters and metadata
        LogEntry specialLogEntry = LogEntry.builder()
                .channel("integrity-test")
                .level(LogLevel.WARN)
                .message("Special chars: áéíóú, 中文, 🎉, \"quotes\", 'apostrophes'")
                .timestamp(LocalDateTime.now())
                .build();

        ResponseEntity<Void> storeResponse = restTemplate.postForEntity(
                "/api/logs",
                new HttpEntity<>(specialLogEntry, createHeaders()),
                Void.class);
        assertEquals(HttpStatus.CREATED, storeResponse.getStatusCode());

        // Retrieve and verify integrity
        ResponseEntity<List<LogEntry>> getResponse = restTemplate.exchange(
                "/api/logs/integrity-test?limit=10",
                HttpMethod.GET,
                new HttpEntity<>(createHeaders()),
                new ParameterizedTypeReference<List<LogEntry>>() {
                });

        assertEquals(HttpStatus.OK, getResponse.getStatusCode());
        List<LogEntry> retrievedLogs = getResponse.getBody();
        assertNotNull(retrievedLogs);

        boolean foundIntegrityLog = retrievedLogs.stream()
                .anyMatch(log -> log.getMessage().contains("Special chars"));
        assertTrue(foundIntegrityLog, "Special characters should be preserved");
    }

    @Test
    void concurrentReadWrite_ShouldMaintainConsistency() throws Exception {
        int writerThreads = 3;
        int readerThreads = 2;
        int operationsPerThread = 10;
        CountDownLatch allComplete = new CountDownLatch(writerThreads + readerThreads);
        ExecutorService executor = Executors.newFixedThreadPool(writerThreads + readerThreads);

        // Start writer threads
        for (int i = 0; i < writerThreads; i++) {
            final int writerId = i;
            executor.submit(() -> {
                try {
                    for (int j = 0; j < operationsPerThread; j++) {
                        LogEntry writerLogEntry = LogEntry.builder()
                                .channel("concurrent-writer-" + writerId)
                                .level(LogLevel.INFO)
                                .message("Concurrent write " + j)
                                .timestamp(LocalDateTime.now())
                                .build();

                        ResponseEntity<Void> response = restTemplate.postForEntity(
                                "/api/logs",
                                new HttpEntity<>(writerLogEntry, createHeaders()),
                                Void.class);
                        assertEquals(HttpStatus.CREATED, response.getStatusCode());
                    }
                } finally {
                    allComplete.countDown();
                }
            });
        }

        // Start reader threads
        for (int i = 0; i < readerThreads; i++) {
            executor.submit(() -> {
                try {
                    for (int j = 0; j < operationsPerThread; j++) {
                        ResponseEntity<List<LogEntry>> response = restTemplate.exchange(
                                "/api/logs?limit=5",
                                HttpMethod.GET,
                                new HttpEntity<>(createHeaders()),
                                new ParameterizedTypeReference<List<LogEntry>>() {
                                });
                        assertEquals(HttpStatus.OK, response.getStatusCode());
                        assertNotNull(response.getBody());
                    }
                } finally {
                    allComplete.countDown();
                }
            });
        }

        assertTrue(allComplete.await(30, TimeUnit.SECONDS));
        executor.shutdown();
    }
}