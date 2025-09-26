package com.logpilot.server.grpc.handler;

import com.logpilot.core.model.LogEntry;
import com.logpilot.core.model.LogLevel;
import com.logpilot.core.storage.LogStorage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class GrpcLogHandlerTest {

    @Mock
    private LogStorage logStorage;

    private GrpcLogHandler grpcLogHandler;
    private LogEntry testLogEntry;
    private List<LogEntry> testLogEntries;

    @BeforeEach
    void setUp() {
        grpcLogHandler = new GrpcLogHandler(logStorage);

        testLogEntry = new LogEntry("test-channel", LogLevel.INFO, "Test message");

        testLogEntries = Arrays.asList(
            new LogEntry("channel1", LogLevel.INFO, "Message 1"),
            new LogEntry("channel2", LogLevel.ERROR, "Message 2")
        );
    }

    @Test
    void grpcLogHandler_ShouldHaveCorrectAnnotations() {
        Class<GrpcLogHandler> handlerClass = GrpcLogHandler.class;

        assertTrue(handlerClass.isAnnotationPresent(Component.class));
        assertTrue(handlerClass.isAnnotationPresent(ConditionalOnExpression.class));

        Component componentAnnotation = handlerClass.getAnnotation(Component.class);
        assertEquals("grpcLogHandler", componentAnnotation.value());

        ConditionalOnExpression conditional = handlerClass.getAnnotation(ConditionalOnExpression.class);
        String expectedExpression = "'${logpilot.server.protocol:all}' == 'grpc' or '${logpilot.server.protocol:all}' == 'all'";
        assertEquals(expectedExpression, conditional.value());
    }

    @Test
    void constructor_WithLogStorage_ShouldCreateInstance() {
        assertNotNull(grpcLogHandler);
        assertDoesNotThrow(() -> new GrpcLogHandler(logStorage));
    }

    @Test
    void constructor_WithNullLogStorage_ShouldThrowException() {
        assertThrows(NullPointerException.class, () -> {
            new GrpcLogHandler(null);
        });
    }

    @Test
    void storeLog_WithValidLogEntry_ShouldCallLogStorage() {
        grpcLogHandler.storeLog(testLogEntry);

        verify(logStorage, times(1)).store(testLogEntry);
    }

    @Test
    void storeLog_WithNullLogEntry_ShouldCallLogStorageWithNull() {
        grpcLogHandler.storeLog(null);

        verify(logStorage, times(1)).store(null);
    }

    @Test
    void storeLog_WhenStorageThrowsException_ShouldPropagateException() {
        doThrow(new RuntimeException("Storage error")).when(logStorage).store(any(LogEntry.class));

        assertThrows(RuntimeException.class, () -> {
            grpcLogHandler.storeLog(testLogEntry);
        });

        verify(logStorage, times(1)).store(testLogEntry);
    }

    @Test
    void storeLogs_WithValidLogEntries_ShouldCallLogStorage() {
        grpcLogHandler.storeLogs(testLogEntries);

        verify(logStorage, times(1)).storeLogs(testLogEntries);
    }

    @Test
    void storeLogs_WithEmptyList_ShouldCallLogStorage() {
        List<LogEntry> emptyList = Collections.emptyList();

        grpcLogHandler.storeLogs(emptyList);

        verify(logStorage, times(1)).storeLogs(emptyList);
    }

    @Test
    void storeLogs_WithNullList_ShouldCallLogStorageWithNull() {
        grpcLogHandler.storeLogs(null);

        verify(logStorage, times(1)).storeLogs(null);
    }

    @Test
    void storeLogs_WhenStorageThrowsException_ShouldPropagateException() {
        doThrow(new RuntimeException("Storage error")).when(logStorage).storeLogs(anyList());

        assertThrows(RuntimeException.class, () -> {
            grpcLogHandler.storeLogs(testLogEntries);
        });

        verify(logStorage, times(1)).storeLogs(testLogEntries);
    }

    @Test
    void getLogsForConsumer_WithValidParameters_ShouldCallLogStorage() {
        when(logStorage.retrieve("test-channel", "consumer1", 100)).thenReturn(testLogEntries);

        List<LogEntry> result = grpcLogHandler.getLogsForConsumer("test-channel", "consumer1", 100);

        assertEquals(testLogEntries, result);
        verify(logStorage, times(1)).retrieve("test-channel", "consumer1", 100);
    }

    @Test
    void getLogsForConsumer_WithNullChannel_ShouldCallLogStorage() {
        when(logStorage.retrieve(null, "consumer1", 100)).thenReturn(Collections.emptyList());

        List<LogEntry> result = grpcLogHandler.getLogsForConsumer(null, "consumer1", 100);

        assertNotNull(result);
        verify(logStorage, times(1)).retrieve(null, "consumer1", 100);
    }

    @Test
    void getLogsForConsumer_WithNullConsumerId_ShouldCallLogStorage() {
        when(logStorage.retrieve("test-channel", null, 100)).thenReturn(testLogEntries);

        List<LogEntry> result = grpcLogHandler.getLogsForConsumer("test-channel", null, 100);

        assertEquals(testLogEntries, result);
        verify(logStorage, times(1)).retrieve("test-channel", null, 100);
    }

    @Test
    void getLogsForConsumer_WithZeroLimit_ShouldCallLogStorage() {
        when(logStorage.retrieve("test-channel", "consumer1", 0)).thenReturn(Collections.emptyList());

        List<LogEntry> result = grpcLogHandler.getLogsForConsumer("test-channel", "consumer1", 0);

        assertNotNull(result);
        verify(logStorage, times(1)).retrieve("test-channel", "consumer1", 0);
    }

    @Test
    void getLogsForConsumer_WhenStorageThrowsException_ShouldPropagateException() {
        when(logStorage.retrieve(anyString(), anyString(), anyInt())).thenThrow(new RuntimeException("Retrieval error"));

        assertThrows(RuntimeException.class, () -> {
            grpcLogHandler.getLogsForConsumer("test-channel", "consumer1", 100);
        });

        verify(logStorage, times(1)).retrieve("test-channel", "consumer1", 100);
    }

    @Test
    void getAllLogs_WithValidLimit_ShouldCallLogStorage() {
        when(logStorage.retrieveAll(100)).thenReturn(testLogEntries);

        List<LogEntry> result = grpcLogHandler.getAllLogs(100);

        assertEquals(testLogEntries, result);
        verify(logStorage, times(1)).retrieveAll(100);
    }

    @Test
    void getAllLogs_WithZeroLimit_ShouldCallLogStorage() {
        when(logStorage.retrieveAll(0)).thenReturn(Collections.emptyList());

        List<LogEntry> result = grpcLogHandler.getAllLogs(0);

        assertNotNull(result);
        verify(logStorage, times(1)).retrieveAll(0);
    }

    @Test
    void getAllLogs_WithNegativeLimit_ShouldCallLogStorage() {
        when(logStorage.retrieveAll(-1)).thenReturn(Collections.emptyList());

        List<LogEntry> result = grpcLogHandler.getAllLogs(-1);

        assertNotNull(result);
        verify(logStorage, times(1)).retrieveAll(-1);
    }

    @Test
    void getAllLogs_WhenStorageThrowsException_ShouldPropagateException() {
        when(logStorage.retrieveAll(anyInt())).thenThrow(new RuntimeException("Retrieval error"));

        assertThrows(RuntimeException.class, () -> {
            grpcLogHandler.getAllLogs(100);
        });

        verify(logStorage, times(1)).retrieveAll(100);
    }

    @Test
    void getAllLogs_WithLargeLimit_ShouldCallLogStorage() {
        when(logStorage.retrieveAll(10000)).thenReturn(testLogEntries);

        List<LogEntry> result = grpcLogHandler.getAllLogs(10000);

        assertEquals(testLogEntries, result);
        verify(logStorage, times(1)).retrieveAll(10000);
    }

    @Test
    void handler_ShouldDelegateAllCallsToLogStorage() {
        when(logStorage.retrieveAll(anyInt())).thenReturn(testLogEntries);
        when(logStorage.retrieve(anyString(), anyString(), anyInt())).thenReturn(testLogEntries);

        grpcLogHandler.storeLog(testLogEntry);
        grpcLogHandler.storeLogs(testLogEntries);
        grpcLogHandler.getAllLogs(50);
        grpcLogHandler.getLogsForConsumer("channel", "consumer", 25);

        verify(logStorage, times(1)).store(testLogEntry);
        verify(logStorage, times(1)).storeLogs(testLogEntries);
        verify(logStorage, times(1)).retrieveAll(50);
        verify(logStorage, times(1)).retrieve("channel", "consumer", 25);

        verifyNoMoreInteractions(logStorage);
    }

    @Test
    void handler_ShouldHandleMultipleConsecutiveCalls() {
        when(logStorage.retrieveAll(anyInt())).thenReturn(testLogEntries);

        grpcLogHandler.storeLog(testLogEntry);
        grpcLogHandler.storeLog(testLogEntry);
        grpcLogHandler.getAllLogs(100);
        grpcLogHandler.getAllLogs(200);

        verify(logStorage, times(2)).store(testLogEntry);
        verify(logStorage, times(1)).retrieveAll(100);
        verify(logStorage, times(1)).retrieveAll(200);
    }

    @Test
    void handler_ShouldMaintainLogStorageReference() {
        assertNotNull(grpcLogHandler);

        LogEntry newEntry = new LogEntry("new-channel", LogLevel.DEBUG, "New message");
        grpcLogHandler.storeLog(newEntry);

        verify(logStorage, times(1)).store(newEntry);
    }

    @Test
    void handler_ShouldHandleComplexLogEntries() {
        LogEntry complexEntry = LogEntry.builder()
                .channel("complex-channel")
                .level(LogLevel.ERROR)
                .message("Complex error message")
                .meta(java.util.Map.of("errorCode", 500, "userId", "user123"))
                .build();

        grpcLogHandler.storeLog(complexEntry);

        verify(logStorage, times(1)).store(complexEntry);
    }

    @Test
    void handler_ShouldImplementLogServiceInterface() {
        Method[] methods = com.logpilot.core.service.LogService.class.getDeclaredMethods();
        Class<GrpcLogHandler> handlerClass = GrpcLogHandler.class;

        for (Method method : methods) {
            try {
                Method handlerMethod = handlerClass.getMethod(method.getName(), method.getParameterTypes());
                assertNotNull(handlerMethod);
            } catch (NoSuchMethodException e) {
                fail("GrpcLogHandler should implement method: " + method.getName());
            }
        }
    }

    @Test
    void handler_ShouldHaveCorrectBeanName() {
        Component componentAnnotation = GrpcLogHandler.class.getAnnotation(Component.class);
        assertEquals("grpcLogHandler", componentAnnotation.value());
    }

    @Test
    void handler_ShouldBeConditionalOnGrpcProtocol() {
        ConditionalOnExpression conditional = GrpcLogHandler.class.getAnnotation(ConditionalOnExpression.class);
        String expression = conditional.value();

        assertTrue(expression.contains("grpc"));
        assertTrue(expression.contains("logpilot.server.protocol"));
        assertTrue(expression.contains("all"));
    }
}