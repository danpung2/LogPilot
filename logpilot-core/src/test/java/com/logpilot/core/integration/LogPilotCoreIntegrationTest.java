package com.logpilot.core.integration;

import com.logpilot.core.config.LogPilotProperties;
import com.logpilot.core.config.LogStorageFactory;
import com.logpilot.core.model.LogEntry;
import com.logpilot.core.model.LogLevel;
import com.logpilot.core.storage.LogStorage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

public class LogPilotCoreIntegrationTest {

    @TempDir
    Path tempDir;

    private LogStorage storage;

    @AfterEach
    void tearDown() {
        if (storage != null) {
            storage.close();
        }
    }

    @Test
    void endToEndWorkflow_WithSqliteStorage_ShouldWorkCorrectly() {
        LogPilotProperties properties = new LogPilotProperties();
        properties.getStorage().setType(LogPilotProperties.StorageType.SQLITE);
        properties.getStorage().getSqlite().setPath(tempDir.resolve("integration.db").toString());

        storage = LogStorageFactory.createLogStorage(properties);

        runEndToEndWorkflow(storage);
    }

    @Test
    void endToEndWorkflow_WithFileStorage_ShouldWorkCorrectly() {
        LogPilotProperties properties = new LogPilotProperties();
        properties.getStorage().setType(LogPilotProperties.StorageType.FILE);
        properties.getStorage().setDirectory(tempDir.toString());

        storage = LogStorageFactory.createLogStorage(properties);

        runEndToEndWorkflow(storage);
    }

    @Test
    void multipleChannels_WithMultipleConsumers_ShouldWorkIndependently() {
        setupFileStorage();

        List<LogEntry> channel1Logs = Arrays.asList(
            createLogEntry("channel1", LogLevel.INFO, "Channel 1 Message 1"),
            createLogEntry("channel1", LogLevel.WARN, "Channel 1 Message 2"),
            createLogEntry("channel1", LogLevel.ERROR, "Channel 1 Message 3")
        );

        List<LogEntry> channel2Logs = Arrays.asList(
            createLogEntry("channel2", LogLevel.DEBUG, "Channel 2 Message 1"),
            createLogEntry("channel2", LogLevel.INFO, "Channel 2 Message 2")
        );

        storage.storeLogs(channel1Logs);
        storage.storeLogs(channel2Logs);

        List<LogEntry> consumer1Logs = storage.retrieve("channel1", "consumer1", 2);
        List<LogEntry> consumer2Logs = storage.retrieve("channel1", "consumer2", 10);

        assertEquals(2, consumer1Logs.size());
        assertEquals("Channel 1 Message 1", consumer1Logs.get(0).getMessage());
        assertEquals("Channel 1 Message 2", consumer1Logs.get(1).getMessage());

        assertEquals(3, consumer2Logs.size());

        List<LogEntry> channel2Consumer = storage.retrieve("channel2", "consumer3", 10);
        assertEquals(2, channel2Consumer.size());
        assertEquals("channel2", channel2Consumer.get(0).getChannel());

        storage.store(createLogEntry("channel1", LogLevel.DEBUG, "Channel 1 Message 4"));

        List<LogEntry> consumer1NewLogs = storage.retrieve("channel1", "consumer1", 10);
        assertEquals(2, consumer1NewLogs.size());
        assertEquals("Channel 1 Message 3", consumer1NewLogs.get(0).getMessage());
        assertEquals("Channel 1 Message 4", consumer1NewLogs.get(1).getMessage());

        List<LogEntry> consumer2NewLogs = storage.retrieve("channel1", "consumer2", 10);
        assertEquals(1, consumer2NewLogs.size());
        assertEquals("Channel 1 Message 4", consumer2NewLogs.get(0).getMessage());
    }

    @Test
    void largeBatchProcessing_ShouldHandleCorrectly() {
        setupSqliteStorage();

        List<LogEntry> largeBatch = new ArrayList<>();
        for (int i = 1; i <= 1000; i++) {
            largeBatch.add(createLogEntry("batch-channel", LogLevel.INFO, "Batch message " + i));
        }

        long startTime = System.currentTimeMillis();
        assertDoesNotThrow(() -> storage.storeLogs(largeBatch));
        long endTime = System.currentTimeMillis();

        assertTrue(endTime - startTime < 5000, "Large batch processing should be efficient");

        List<LogEntry> allLogs = storage.retrieveAll(1500);
        assertEquals(1000, allLogs.size());

        List<LogEntry> firstPage = storage.retrieve("batch-channel", "consumer1", 100);
        assertEquals(100, firstPage.size());
        assertEquals("Batch message 1", firstPage.get(0).getMessage());

        List<LogEntry> secondPage = storage.retrieve("batch-channel", "consumer1", 100);
        assertEquals(100, secondPage.size());
        assertEquals("Batch message 101", secondPage.get(0).getMessage());
    }

    @Test
    void metadataHandling_ShouldPreserveComplexData() {
        setupFileStorage();

        Map<String, Object> complexMeta = new HashMap<>();
        complexMeta.put("userId", 12345);
        complexMeta.put("userName", "john.doe@example.com");
        complexMeta.put("timestamp", System.currentTimeMillis());
        complexMeta.put("tags", Arrays.asList("urgent", "customer-service", "billing"));

        Map<String, Object> nestedData = new HashMap<>();
        nestedData.put("requestId", "req-abc-123");
        nestedData.put("sessionId", "sess-xyz-789");
        nestedData.put("ipAddress", "192.168.1.100");
        complexMeta.put("request", nestedData);

        LogEntry logWithMeta = new LogEntry("metadata-test", LogLevel.INFO, "Complex metadata test", complexMeta);
        storage.store(logWithMeta);

        List<LogEntry> retrieved = storage.retrieveAll(1);
        assertEquals(1, retrieved.size());

        LogEntry retrievedLog = retrieved.get(0);
        assertNotNull(retrievedLog.getMeta());

        assertEquals(12345, retrievedLog.getMeta().get("userId"));
        assertEquals("john.doe@example.com", retrievedLog.getMeta().get("userName"));

        @SuppressWarnings("unchecked")
        List<String> tags = (List<String>) retrievedLog.getMeta().get("tags");
        assertEquals(3, tags.size());
        assertTrue(tags.contains("urgent"));
        assertTrue(tags.contains("customer-service"));
        assertTrue(tags.contains("billing"));

        @SuppressWarnings("unchecked")
        Map<String, Object> request = (Map<String, Object>) retrievedLog.getMeta().get("request");
        assertEquals("req-abc-123", request.get("requestId"));
        assertEquals("sess-xyz-789", request.get("sessionId"));
        assertEquals("192.168.1.100", request.get("ipAddress"));
    }

    @Test
    void timestampPrecision_ShouldBePreserved() {
        setupSqliteStorage();

        LocalDateTime preciseTime = LocalDateTime.of(2025, 9, 25, 14, 30, 45);
        LogEntry logWithPreciseTime = createLogEntry("timestamp-test", LogLevel.INFO, "Timestamp precision test");
        logWithPreciseTime.setTimestamp(preciseTime);

        storage.store(logWithPreciseTime);

        List<LogEntry> retrieved = storage.retrieve("timestamp-test", "consumer1", 1);
        assertEquals(1, retrieved.size());

        LogEntry retrievedLog = retrieved.get(0);
        assertEquals(preciseTime, retrievedLog.getTimestamp());
    }

    @Test
    void storageTypeSwitch_ShouldWorkWithSameData() {
        LogPilotProperties fileProps = new LogPilotProperties();
        fileProps.getStorage().setType(LogPilotProperties.StorageType.FILE);
        fileProps.getStorage().setDirectory(tempDir.resolve("file-storage").toString());

        LogPilotProperties sqliteProps = new LogPilotProperties();
        sqliteProps.getStorage().setType(LogPilotProperties.StorageType.SQLITE);
        sqliteProps.getStorage().getSqlite().setPath(tempDir.resolve("sqlite-storage.db").toString());

        LogStorage fileStorage = LogStorageFactory.createLogStorage(fileProps);
        LogStorage sqliteStorage = LogStorageFactory.createLogStorage(sqliteProps);

        try {
            List<LogEntry> testLogs = Arrays.asList(
                createLogEntry("test-channel", LogLevel.INFO, "Test message 1"),
                createLogEntry("test-channel", LogLevel.WARN, "Test message 2")
            );

            fileStorage.storeLogs(testLogs);
            sqliteStorage.storeLogs(testLogs);

            List<LogEntry> fileRetrieved = fileStorage.retrieveAll(10);
            List<LogEntry> sqliteRetrieved = sqliteStorage.retrieveAll(10);

            assertEquals(2, fileRetrieved.size());
            assertEquals(2, sqliteRetrieved.size());

            assertEquals(fileRetrieved.get(0).getMessage(), sqliteRetrieved.get(0).getMessage());
            assertEquals(fileRetrieved.get(1).getMessage(), sqliteRetrieved.get(1).getMessage());

        } finally {
            fileStorage.close();
            sqliteStorage.close();
        }
    }

    @Test
    void edgeCases_ShouldBeHandledGracefully() {
        setupFileStorage();

        LogEntry emptyMessage = createLogEntry("edge-cases", LogLevel.INFO, "");
        assertDoesNotThrow(() -> storage.store(emptyMessage));

        String longMessage = "A".repeat(10000);
        LogEntry longMessageEntry = createLogEntry("edge-cases", LogLevel.ERROR, longMessage);
        assertDoesNotThrow(() -> storage.store(longMessageEntry));

        LogEntry specialChars = createLogEntry("edge-cases", LogLevel.WARN, "Special chars: äöü 中文 🚀 \n\t\r");
        assertDoesNotThrow(() -> storage.store(specialChars));

        LogEntry specialChannel = createLogEntry("special/channel:name*with<chars>", LogLevel.DEBUG, "Special channel");
        assertDoesNotThrow(() -> storage.store(specialChannel));

        List<LogEntry> allEdgeCases = storage.retrieveAll(10);
        assertEquals(4, allEdgeCases.size());

        List<String> messages = allEdgeCases.stream()
                .map(LogEntry::getMessage)
                .sorted()
                .toList();

        assertTrue(messages.contains(""));
        assertTrue(messages.contains(longMessage));
        assertTrue(messages.contains("Special chars: äöü 中文 🚀 \n\t\r"));
        assertTrue(messages.contains("Special channel"));
    }

    private void runEndToEndWorkflow(LogStorage storage) {
        storage.store(createLogEntry("test-channel", LogLevel.INFO, "First message"));
        storage.store(createLogEntry("test-channel", LogLevel.WARN, "Second message"));

        List<LogEntry> batchLogs = Arrays.asList(
            createLogEntry("test-channel", LogLevel.ERROR, "Batch message 1"),
            createLogEntry("test-channel", LogLevel.DEBUG, "Batch message 2"),
            createLogEntry("other-channel", LogLevel.INFO, "Other channel message")
        );
        storage.storeLogs(batchLogs);

        List<LogEntry> consumerLogs = storage.retrieve("test-channel", "consumer1", 3);
        assertEquals(3, consumerLogs.size());
        assertEquals("First message", consumerLogs.get(0).getMessage());
        assertEquals("Second message", consumerLogs.get(1).getMessage());
        assertEquals("Batch message 1", consumerLogs.get(2).getMessage());

        List<LogEntry> remainingLogs = storage.retrieve("test-channel", "consumer1", 10);
        assertEquals(1, remainingLogs.size());
        assertEquals("Batch message 2", remainingLogs.get(0).getMessage());

        List<LogEntry> newConsumerLogs = storage.retrieve("test-channel", "consumer2", 10);
        assertEquals(4, newConsumerLogs.size());

        List<LogEntry> allLogs = storage.retrieveAll(10);
        assertEquals(5, allLogs.size());

        Set<LogLevel> levels = new HashSet<>();
        for (LogEntry log : allLogs) {
            levels.add(log.getLevel());
        }
        assertTrue(levels.contains(LogLevel.INFO));
        assertTrue(levels.contains(LogLevel.WARN));
        assertTrue(levels.contains(LogLevel.ERROR));
        assertTrue(levels.contains(LogLevel.DEBUG));
    }

    private void setupFileStorage() {
        LogPilotProperties properties = new LogPilotProperties();
        properties.getStorage().setType(LogPilotProperties.StorageType.FILE);
        properties.getStorage().setDirectory(tempDir.toString());
        storage = LogStorageFactory.createLogStorage(properties);
    }

    private void setupSqliteStorage() {
        LogPilotProperties properties = new LogPilotProperties();
        properties.getStorage().setType(LogPilotProperties.StorageType.SQLITE);
        properties.getStorage().getSqlite().setPath(tempDir.resolve("integration.db").toString());
        storage = LogStorageFactory.createLogStorage(properties);
    }

    private LogEntry createLogEntry(String channel, LogLevel level, String message) {
        return new LogEntry(channel, level, message);
    }
}