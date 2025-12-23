package com.logpilot.server.rest.service;

import com.logpilot.core.model.LogEntry;
import com.logpilot.core.model.LogLevel;
import com.logpilot.core.storage.LogStorage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class RestLogServiceTest {

    @Mock
    private LogStorage logStorage;

    private RestLogService restLogService;
    private LogEntry testLogEntry;
    private List<LogEntry> testLogEntries;

    @BeforeEach
    void setUp() {
        restLogService = new RestLogService(logStorage);

        testLogEntry = new LogEntry("test-channel", LogLevel.INFO, "Test message");

        testLogEntries = Arrays.asList(
                new LogEntry("channel1", LogLevel.INFO, "Message 1"),
                new LogEntry("channel2", LogLevel.ERROR, "Message 2"));
    }

    @Test
    void restLogService_ShouldHaveCorrectAnnotations() {
        Class<RestLogService> serviceClass = RestLogService.class;

        assertTrue(serviceClass.isAnnotationPresent(Service.class));
        assertTrue(serviceClass.isAnnotationPresent(ConditionalOnExpression.class));

        Service serviceAnnotation = serviceClass.getAnnotation(Service.class);
        assertEquals("restLogService", serviceAnnotation.value());

        ConditionalOnExpression conditional = serviceClass.getAnnotation(ConditionalOnExpression.class);
        String expectedExpression = "'${logpilot.server.protocol:all}' == 'rest' or '${logpilot.server.protocol:all}' == 'all'";
        assertEquals(expectedExpression, conditional.value());
    }

    @Test
    void constructor_WithLogStorage_ShouldCreateInstance() {
        assertNotNull(restLogService);
        assertDoesNotThrow(() -> new RestLogService(logStorage));
    }

    @Test
    void constructor_WithNullLogStorage_ShouldThrowException() {
        assertThrows(NullPointerException.class, () -> {
            new RestLogService(null);
        });
    }

    @Test
    void storeLog_WithValidLogEntry_ShouldCallLogStorage() {
        restLogService.storeLog(testLogEntry);

        verify(logStorage, times(1)).store(testLogEntry);
    }

    @Test
    void storeLog_WithNullLogEntry_ShouldCallLogStorageWithNull() {
        restLogService.storeLog(null);

        verify(logStorage, times(1)).store(null);
    }

    @Test
    void storeLog_WhenStorageThrowsException_ShouldPropagateException() {
        doThrow(new RuntimeException("Storage error")).when(logStorage).store(any(LogEntry.class));

        assertThrows(RuntimeException.class, () -> {
            restLogService.storeLog(testLogEntry);
        });

        verify(logStorage, times(1)).store(testLogEntry);
    }

    @Test
    void storeLogs_WithValidLogEntries_ShouldCallLogStorage() {
        restLogService.storeLogs(testLogEntries);

        verify(logStorage, times(1)).storeLogs(testLogEntries);
    }

    @Test
    void storeLogs_WithEmptyList_ShouldCallLogStorage() {
        List<LogEntry> emptyList = Collections.emptyList();

        restLogService.storeLogs(emptyList);

        verify(logStorage, times(1)).storeLogs(emptyList);
    }

    @Test
    void storeLogs_WithNullList_ShouldCallLogStorageWithNull() {
        restLogService.storeLogs(null);

        verify(logStorage, times(1)).storeLogs(null);
    }

    @Test
    void storeLogs_WhenStorageThrowsException_ShouldPropagateException() {
        doThrow(new RuntimeException("Storage error")).when(logStorage).storeLogs(anyList());

        assertThrows(RuntimeException.class, () -> {
            restLogService.storeLogs(testLogEntries);
        });

        verify(logStorage, times(1)).storeLogs(testLogEntries);
    }

    @Test
    void getLogsForConsumer_WithValidParameters_ShouldCallLogStorage() {
        when(logStorage.retrieve("test-channel", "consumer1", 100, true)).thenReturn(testLogEntries);

        List<LogEntry> result = restLogService.getLogsForConsumer("test-channel", "consumer1", 100);

        assertEquals(testLogEntries, result);
        verify(logStorage, times(1)).retrieve("test-channel", "consumer1", 100, true);
    }

    @Test
    void getLogsForConsumer_WithNullChannel_ShouldCallLogStorage() {
        when(logStorage.retrieve(null, "consumer1", 100, true)).thenReturn(Collections.emptyList());

        List<LogEntry> result = restLogService.getLogsForConsumer(null, "consumer1", 100);

        assertNotNull(result);
        verify(logStorage, times(1)).retrieve(null, "consumer1", 100, true);
    }

    @Test
    void getLogsForConsumer_WithNullConsumerId_ShouldCallLogStorage() {
        when(logStorage.retrieve("test-channel", null, 100, true)).thenReturn(testLogEntries);

        List<LogEntry> result = restLogService.getLogsForConsumer("test-channel", null, 100);

        assertEquals(testLogEntries, result);
        verify(logStorage, times(1)).retrieve("test-channel", null, 100, true);
    }

    @Test
    void getLogsForConsumer_WithZeroLimit_ShouldCallLogStorage() {
        when(logStorage.retrieve("test-channel", "consumer1", 0, true)).thenReturn(Collections.emptyList());

        List<LogEntry> result = restLogService.getLogsForConsumer("test-channel", "consumer1", 0);

        assertNotNull(result);
        verify(logStorage, times(1)).retrieve("test-channel", "consumer1", 0, true);
    }

    @Test
    void getLogsForConsumer_WhenStorageThrowsException_ShouldPropagateException() {
        when(logStorage.retrieve(anyString(), anyString(), anyInt(), anyBoolean()))
                .thenThrow(new RuntimeException("Retrieval error"));

        assertThrows(RuntimeException.class, () -> {
            restLogService.getLogsForConsumer("test-channel", "consumer1", 100);
        });

        verify(logStorage, times(1)).retrieve("test-channel", "consumer1", 100, true);
    }

    @Test
    void getAllLogs_WithValidLimit_ShouldCallLogStorage() {
        when(logStorage.retrieveAll(100)).thenReturn(testLogEntries);

        List<LogEntry> result = restLogService.getAllLogs(100);

        assertEquals(testLogEntries, result);
        verify(logStorage, times(1)).retrieveAll(100);
    }

    @Test
    void getAllLogs_WithZeroLimit_ShouldCallLogStorage() {
        when(logStorage.retrieveAll(0)).thenReturn(Collections.emptyList());

        List<LogEntry> result = restLogService.getAllLogs(0);

        assertNotNull(result);
        verify(logStorage, times(1)).retrieveAll(0);
    }

    @Test
    void getAllLogs_WithNegativeLimit_ShouldCallLogStorage() {
        when(logStorage.retrieveAll(-1)).thenReturn(Collections.emptyList());

        List<LogEntry> result = restLogService.getAllLogs(-1);

        assertNotNull(result);
        verify(logStorage, times(1)).retrieveAll(-1);
    }

    @Test
    void getAllLogs_WhenStorageThrowsException_ShouldPropagateException() {
        when(logStorage.retrieveAll(anyInt())).thenThrow(new RuntimeException("Retrieval error"));

        assertThrows(RuntimeException.class, () -> {
            restLogService.getAllLogs(100);
        });

        verify(logStorage, times(1)).retrieveAll(100);
    }

    @Test
    void getAllLogs_WithLargeLimit_ShouldCallLogStorage() {
        when(logStorage.retrieveAll(10000)).thenReturn(testLogEntries);

        List<LogEntry> result = restLogService.getAllLogs(10000);

        assertEquals(testLogEntries, result);
        verify(logStorage, times(1)).retrieveAll(10000);
    }

    @Test
    void service_ShouldDelegateAllCallsToLogStorage() {
        when(logStorage.retrieveAll(anyInt())).thenReturn(testLogEntries);
        when(logStorage.retrieve(anyString(), anyString(), anyInt(), anyBoolean())).thenReturn(testLogEntries);

        restLogService.storeLog(testLogEntry);
        restLogService.storeLogs(testLogEntries);
        restLogService.getAllLogs(50);
        restLogService.getLogsForConsumer("channel", "consumer", 25);

        verify(logStorage, times(1)).store(testLogEntry);
        verify(logStorage, times(1)).storeLogs(testLogEntries);
        verify(logStorage, times(1)).retrieveAll(50);
        verify(logStorage, times(1)).retrieve("channel", "consumer", 25, true);

        verifyNoMoreInteractions(logStorage);
    }

    @Test
    void service_ShouldHandleMultipleConsecutiveCalls() {
        when(logStorage.retrieveAll(anyInt())).thenReturn(testLogEntries);

        restLogService.storeLog(testLogEntry);
        restLogService.storeLog(testLogEntry);
        restLogService.getAllLogs(100);
        restLogService.getAllLogs(200);

        verify(logStorage, times(2)).store(testLogEntry);
        verify(logStorage, times(1)).retrieveAll(100);
        verify(logStorage, times(1)).retrieveAll(200);
    }

    @Test
    void service_ShouldMaintainLogStorageReference() {
        assertNotNull(restLogService);

        LogEntry newEntry = new LogEntry("new-channel", LogLevel.DEBUG, "New message");
        restLogService.storeLog(newEntry);

        verify(logStorage, times(1)).store(newEntry);
    }

    @Test
    void service_ShouldHandleComplexLogEntries() {
        LogEntry complexEntry = LogEntry.builder()
                .channel("complex-channel")
                .level(LogLevel.ERROR)
                .message("Complex error message")
                .meta(java.util.Map.of("errorCode", 500, "userId", "user123"))
                .build();

        restLogService.storeLog(complexEntry);

        verify(logStorage, times(1)).store(complexEntry);
    }
}