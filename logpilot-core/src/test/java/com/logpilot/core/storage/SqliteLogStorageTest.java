package com.logpilot.core.storage;

import com.logpilot.core.model.LogEntry;
import com.logpilot.core.model.LogLevel;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import com.logpilot.core.config.LogPilotProperties;

public class SqliteLogStorageTest {

    @TempDir
    Path tempDir;

    private SqliteLogStorage storage;
    private String dbPath;

    @BeforeEach
    void setUp() {
        dbPath = tempDir.resolve("test.db").toString();

        LogPilotProperties.Storage.Sqlite config = new LogPilotProperties.Storage.Sqlite();
        config.setPath(dbPath);

        storage = new SqliteLogStorage(config);
    }

    @AfterEach
    void tearDown() {
        if (storage != null) {
            storage.close();
        }
    }

    @Test
    void initialize_ShouldCreateDatabase() {
        // Database should be created during construction
        assertNotNull(storage);
    }

    @Test
    void store_WithValidLogEntry_ShouldStoreSuccessfully() {
        LogEntry logEntry = createTestLogEntry("test-channel", LogLevel.INFO, "Test message");

        assertDoesNotThrow(() -> storage.store(logEntry));
    }

    @Test
    void store_WithMetadata_ShouldStoreMetadata() {
        Map<String, Object> meta = new HashMap<>();
        meta.put("userId", 123);
        meta.put("sessionId", "abc-123");

        LogEntry logEntry = new LogEntry("test-channel", LogLevel.INFO, "Test with metadata", meta);

        assertDoesNotThrow(() -> storage.store(logEntry));

        // Retrieve for specific channel
        List<LogEntry> retrieved = storage.retrieve("test-channel", "consumer1", 10);
        assertEquals(1, retrieved.size());

        LogEntry stored = retrieved.get(0);
        assertNotNull(stored.getMeta());
        assertEquals(123, stored.getMeta().get("userId"));
        assertEquals("abc-123", stored.getMeta().get("sessionId"));
    }

    @Test
    void store_WithNullMetadata_ShouldStoreWithoutMeta() {
        LogEntry logEntry = createTestLogEntry("test-channel", LogLevel.INFO, "Test without metadata");
        logEntry.setMeta(null);

        assertDoesNotThrow(() -> storage.store(logEntry));

        List<LogEntry> retrieved = storage.retrieve("test-channel", "consumer1", 10);
        assertEquals(1, retrieved.size());
        assertNull(retrieved.get(0).getMeta());
    }

    @Test
    void storeLogs_WithValidEntries_ShouldStoreBatch() {
        List<LogEntry> entries = Arrays.asList(
                createTestLogEntry("channel1", LogLevel.INFO, "Message 1"),
                createTestLogEntry("channel2", LogLevel.WARN, "Message 2"),
                createTestLogEntry("channel1", LogLevel.ERROR, "Message 3"));

        assertDoesNotThrow(() -> storage.storeLogs(entries));

        List<LogEntry> retrieved1 = storage.retrieve("channel1", "consumer1", 10);
        assertEquals(2, retrieved1.size());

        List<LogEntry> retrieved2 = storage.retrieve("channel2", "consumer1", 10);
        assertEquals(1, retrieved2.size());
    }

    @Test
    void storeLogs_WithEmptyList_ShouldNotFail() {
        assertDoesNotThrow(() -> storage.storeLogs(new ArrayList<>()));
        assertDoesNotThrow(() -> storage.storeLogs(null));
    }

    @Test
    void retrieve_WithNewConsumer_ShouldReturnAllLogs() {
        // Store some logs
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

        // First retrieval should get all logs
        List<LogEntry> firstRetrieval = storage.retrieve("test-channel", "consumer1", 10);
        assertEquals(2, firstRetrieval.size());

        // Store more logs
        List<LogEntry> newEntries = Arrays.asList(
                createTestLogEntry("test-channel", LogLevel.ERROR, "Message 3"),
                createTestLogEntry("test-channel", LogLevel.DEBUG, "Message 4"));
        storage.storeLogs(newEntries);

        // Second retrieval should get only new logs
        List<LogEntry> secondRetrieval = storage.retrieve("test-channel", "consumer1", 10);
        assertEquals(2, secondRetrieval.size());
        assertEquals("Message 3", secondRetrieval.get(0).getMessage());
        assertEquals("Message 4", secondRetrieval.get(1).getMessage());
    }

    @Test
    void retrieve_WithLimit_ShouldRespectLimit() {
        // Store 5 logs
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
    void retrieve_WithDifferentChannels_ShouldFilterByChannel() {
        storage.store(createTestLogEntry("channel1", LogLevel.INFO, "Channel 1 message"));
        storage.store(createTestLogEntry("channel2", LogLevel.INFO, "Channel 2 message"));
        storage.store(createTestLogEntry("channel1", LogLevel.WARN, "Another channel 1 message"));

        List<LogEntry> channel1Logs = storage.retrieve("channel1", "consumer1", 10);
        List<LogEntry> channel2Logs = storage.retrieve("channel2", "consumer2", 10);

        assertEquals(2, channel1Logs.size());
        assertEquals(1, channel2Logs.size());

        assertEquals("channel1", channel1Logs.get(0).getChannel());
        assertEquals("channel1", channel1Logs.get(1).getChannel());
        assertEquals("channel2", channel2Logs.get(0).getChannel());
    }

    @Test
    void multipleConsumers_ShouldHaveIndependentOffsets() {
        // Store logs
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

        // Consumer 1 should get remaining logs from before + new log
        List<LogEntry> consumer1Second = storage.retrieve("test-channel", "consumer1", 10);
        assertEquals(2, consumer1Second.size()); // Message 3 + Message 4

        // Consumer 2 should get only new log
        List<LogEntry> consumer2Second = storage.retrieve("test-channel", "consumer2", 10);
        assertEquals(1, consumer2Second.size()); // Only Message 4
        assertEquals("Message 4", consumer2Second.get(0).getMessage());
    }

    @Test
    void close_ShouldCloseConnection() {
        assertDoesNotThrow(() -> storage.close());

        // Trying to use storage after close should throw exception
        LogEntry logEntry = createTestLogEntry("test", LogLevel.INFO, "test");
        assertThrows(RuntimeException.class, () -> storage.store(logEntry));
    }

    @Test
    void storageTimestampPersistence_ShouldMaintainTimestamp() {
        LocalDateTime specificTime = LocalDateTime.of(2025, 9, 25, 10, 30, 45);
        LogEntry originalEntry = createTestLogEntry("test-channel", LogLevel.INFO, "Timestamp test");
        originalEntry.setTimestamp(specificTime);

        storage.store(originalEntry);

        List<LogEntry> retrieved = storage.retrieve("test-channel", "consumer1", 1);
        assertEquals(1, retrieved.size());

        LogEntry storedEntry = retrieved.get(0);
        assertEquals(specificTime, storedEntry.getTimestamp());
    }

    private LogEntry createTestLogEntry(String channel, LogLevel level, String message) {
        return new LogEntry(channel, level, message);
    }
}