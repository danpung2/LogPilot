package com.logpilot.core.service;

import com.logpilot.core.model.LogEntry;
import com.logpilot.core.model.LogLevel;
import com.logpilot.core.storage.LogStorage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class LogServiceTest {

    @Mock
    private LogStorage mockLogStorage;

    private LogService logService;

    @BeforeEach
    void setUp() {
        // Create a concrete implementation of LogService for testing
        logService = new TestLogService(mockLogStorage);
    }

    @Test
    void storeLog_WithValidLogEntry_ShouldCallStorage() {
        LogEntry logEntry = createTestLogEntry("test-channel", LogLevel.INFO, "Test message");

        logService.storeLog(logEntry);

        verify(mockLogStorage, times(1)).store(logEntry);
    }

    @Test
    void storeLog_WithNullLogEntry_ShouldThrowException() {
        assertThrows(IllegalArgumentException.class, () -> logService.storeLog(null));
        verify(mockLogStorage, never()).store(any());
    }

    @Test
    void storeLogs_WithValidEntries_ShouldCallStorage() {
        List<LogEntry> logEntries = Arrays.asList(
                createTestLogEntry("channel1", LogLevel.INFO, "Message 1"),
                createTestLogEntry("channel2", LogLevel.WARN, "Message 2"));

        logService.storeLogs(logEntries);

        verify(mockLogStorage, times(1)).storeLogs(logEntries);
    }

    @Test
    void storeLogs_WithEmptyList_ShouldCallStorageWithEmptyList() {
        List<LogEntry> emptyList = Arrays.asList();

        logService.storeLogs(emptyList);

        verify(mockLogStorage, times(1)).storeLogs(emptyList);
    }

    @Test
    void storeLogs_WithNullList_ShouldThrowException() {
        assertThrows(IllegalArgumentException.class, () -> logService.storeLogs(null));
        verify(mockLogStorage, never()).storeLogs(any());
    }

    @Test
    void getLogsForConsumer_WithValidParameters_ShouldCallStorage() {
        String channel = "test-channel";
        String consumerId = "consumer-123";
        int limit = 50;

        List<LogEntry> expectedLogs = Arrays.asList(
                createTestLogEntry(channel, LogLevel.INFO, "Message 1"),
                createTestLogEntry(channel, LogLevel.WARN, "Message 2"));

        when(mockLogStorage.retrieve(channel, consumerId, limit)).thenReturn(expectedLogs);

        List<LogEntry> actualLogs = logService.getLogsForConsumer(channel, consumerId, limit);

        assertEquals(expectedLogs, actualLogs);
        verify(mockLogStorage, times(1)).retrieve(channel, consumerId, limit);
    }

    @Test
    void getLogsForConsumer_WithNullChannel_ShouldThrowException() {
        assertThrows(IllegalArgumentException.class,
                () -> logService.getLogsForConsumer(null, "consumer", 10));
        verify(mockLogStorage, never()).retrieve(any(), any(), anyInt());
    }

    @Test
    void getLogsForConsumer_WithEmptyChannel_ShouldThrowException() {
        assertThrows(IllegalArgumentException.class,
                () -> logService.getLogsForConsumer("", "consumer", 10));
        verify(mockLogStorage, never()).retrieve(any(), any(), anyInt());
    }

    @Test
    void getLogsForConsumer_WithNullConsumerId_ShouldThrowException() {
        assertThrows(IllegalArgumentException.class,
                () -> logService.getLogsForConsumer("channel", null, 10));
        verify(mockLogStorage, never()).retrieve(any(), any(), anyInt());
    }

    @Test
    void getLogsForConsumer_WithEmptyConsumerId_ShouldThrowException() {
        assertThrows(IllegalArgumentException.class,
                () -> logService.getLogsForConsumer("channel", "", 10));
        verify(mockLogStorage, never()).retrieve(any(), any(), anyInt());
    }

    @Test
    void getLogsForConsumer_WithNegativeLimit_ShouldThrowException() {
        assertThrows(IllegalArgumentException.class,
                () -> logService.getLogsForConsumer("channel", "consumer", -1));
        verify(mockLogStorage, never()).retrieve(any(), any(), anyInt());
    }

    @Test
    void getLogsForConsumer_WithZeroLimit_ShouldCallStorage() {
        String channel = "test-channel";
        String consumerId = "consumer-123";
        int limit = 0;

        when(mockLogStorage.retrieve(channel, consumerId, limit)).thenReturn(Arrays.asList());

        List<LogEntry> actualLogs = logService.getLogsForConsumer(channel, consumerId, limit);

        assertNotNull(actualLogs);
        assertTrue(actualLogs.isEmpty());
        verify(mockLogStorage, times(1)).retrieve(channel, consumerId, limit);
    }

    @Test
    void getAllLogs_WithValidLimit_ShouldCallStorage() {
        int limit = 100;
        List<LogEntry> expectedLogs = Arrays.asList(
                createTestLogEntry("channel1", LogLevel.INFO, "Message 1"),
                createTestLogEntry("channel2", LogLevel.ERROR, "Message 2"),
                createTestLogEntry("channel3", LogLevel.DEBUG, "Message 3"));

        when(mockLogStorage.retrieveAll(limit)).thenReturn(expectedLogs);

        List<LogEntry> actualLogs = logService.getAllLogs(limit);

        assertEquals(expectedLogs, actualLogs);
        verify(mockLogStorage, times(1)).retrieveAll(limit);
    }

    @Test
    void getAllLogs_WithNegativeLimit_ShouldThrowException() {
        assertThrows(IllegalArgumentException.class, () -> logService.getAllLogs(-1));
        verify(mockLogStorage, never()).retrieveAll(anyInt());
    }

    @Test
    void getAllLogs_WithZeroLimit_ShouldCallStorage() {
        int limit = 0;

        when(mockLogStorage.retrieveAll(limit)).thenReturn(Arrays.asList());

        List<LogEntry> actualLogs = logService.getAllLogs(limit);

        assertNotNull(actualLogs);
        assertTrue(actualLogs.isEmpty());
        verify(mockLogStorage, times(1)).retrieveAll(limit);
    }

    @Test
    void getAllLogs_WithLargeLimit_ShouldCallStorage() {
        int limit = Integer.MAX_VALUE;

        when(mockLogStorage.retrieveAll(limit)).thenReturn(Arrays.asList());

        List<LogEntry> actualLogs = logService.getAllLogs(limit);

        assertNotNull(actualLogs);
        verify(mockLogStorage, times(1)).retrieveAll(limit);
    }

    private LogEntry createTestLogEntry(String channel, LogLevel level, String message) {
        return new LogEntry(channel, level, message);
    }

    // Test implementation of LogService with validation
    private static class TestLogService implements LogService {
        private final LogStorage logStorage;

        public TestLogService(LogStorage logStorage) {
            this.logStorage = logStorage;
        }

        @Override
        public void storeLog(LogEntry logEntry) {
            if (logEntry == null) {
                throw new IllegalArgumentException("LogEntry cannot be null");
            }
            logStorage.store(logEntry);
        }

        @Override
        public void storeLogs(List<LogEntry> logEntries) {
            if (logEntries == null) {
                throw new IllegalArgumentException("LogEntries list cannot be null");
            }
            logStorage.storeLogs(logEntries);
        }

        @Override
        public List<LogEntry> getLogsForConsumer(String channel, String consumerId, int limit) {
            if (channel == null || channel.trim().isEmpty()) {
                throw new IllegalArgumentException("Channel cannot be null or empty");
            }
            if (consumerId == null || consumerId.trim().isEmpty()) {
                throw new IllegalArgumentException("ConsumerId cannot be null or empty");
            }
            if (limit < 0) {
                throw new IllegalArgumentException("Limit cannot be negative");
            }
            return logStorage.retrieve(channel, consumerId, limit);
        }

        @Override
        public List<LogEntry> getAllLogs(int limit) {
            if (limit < 0) {
                throw new IllegalArgumentException("Limit cannot be negative");
            }
            return logStorage.retrieveAll(limit);
        }

        @Override
        public void commitLogOffset(String channel, String consumerId, long lastLogId) {
            logStorage.commitOffset(channel, consumerId, lastLogId);
        }

        @Override
        public void seekToBeginning(String channel, String consumerId) {
            logStorage.seekToBeginning(channel, consumerId);
        }

        @Override
        public void seekToEnd(String channel, String consumerId) {
            logStorage.seekToEnd(channel, consumerId);
        }

        @Override
        public void seekToId(String channel, String consumerId, long logId) {
            logStorage.seekToId(channel, consumerId, logId);
        }
    }
}