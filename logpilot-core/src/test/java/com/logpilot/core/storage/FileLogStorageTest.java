package com.logpilot.core.storage;

import com.logpilot.core.model.LogEntry;
import com.logpilot.core.model.LogLevel;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

public class FileLogStorageTest {

    @TempDir
    Path tempDir;

    private FileLogStorage storage;

    @BeforeEach
    void setUp() {
        storage = new FileLogStorage(tempDir.toString());
    }

    @AfterEach
    void tearDown() {
        if (storage != null) {
            storage.close();
        }
    }

    @Test
    void initialize_ShouldCreateStorageDirectory() throws IOException {
        assertTrue(Files.exists(tempDir));
        assertTrue(Files.isDirectory(tempDir));

        Path offsetsDir = tempDir.resolve(".offsets");
        assertTrue(Files.exists(offsetsDir));
        assertTrue(Files.isDirectory(offsetsDir));
    }

    @Test
    void store_WithValidLogEntry_ShouldCreateLogFile() throws IOException {
        LogEntry logEntry = createTestLogEntry("test-channel", LogLevel.INFO, "Test message");

        storage.store(logEntry);

        Path logFile = tempDir.resolve("test-channel.log");
        assertTrue(Files.exists(logFile));

        List<String> lines = Files.readAllLines(logFile);
        assertEquals(1, lines.size());
        assertTrue(lines.get(0).contains("Test message"));
        assertTrue(lines.get(0).contains("INFO"));
        assertTrue(lines.get(0).contains("test-channel"));
    }

    @Test
    void store_WithSpecialCharactersInChannel_ShouldSanitizeFilename() {
        LogEntry logEntry = createTestLogEntry("test/channel:with*special<chars>", LogLevel.INFO, "Test");

        assertDoesNotThrow(() -> storage.store(logEntry));

        // Should create a sanitized filename
        Path sanitizedFile = tempDir.resolve("test_channel_with_special_chars_.log");
        assertTrue(Files.exists(sanitizedFile));
    }

    @Test
    void store_WithMetadata_ShouldStoreMetadataAsJson() throws IOException {
        Map<String, Object> meta = new HashMap<>();
        meta.put("userId", 123);
        meta.put("traceId", "trace-abc-123");
        meta.put("nested", Map.of("key", "value"));

        LogEntry logEntry = new LogEntry("test-channel", LogLevel.WARN, "Test with metadata", meta);

        storage.store(logEntry);

        Path logFile = tempDir.resolve("test-channel.log");
        List<String> lines = Files.readAllLines(logFile);
        assertEquals(1, lines.size());

        String logLine = lines.get(0);
        assertTrue(logLine.contains("\"userId\":123"));
        assertTrue(logLine.contains("\"traceId\":\"trace-abc-123\""));
        assertTrue(logLine.contains("\"meta\""));
    }

    @Test
    void storeLogs_WithMultipleEntries_ShouldStoreBatch() throws IOException {
        List<LogEntry> entries = Arrays.asList(
                createTestLogEntry("channel1", LogLevel.INFO, "Message 1"),
                createTestLogEntry("channel1", LogLevel.WARN, "Message 2"),
                createTestLogEntry("channel2", LogLevel.ERROR, "Message 3"));

        storage.storeLogs(entries);

        // Check channel1 file
        Path channel1File = tempDir.resolve("channel1.log");
        assertTrue(Files.exists(channel1File));
        List<String> channel1Lines = Files.readAllLines(channel1File);
        assertEquals(2, channel1Lines.size());

        // Check channel2 file
        Path channel2File = tempDir.resolve("channel2.log");
        assertTrue(Files.exists(channel2File));
        List<String> channel2Lines = Files.readAllLines(channel2File);
        assertEquals(1, channel2Lines.size());
    }

    @Test
    void storeLogs_WithEmptyList_ShouldNotFail() {
        assertDoesNotThrow(() -> storage.storeLogs(new ArrayList<>()));
        assertDoesNotThrow(() -> storage.storeLogs(null));
    }

    @Test
    void retrieve_WithNewConsumer_ShouldReturnAllLogs() {
        List<LogEntry> entries = Arrays.asList(
                createTestLogEntry("test-channel", LogLevel.INFO, "Message 1"),
                createTestLogEntry("test-channel", LogLevel.WARN, "Message 2"),
                createTestLogEntry("test-channel", LogLevel.ERROR, "Message 3"));
        storage.storeLogs(entries);

        List<LogEntry> retrieved = storage.retrieve("test-channel", "consumer1", 10);

        assertEquals(3, retrieved.size());
        assertEquals("Message 1", retrieved.get(0).getMessage());
        assertEquals("Message 2", retrieved.get(1).getMessage());
        assertEquals("Message 3", retrieved.get(2).getMessage());
    }

    @Test
    void retrieve_WithExistingConsumer_ShouldReturnOnlyNewLogs() {
        // Store initial logs
        List<LogEntry> initialEntries = Arrays.asList(
                createTestLogEntry("test-channel", LogLevel.INFO, "Message 1"),
                createTestLogEntry("test-channel", LogLevel.WARN, "Message 2"));
        storage.storeLogs(initialEntries);

        // First retrieval
        List<LogEntry> firstRetrieval = storage.retrieve("test-channel", "consumer1", 10);
        assertEquals(2, firstRetrieval.size());

        // Store more logs
        storage.store(createTestLogEntry("test-channel", LogLevel.ERROR, "Message 3"));

        // Second retrieval should get only new logs
        List<LogEntry> secondRetrieval = storage.retrieve("test-channel", "consumer1", 10);
        assertEquals(1, secondRetrieval.size());
        assertEquals("Message 3", secondRetrieval.get(0).getMessage());
    }

    @Test
    void retrieve_WithLimit_ShouldRespectLimit() {
        List<LogEntry> entries = new ArrayList<>();
        for (int i = 1; i <= 5; i++) {
            entries.add(createTestLogEntry("test-channel", LogLevel.INFO, "Message " + i));
        }
        storage.storeLogs(entries);

        List<LogEntry> retrieved = storage.retrieve("test-channel", "consumer1", 3);

        assertEquals(3, retrieved.size());
        assertEquals("Message 1", retrieved.get(0).getMessage());
        assertEquals("Message 2", retrieved.get(1).getMessage());
        assertEquals("Message 3", retrieved.get(2).getMessage());
    }

    @Test
    void retrieve_WithNonexistentChannel_ShouldReturnEmptyList() {
        List<LogEntry> retrieved = storage.retrieve("nonexistent-channel", "consumer1", 10);

        assertNotNull(retrieved);
        assertTrue(retrieved.isEmpty());
    }

    @Test
    void multipleConsumers_ShouldHaveIndependentOffsets() {
        List<LogEntry> entries = Arrays.asList(
                createTestLogEntry("test-channel", LogLevel.INFO, "Message 1"),
                createTestLogEntry("test-channel", LogLevel.WARN, "Message 2"),
                createTestLogEntry("test-channel", LogLevel.ERROR, "Message 3"));
        storage.storeLogs(entries);

        // Consumer 1 reads 2 logs
        List<LogEntry> consumer1First = storage.retrieve("test-channel", "consumer1", 2);
        assertEquals(2, consumer1First.size());

        // Consumer 2 reads all logs
        List<LogEntry> consumer2First = storage.retrieve("test-channel", "consumer2", 10);
        assertEquals(3, consumer2First.size());

        // Add more logs
        storage.store(createTestLogEntry("test-channel", LogLevel.DEBUG, "Message 4"));

        // Consumer 1 should get remaining + new logs
        List<LogEntry> consumer1Second = storage.retrieve("test-channel", "consumer1", 10);
        assertEquals(2, consumer1Second.size());

        // Consumer 2 should get only new log
        List<LogEntry> consumer2Second = storage.retrieve("test-channel", "consumer2", 10);
        assertEquals(1, consumer2Second.size());
        assertEquals("Message 4", consumer2Second.get(0).getMessage());
    }

    @Test
    void consumerOffsetPersistence_ShouldSurviveRestart() throws IOException {
        // Store logs and consume some
        List<LogEntry> entries = Arrays.asList(
                createTestLogEntry("test-channel", LogLevel.INFO, "Message 1"),
                createTestLogEntry("test-channel", LogLevel.WARN, "Message 2"),
                createTestLogEntry("test-channel", LogLevel.ERROR, "Message 3"));
        storage.storeLogs(entries);

        storage.retrieve("test-channel", "persistent-consumer", 2);

        // Close and recreate storage
        storage.close();
        storage = new FileLogStorage(tempDir.toString());

        // Add new log
        storage.store(createTestLogEntry("test-channel", LogLevel.DEBUG, "Message 4"));

        // Consumer should get remaining logs from before + new log
        List<LogEntry> retrieved = storage.retrieve("test-channel", "persistent-consumer", 10);
        assertEquals(2, retrieved.size());
        assertEquals("Message 3", retrieved.get(0).getMessage());
        assertEquals("Message 4", retrieved.get(1).getMessage());
    }

    @Test
    void timestampPersistence_ShouldMaintainTimestamp() {
        LocalDateTime specificTime = LocalDateTime.of(2025, 9, 25, 14, 30, 45);
        LogEntry originalEntry = createTestLogEntry("test-channel", LogLevel.INFO, "Timestamp test");
        originalEntry.setTimestamp(specificTime);

        storage.store(originalEntry);

        List<LogEntry> retrieved = storage.retrieve("test-channel", "consumer1", 1);
        assertEquals(1, retrieved.size());

        LogEntry storedEntry = retrieved.get(0);
        assertEquals(specificTime, storedEntry.getTimestamp());
    }

    @Test
    void concurrentAccess_ShouldHandleMultipleOperations() {
        // This test verifies that concurrent operations don't cause issues
        // In a real scenario, you might want to use actual threads, but for unit tests
        // we'll just verify that multiple operations work correctly

        LogEntry entry1 = createTestLogEntry("channel1", LogLevel.INFO, "Message 1");
        LogEntry entry2 = createTestLogEntry("channel2", LogLevel.WARN, "Message 2");

        storage.store(entry1);
        storage.store(entry2);

        List<LogEntry> channel1Logs = storage.retrieve("channel1", "consumer1", 10);
        List<LogEntry> channel2Logs = storage.retrieve("channel2", "consumer2", 10);

        assertEquals(1, channel1Logs.size());
        assertEquals(1, channel2Logs.size());
        assertEquals("Message 1", channel1Logs.get(0).getMessage());
        assertEquals("Message 2", channel2Logs.get(0).getMessage());
    }

    @Test
    void malformedLogLine_ShouldBeSkippedGracefully() throws IOException {
        // Create a log file with malformed JSON
        Path logFile = tempDir.resolve("test-channel.log");
        Files.write(logFile, Arrays.asList(
                "{\"valid\":\"json\",\"channel\":\"test-channel\",\"level\":\"INFO\",\"message\":\"Valid message\",\"timestamp\":\"2025-09-25T10:30:00\"}",
                "this is not valid json",
                "{\"another\":\"valid\",\"channel\":\"test-channel\",\"level\":\"WARN\",\"message\":\"Another valid message\",\"timestamp\":\"2025-09-25T10:31:00\"}"));

        List<LogEntry> retrieved = storage.retrieve("test-channel", "consumer1", 10);

        // Should only get valid entries
        assertEquals(2, retrieved.size());
        assertEquals("Valid message", retrieved.get(0).getMessage());
        assertEquals("Another valid message", retrieved.get(1).getMessage());
    }

    @Test
    void close_ShouldSaveConsumerOffsets() throws IOException {
        // Store logs and consume some
        storage.storeLogs(Arrays.asList(
                createTestLogEntry("test-channel", LogLevel.INFO, "Message 1"),
                createTestLogEntry("test-channel", LogLevel.WARN, "Message 2")));

        storage.retrieve("test-channel", "test-consumer", 1);

        // Close should save offsets
        storage.close();

        // Check that offset files exist
        Path offsetsDir = tempDir.resolve(".offsets");
        assertTrue(Files.exists(offsetsDir));

        // Check that there's an offset file (the exact filename depends on
        // sanitization)
        try (var pathStream = Files.list(offsetsDir)) {
            long offsetFileCount = pathStream.filter(path -> path.toString().endsWith(".offset")).count();
            assertTrue(offsetFileCount > 0);
        }
    }

    private LogEntry createTestLogEntry(String channel, LogLevel level, String message) {
        return new LogEntry(channel, level, message);
    }
}