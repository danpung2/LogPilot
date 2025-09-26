package com.logpilot.server.performance;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.logpilot.core.model.LogEntry;
import com.logpilot.core.model.LogLevel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
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
    "logpilot.storage.directory=/tmp/performance-test"
})
public class PerformanceTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    private static final int PERFORMANCE_TIMEOUT_SECONDS = 30;
    private static final int HIGH_THROUGHPUT_REQUESTS = 100;
    private static final int CONCURRENT_CLIENTS = 10;
    private static final int LARGE_BATCH_SIZE = 500;

    @BeforeEach
    void setUp() {
        // Warm up the application
        LogEntry warmupEntry = LogEntry.builder()
                .channel("warmup")
                .level(LogLevel.INFO)
                .message("Warmup message")
                .timestamp(LocalDateTime.now())
                .build();

        restTemplate.postForEntity("/api/logs", warmupEntry, Void.class);
    }

    @Test
    void restApi_ShouldHandleHighThroughput() {
        int requestCount = HIGH_THROUGHPUT_REQUESTS;
        List<LogEntry> testEntries = new ArrayList<>();

        // Prepare test data
        for (int i = 0; i < requestCount; i++) {
            testEntries.add(LogEntry.builder()
                    .channel("throughput-test")
                    .level(LogLevel.INFO)
                    .message("High throughput message " + i)
                    .timestamp(LocalDateTime.now())
                    .build());
        }

        long startTime = System.currentTimeMillis();

        // Send requests
        List<ResponseEntity<Void>> responses = new ArrayList<>();
        for (LogEntry entry : testEntries) {
            ResponseEntity<Void> response = restTemplate.postForEntity("/api/logs", entry, Void.class);
            responses.add(response);
        }

        long endTime = System.currentTimeMillis();
        long totalTime = endTime - startTime;

        // Verify all requests succeeded
        for (ResponseEntity<Void> response : responses) {
            assertEquals(HttpStatus.CREATED, response.getStatusCode());
        }

        // Performance assertions
        double requestsPerSecond = (double) requestCount / (totalTime / 1000.0);
        assertTrue(requestsPerSecond > 10,
            "Should handle at least 10 requests per second, got: " + requestsPerSecond);
        assertTrue(totalTime < PERFORMANCE_TIMEOUT_SECONDS * 1000,
            "High throughput test should complete within " + PERFORMANCE_TIMEOUT_SECONDS + " seconds");

        System.out.println("REST API Throughput: " + requestsPerSecond + " requests/second");
    }

    @Test
    void concurrentClients_ShouldMaintainPerformance() throws InterruptedException {
        int clientCount = CONCURRENT_CLIENTS;
        int requestsPerClient = 20;
        CountDownLatch latch = new CountDownLatch(clientCount);
        ExecutorService executor = Executors.newFixedThreadPool(clientCount);
        List<CompletableFuture<Long>> futures = new ArrayList<>();

        long testStartTime = System.currentTimeMillis();

        for (int i = 0; i < clientCount; i++) {
            final int clientId = i;
            CompletableFuture<Long> future = CompletableFuture.supplyAsync(() -> {
                long clientStartTime = System.currentTimeMillis();
                try {
                    for (int j = 0; j < requestsPerClient; j++) {
                        LogEntry entry = LogEntry.builder()
                                .channel("concurrent-client-" + clientId)
                                .level(LogLevel.INFO)
                                .message("Concurrent message " + j)
                                .timestamp(LocalDateTime.now())
                                .build();

                        ResponseEntity<Void> response = restTemplate.postForEntity(
                            "/api/logs", entry, Void.class);
                        assertEquals(HttpStatus.CREATED, response.getStatusCode());
                    }
                } finally {
                    latch.countDown();
                }
                return System.currentTimeMillis() - clientStartTime;
            }, executor);
            futures.add(future);
        }

        // Wait for all clients to complete
        assertTrue(latch.await(PERFORMANCE_TIMEOUT_SECONDS, TimeUnit.SECONDS),
            "All concurrent clients should complete within timeout");

        long testEndTime = System.currentTimeMillis();
        long totalTestTime = testEndTime - testStartTime;

        // Verify no client took too long individually
        for (CompletableFuture<Long> future : futures) {
            try {
                Long clientTime = future.get();
                assertTrue(clientTime < 15000,
                    "Individual client should complete within 15 seconds, took: " + clientTime + "ms");
            } catch (Exception e) {
                fail("Client execution failed: " + e.getMessage());
            }
        }

        // Calculate overall performance
        int totalRequests = clientCount * requestsPerClient;
        double overallThroughput = (double) totalRequests / (totalTestTime / 1000.0);
        assertTrue(overallThroughput > 15,
            "Concurrent throughput should exceed 15 requests/second, got: " + overallThroughput);

        System.out.println("Concurrent Performance: " + overallThroughput + " requests/second with "
            + clientCount + " clients");

        executor.shutdown();
    }

    @Test
    void largeBatches_ShouldProcessWithinTimeout() {
        int batchSize = LARGE_BATCH_SIZE;
        List<LogEntry> largeBatch = new ArrayList<>();

        for (int i = 0; i < batchSize; i++) {
            largeBatch.add(LogEntry.builder()
                    .channel("large-batch-test")
                    .level(LogLevel.INFO)
                    .message("Large batch message " + i + " with some additional content to test processing time")
                    .timestamp(LocalDateTime.now())
                    .build());
        }

        long startTime = System.currentTimeMillis();

        ResponseEntity<Void> response = restTemplate.postForEntity(
            "/api/logs/batch", largeBatch, Void.class);

        long endTime = System.currentTimeMillis();
        long processingTime = endTime - startTime;

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertTrue(processingTime < 10000,
            "Large batch (" + batchSize + " entries) should process within 10 seconds, took: "
            + processingTime + "ms");

        // Verify batch was stored
        ResponseEntity<List<LogEntry>> retrievalResponse = restTemplate.exchange(
            "/api/logs/large-batch-test?limit=" + (batchSize + 10),
            HttpMethod.GET,
            null,
            new ParameterizedTypeReference<List<LogEntry>>() {}
        );

        assertEquals(HttpStatus.OK, retrievalResponse.getStatusCode());
        List<LogEntry> retrievedLogs = retrievalResponse.getBody();
        assertNotNull(retrievedLogs);
        assertTrue(retrievedLogs.size() >= batchSize,
            "Should retrieve at least " + batchSize + " entries");

        System.out.println("Large batch processing: " + processingTime + "ms for " + batchSize + " entries");
    }

    @Test
    void memoryUsage_ShouldStayWithinLimits() throws InterruptedException {
        Runtime runtime = Runtime.getRuntime();

        // Measure initial memory
        System.gc(); // Force garbage collection
        Thread.sleep(100);
        long initialMemory = runtime.totalMemory() - runtime.freeMemory();

        // Perform memory-intensive operations
        int operationCount = 50;
        for (int i = 0; i < operationCount; i++) {
            List<LogEntry> batch = new ArrayList<>();
            for (int j = 0; j < 20; j++) {
                batch.add(LogEntry.builder()
                        .channel("memory-test-" + i)
                        .level(LogLevel.INFO)
                        .message("Memory test message " + j + " with additional content for memory usage testing")
                        .timestamp(LocalDateTime.now())
                        .build());
            }

            ResponseEntity<Void> response = restTemplate.postForEntity(
                "/api/logs/batch", batch, Void.class);
            assertEquals(HttpStatus.CREATED, response.getStatusCode());
        }

        // Measure final memory
        System.gc(); // Force garbage collection
        Thread.sleep(100);
        long finalMemory = runtime.totalMemory() - runtime.freeMemory();
        long memoryIncrease = finalMemory - initialMemory;

        // Memory increase should be reasonable (less than 50MB)
        long maxMemoryIncrease = 50 * 1024 * 1024; // 50MB
        assertTrue(memoryIncrease < maxMemoryIncrease,
            "Memory increase should be less than 50MB, but was: " + (memoryIncrease / 1024 / 1024) + "MB");

        System.out.println("Memory usage increase: " + (memoryIncrease / 1024 / 1024) + "MB");
    }

    @Test
    void storagePerformance_ShouldMeetRequirements() {
        int testIterations = 20;
        List<Long> writeTimes = new ArrayList<>();
        List<Long> readTimes = new ArrayList<>();

        for (int i = 0; i < testIterations; i++) {
            // Measure write performance
            LogEntry testEntry = LogEntry.builder()
                    .channel("storage-perf-test-" + i)
                    .level(LogLevel.INFO)
                    .message("Storage performance test message " + i)
                    .timestamp(LocalDateTime.now())
                    .build();

            long writeStartTime = System.nanoTime();
            ResponseEntity<Void> writeResponse = restTemplate.postForEntity(
                "/api/logs", testEntry, Void.class);
            long writeEndTime = System.nanoTime();

            assertEquals(HttpStatus.CREATED, writeResponse.getStatusCode());
            writeTimes.add(writeEndTime - writeStartTime);

            // Measure read performance
            long readStartTime = System.nanoTime();
            ResponseEntity<List<LogEntry>> readResponse = restTemplate.exchange(
                "/api/logs/storage-perf-test-" + i + "?limit=10",
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<List<LogEntry>>() {}
            );
            long readEndTime = System.nanoTime();

            assertEquals(HttpStatus.OK, readResponse.getStatusCode());
            readTimes.add(readEndTime - readStartTime);
        }

        // Calculate averages
        double avgWriteTime = writeTimes.stream().mapToLong(Long::longValue).average().orElse(0);
        double avgReadTime = readTimes.stream().mapToLong(Long::longValue).average().orElse(0);

        // Convert to milliseconds
        double avgWriteMs = avgWriteTime / 1_000_000;
        double avgReadMs = avgReadTime / 1_000_000;

        // Performance assertions
        assertTrue(avgWriteMs < 500, "Average write time should be less than 500ms, got: " + avgWriteMs + "ms");
        assertTrue(avgReadMs < 200, "Average read time should be less than 200ms, got: " + avgReadMs + "ms");

        System.out.println("Storage Performance - Write: " + avgWriteMs + "ms, Read: " + avgReadMs + "ms");
    }

    @Test
    void responseTime_ShouldMeetSLA() {
        int sampleSize = 30;
        List<Long> responseTimes = new ArrayList<>();

        for (int i = 0; i < sampleSize; i++) {
            LogEntry testEntry = LogEntry.builder()
                    .channel("sla-test")
                    .level(LogLevel.INFO)
                    .message("SLA test message " + i)
                    .timestamp(LocalDateTime.now())
                    .build();

            long startTime = System.nanoTime();
            ResponseEntity<Void> response = restTemplate.postForEntity("/api/logs", testEntry, Void.class);
            long endTime = System.nanoTime();

            assertEquals(HttpStatus.CREATED, response.getStatusCode());
            responseTimes.add(endTime - startTime);
        }

        // Calculate statistics
        double avgResponseTime = responseTimes.stream().mapToLong(Long::longValue).average().orElse(0) / 1_000_000;
        long maxResponseTime = responseTimes.stream().mapToLong(Long::longValue).max().orElse(0) / 1_000_000;
        long minResponseTime = responseTimes.stream().mapToLong(Long::longValue).min().orElse(0) / 1_000_000;

        // Calculate 95th percentile
        responseTimes.sort(Long::compareTo);
        int percentile95Index = (int) Math.ceil(0.95 * sampleSize) - 1;
        long percentile95 = responseTimes.get(percentile95Index) / 1_000_000;

        // SLA assertions
        assertTrue(avgResponseTime < 1000, "Average response time should be less than 1000ms, got: " + avgResponseTime + "ms");
        assertTrue(percentile95 < 2000, "95th percentile should be less than 2000ms, got: " + percentile95 + "ms");
        assertTrue(maxResponseTime < 5000, "Max response time should be less than 5000ms, got: " + maxResponseTime + "ms");

        System.out.println("Response Time SLA - Avg: " + avgResponseTime + "ms, 95th: " + percentile95 + "ms, Max: " + maxResponseTime + "ms");
    }

    @Test
    void errorRate_ShouldStayBelowThreshold() {
        int totalRequests = 100;
        int errorCount = 0;

        for (int i = 0; i < totalRequests; i++) {
            LogEntry testEntry = LogEntry.builder()
                    .channel("error-rate-test")
                    .level(LogLevel.INFO)
                    .message("Error rate test message " + i)
                    .timestamp(LocalDateTime.now())
                    .build();

            try {
                ResponseEntity<Void> response = restTemplate.postForEntity("/api/logs", testEntry, Void.class);
                if (!response.getStatusCode().is2xxSuccessful()) {
                    errorCount++;
                }
            } catch (Exception e) {
                errorCount++;
            }
        }

        double errorRate = (double) errorCount / totalRequests * 100;

        // Error rate should be less than 1%
        assertTrue(errorRate < 1.0, "Error rate should be less than 1%, got: " + errorRate + "%");

        System.out.println("Error Rate: " + errorRate + "% (" + errorCount + "/" + totalRequests + ")");
    }
}