package com.logpilot.server.grpc;

import com.logpilot.core.model.LogEntry;
import com.logpilot.core.model.LogLevel;
import com.logpilot.core.service.LogService;
import com.logpilot.grpc.proto.LogPilotProto;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class LogPilotGrpcServiceTest {

    @Mock
    private LogService logService;

    @Mock
    private StreamObserver<LogPilotProto.LogResponse> logResponseObserver;

    @Mock
    private StreamObserver<LogPilotProto.SendLogsResponse> sendLogsResponseObserver;

    @Mock
    private StreamObserver<LogPilotProto.ListLogsResponse> listLogsResponseObserver;

    @Mock
    private StreamObserver<LogPilotProto.FetchLogsResponse> fetchLogsResponseObserver;

    private LogPilotGrpcService grpcService;
    private LogPilotProto.LogRequest testLogRequest;
    private List<LogEntry> testLogEntries;

    @BeforeEach
    void setUp() {
        grpcService = new LogPilotGrpcService(logService);

        testLogRequest = LogPilotProto.LogRequest.newBuilder()
                .setChannel("test-channel")
                .setLevel("INFO")
                .setMessage("Test message")
                .putMeta("userId", "123")
                .putMeta("sessionId", "session-abc")
                .build();

        testLogEntries = Arrays.asList(
            new LogEntry("channel1", LogLevel.INFO, "Message 1"),
            new LogEntry("channel2", LogLevel.ERROR, "Message 2")
        );
    }

    @Test
    void grpcService_ShouldHaveCorrectAnnotations() {
        Class<LogPilotGrpcService> serviceClass = LogPilotGrpcService.class;

        assertTrue(serviceClass.isAnnotationPresent(GrpcService.class));
        assertTrue(serviceClass.isAnnotationPresent(ConditionalOnExpression.class));

        ConditionalOnExpression conditional = serviceClass.getAnnotation(ConditionalOnExpression.class);
        String expectedExpression = "'${logpilot.server.protocol:all}' == 'grpc' or '${logpilot.server.protocol:all}' == 'all'";
        assertEquals(expectedExpression, conditional.value());
    }

    @Test
    void sendLog_WithValidRequest_ShouldReturnSuccessResponse() {
        grpcService.sendLog(testLogRequest, logResponseObserver);

        ArgumentCaptor<LogEntry> logEntryCaptor = ArgumentCaptor.forClass(LogEntry.class);
        verify(logService, times(1)).storeLog(logEntryCaptor.capture());

        LogEntry capturedEntry = logEntryCaptor.getValue();
        assertEquals("test-channel", capturedEntry.getChannel());
        assertEquals(LogLevel.INFO, capturedEntry.getLevel());
        assertEquals("Test message", capturedEntry.getMessage());
        assertNotNull(capturedEntry.getMeta());
        assertEquals("123", capturedEntry.getMeta().get("userId"));
        assertEquals("session-abc", capturedEntry.getMeta().get("sessionId"));

        ArgumentCaptor<LogPilotProto.LogResponse> responseCaptor = ArgumentCaptor.forClass(LogPilotProto.LogResponse.class);
        verify(logResponseObserver, times(1)).onNext(responseCaptor.capture());
        verify(logResponseObserver, times(1)).onCompleted();

        LogPilotProto.LogResponse response = responseCaptor.getValue();
        assertEquals("success", response.getStatus());
        assertEquals("Log stored successfully", response.getMessage());
    }

    @Test
    void sendLog_WithInvalidLogLevel_ShouldDefaultToInfo() {
        LogPilotProto.LogRequest invalidRequest = LogPilotProto.LogRequest.newBuilder()
                .setChannel("test-channel")
                .setLevel("INVALID_LEVEL")
                .setMessage("Test message")
                .build();

        grpcService.sendLog(invalidRequest, logResponseObserver);

        ArgumentCaptor<LogEntry> logEntryCaptor = ArgumentCaptor.forClass(LogEntry.class);
        verify(logService, times(1)).storeLog(logEntryCaptor.capture());

        LogEntry capturedEntry = logEntryCaptor.getValue();
        assertEquals(LogLevel.INFO, capturedEntry.getLevel());

        verify(logResponseObserver, times(1)).onNext(any(LogPilotProto.LogResponse.class));
        verify(logResponseObserver, times(1)).onCompleted();
    }

    @Test
    void sendLog_WithMetadata_ShouldConvertCorrectly() {
        LogPilotProto.LogRequest requestWithMeta = LogPilotProto.LogRequest.newBuilder()
                .setChannel("meta-channel")
                .setLevel("ERROR")
                .setMessage("Error with metadata")
                .putMeta("errorCode", "500")
                .putMeta("component", "payment")
                .putMeta("retryCount", "3")
                .build();

        grpcService.sendLog(requestWithMeta, logResponseObserver);

        ArgumentCaptor<LogEntry> logEntryCaptor = ArgumentCaptor.forClass(LogEntry.class);
        verify(logService, times(1)).storeLog(logEntryCaptor.capture());

        LogEntry capturedEntry = logEntryCaptor.getValue();
        Map<String, Object> meta = capturedEntry.getMeta();
        assertEquals("500", meta.get("errorCode"));
        assertEquals("payment", meta.get("component"));
        assertEquals("3", meta.get("retryCount"));

        verify(logResponseObserver, times(1)).onNext(any(LogPilotProto.LogResponse.class));
        verify(logResponseObserver, times(1)).onCompleted();
    }

    @Test
    void sendLog_WithEmptyMessage_ShouldHandleGracefully() {
        LogPilotProto.LogRequest emptyMessageRequest = LogPilotProto.LogRequest.newBuilder()
                .setChannel("empty-channel")
                .setLevel("WARN")
                .setMessage("")
                .build();

        grpcService.sendLog(emptyMessageRequest, logResponseObserver);

        ArgumentCaptor<LogEntry> logEntryCaptor = ArgumentCaptor.forClass(LogEntry.class);
        verify(logService, times(1)).storeLog(logEntryCaptor.capture());

        LogEntry capturedEntry = logEntryCaptor.getValue();
        assertEquals("", capturedEntry.getMessage());

        verify(logResponseObserver, times(1)).onNext(any(LogPilotProto.LogResponse.class));
        verify(logResponseObserver, times(1)).onCompleted();
    }

    @Test
    void sendLog_WhenServiceThrowsException_ShouldReturnErrorResponse() {
        doThrow(new RuntimeException("Storage error")).when(logService).storeLog(any(LogEntry.class));

        grpcService.sendLog(testLogRequest, logResponseObserver);

        ArgumentCaptor<LogPilotProto.LogResponse> responseCaptor = ArgumentCaptor.forClass(LogPilotProto.LogResponse.class);
        verify(logResponseObserver, times(1)).onNext(responseCaptor.capture());
        verify(logResponseObserver, times(1)).onCompleted();

        LogPilotProto.LogResponse response = responseCaptor.getValue();
        assertEquals("error", response.getStatus());
        assertTrue(response.getMessage().contains("Failed to store log"));
        assertTrue(response.getMessage().contains("Storage error"));
    }

    @Test
    void sendLogs_WithValidRequests_ShouldReturnSuccessResponse() {
        LogPilotProto.SendLogsRequest batchRequest = LogPilotProto.SendLogsRequest.newBuilder()
                .addLogRequests(testLogRequest)
                .addLogRequests(LogPilotProto.LogRequest.newBuilder()
                        .setChannel("batch-channel")
                        .setLevel("DEBUG")
                        .setMessage("Batch message")
                        .build())
                .build();

        grpcService.sendLogs(batchRequest, sendLogsResponseObserver);

        ArgumentCaptor<List<LogEntry>> logEntriesCaptor = ArgumentCaptor.forClass(List.class);
        verify(logService, times(1)).storeLogs(logEntriesCaptor.capture());

        List<LogEntry> capturedEntries = logEntriesCaptor.getValue();
        assertEquals(2, capturedEntries.size());
        assertEquals("test-channel", capturedEntries.get(0).getChannel());
        assertEquals("batch-channel", capturedEntries.get(1).getChannel());

        ArgumentCaptor<LogPilotProto.SendLogsResponse> responseCaptor = ArgumentCaptor.forClass(LogPilotProto.SendLogsResponse.class);
        verify(sendLogsResponseObserver, times(1)).onNext(responseCaptor.capture());
        verify(sendLogsResponseObserver, times(1)).onCompleted();

        LogPilotProto.SendLogsResponse response = responseCaptor.getValue();
        assertEquals("success", response.getStatus());
        assertEquals("Logs stored successfully", response.getMessage());
    }

    @Test
    void sendLogs_WithEmptyList_ShouldReturnSuccessResponse() {
        LogPilotProto.SendLogsRequest emptyRequest = LogPilotProto.SendLogsRequest.newBuilder().build();

        grpcService.sendLogs(emptyRequest, sendLogsResponseObserver);

        ArgumentCaptor<List<LogEntry>> logEntriesCaptor = ArgumentCaptor.forClass(List.class);
        verify(logService, times(1)).storeLogs(logEntriesCaptor.capture());

        List<LogEntry> capturedEntries = logEntriesCaptor.getValue();
        assertTrue(capturedEntries.isEmpty());

        verify(sendLogsResponseObserver, times(1)).onNext(any(LogPilotProto.SendLogsResponse.class));
        verify(sendLogsResponseObserver, times(1)).onCompleted();
    }

    @Test
    void sendLogs_WhenServiceThrowsException_ShouldReturnErrorResponse() {
        doThrow(new RuntimeException("Batch storage error")).when(logService).storeLogs(anyList());

        LogPilotProto.SendLogsRequest batchRequest = LogPilotProto.SendLogsRequest.newBuilder()
                .addLogRequests(testLogRequest)
                .build();

        grpcService.sendLogs(batchRequest, sendLogsResponseObserver);

        ArgumentCaptor<LogPilotProto.SendLogsResponse> responseCaptor = ArgumentCaptor.forClass(LogPilotProto.SendLogsResponse.class);
        verify(sendLogsResponseObserver, times(1)).onNext(responseCaptor.capture());
        verify(sendLogsResponseObserver, times(1)).onCompleted();

        LogPilotProto.SendLogsResponse response = responseCaptor.getValue();
        assertEquals("error", response.getStatus());
        assertTrue(response.getMessage().contains("Failed to store logs"));
        assertTrue(response.getMessage().contains("Batch storage error"));
    }

    @Test
    void listLogs_ShouldReturnProtoLogEntries() {
        when(logService.getAllLogs(100)).thenReturn(testLogEntries);

        LogPilotProto.ListLogsRequest listRequest = LogPilotProto.ListLogsRequest.newBuilder().build();

        grpcService.listLogs(listRequest, listLogsResponseObserver);

        verify(logService, times(1)).getAllLogs(100);

        ArgumentCaptor<LogPilotProto.ListLogsResponse> responseCaptor = ArgumentCaptor.forClass(LogPilotProto.ListLogsResponse.class);
        verify(listLogsResponseObserver, times(1)).onNext(responseCaptor.capture());
        verify(listLogsResponseObserver, times(1)).onCompleted();

        LogPilotProto.ListLogsResponse response = responseCaptor.getValue();
        assertEquals(2, response.getLogsCount());
        assertEquals("channel1", response.getLogs(0).getChannel());
        assertEquals("channel2", response.getLogs(1).getChannel());
    }

    @Test
    void listLogs_WithNoLogs_ShouldReturnEmptyResponse() {
        when(logService.getAllLogs(100)).thenReturn(Collections.emptyList());

        LogPilotProto.ListLogsRequest listRequest = LogPilotProto.ListLogsRequest.newBuilder().build();

        grpcService.listLogs(listRequest, listLogsResponseObserver);

        ArgumentCaptor<LogPilotProto.ListLogsResponse> responseCaptor = ArgumentCaptor.forClass(LogPilotProto.ListLogsResponse.class);
        verify(listLogsResponseObserver, times(1)).onNext(responseCaptor.capture());
        verify(listLogsResponseObserver, times(1)).onCompleted();

        LogPilotProto.ListLogsResponse response = responseCaptor.getValue();
        assertEquals(0, response.getLogsCount());
    }

    @Test
    void listLogs_WhenServiceThrowsException_ShouldCallOnError() {
        RuntimeException testException = new RuntimeException("List retrieval error");
        when(logService.getAllLogs(anyInt())).thenThrow(testException);

        LogPilotProto.ListLogsRequest listRequest = LogPilotProto.ListLogsRequest.newBuilder().build();

        grpcService.listLogs(listRequest, listLogsResponseObserver);

        verify(listLogsResponseObserver, times(1)).onError(testException);
        verify(listLogsResponseObserver, never()).onNext(any());
        verify(listLogsResponseObserver, never()).onCompleted();
    }

    @Test
    void fetchLogs_WithChannel_ShouldCallGetLogsForConsumer() {
        when(logService.getLogsForConsumer("fetch-channel", "consumer1", 50)).thenReturn(testLogEntries);

        LogPilotProto.FetchLogsRequest fetchRequest = LogPilotProto.FetchLogsRequest.newBuilder()
                .setChannel("fetch-channel")
                .setSince("consumer1")
                .setLimit(50)
                .build();

        grpcService.fetchLogs(fetchRequest, fetchLogsResponseObserver);

        verify(logService, times(1)).getLogsForConsumer("fetch-channel", "consumer1", 50);

        ArgumentCaptor<LogPilotProto.FetchLogsResponse> responseCaptor = ArgumentCaptor.forClass(LogPilotProto.FetchLogsResponse.class);
        verify(fetchLogsResponseObserver, times(1)).onNext(responseCaptor.capture());
        verify(fetchLogsResponseObserver, times(1)).onCompleted();

        LogPilotProto.FetchLogsResponse response = responseCaptor.getValue();
        assertEquals(2, response.getLogsCount());
    }

    @Test
    void fetchLogs_WithoutChannel_ShouldCallGetAllLogs() {
        when(logService.getAllLogs(75)).thenReturn(testLogEntries);

        LogPilotProto.FetchLogsRequest fetchRequest = LogPilotProto.FetchLogsRequest.newBuilder()
                .setChannel("")
                .setLimit(75)
                .build();

        grpcService.fetchLogs(fetchRequest, fetchLogsResponseObserver);

        verify(logService, times(1)).getAllLogs(75);
        verify(logService, never()).getLogsForConsumer(anyString(), anyString(), anyInt());

        verify(fetchLogsResponseObserver, times(1)).onNext(any(LogPilotProto.FetchLogsResponse.class));
        verify(fetchLogsResponseObserver, times(1)).onCompleted();
    }

    @Test
    void fetchLogs_WithEmptyChannel_ShouldCallGetAllLogs() {
        when(logService.getAllLogs(100)).thenReturn(testLogEntries);

        LogPilotProto.FetchLogsRequest fetchRequest = LogPilotProto.FetchLogsRequest.newBuilder()
                .setLimit(100)
                .build();

        grpcService.fetchLogs(fetchRequest, fetchLogsResponseObserver);

        verify(logService, times(1)).getAllLogs(100);
        verify(logService, never()).getLogsForConsumer(anyString(), anyString(), anyInt());
    }

    @Test
    void fetchLogs_WhenServiceThrowsException_ShouldCallOnError() {
        RuntimeException testException = new RuntimeException("Fetch retrieval error");
        when(logService.getAllLogs(anyInt())).thenThrow(testException);

        LogPilotProto.FetchLogsRequest fetchRequest = LogPilotProto.FetchLogsRequest.newBuilder()
                .setLimit(100)
                .build();

        grpcService.fetchLogs(fetchRequest, fetchLogsResponseObserver);

        verify(fetchLogsResponseObserver, times(1)).onError(testException);
        verify(fetchLogsResponseObserver, never()).onNext(any());
        verify(fetchLogsResponseObserver, never()).onCompleted();
    }

    @Test
    void convertLogRequestToLogEntry_ShouldMapAllFields() {
        LogPilotProto.LogRequest fullRequest = LogPilotProto.LogRequest.newBuilder()
                .setChannel("convert-channel")
                .setLevel("ERROR")
                .setMessage("Conversion test message")
                .putMeta("traceId", "trace-123")
                .putMeta("spanId", "span-456")
                .build();

        try {
            Method convertMethod = LogPilotGrpcService.class
                    .getDeclaredMethod("convertLogRequestToLogEntry", LogPilotProto.LogRequest.class);
            convertMethod.setAccessible(true);

            LogEntry result = (LogEntry) convertMethod.invoke(grpcService, fullRequest);

            assertEquals("convert-channel", result.getChannel());
            assertEquals(LogLevel.ERROR, result.getLevel());
            assertEquals("Conversion test message", result.getMessage());
            assertNotNull(result.getMeta());
            assertEquals("trace-123", result.getMeta().get("traceId"));
            assertEquals("span-456", result.getMeta().get("spanId"));
            assertNotNull(result.getTimestamp());
        } catch (Exception e) {
            fail("Should be able to call convertLogRequestToLogEntry method");
        }
    }

    @Test
    void convertToProtoLogEntry_ShouldMapAllFields() {
        LogEntry logEntry = LogEntry.builder()
                .channel("proto-channel")
                .level(LogLevel.WARN)
                .message("Proto conversion test")
                .meta(Map.of("requestId", "req-789", "userId", "user-123"))
                .build();

        try {
            Method convertMethod = LogPilotGrpcService.class
                    .getDeclaredMethod("convertToProtoLogEntry", LogEntry.class);
            convertMethod.setAccessible(true);

            LogPilotProto.LogEntry result = (LogPilotProto.LogEntry) convertMethod.invoke(grpcService, logEntry);

            assertEquals("proto-channel", result.getChannel());
            assertEquals("WARN", result.getLevel());
            assertEquals("Proto conversion test", result.getMessage());
            assertEquals("req-789", result.getMetaMap().get("requestId"));
            assertEquals("user-123", result.getMetaMap().get("userId"));
            assertTrue(result.getTimestamp() > 0);
        } catch (Exception e) {
            fail("Should be able to call convertToProtoLogEntry method");
        }
    }

    @Test
    void convertStringToLogLevel_WithValidLevel_ShouldConvert() {
        try {
            Method convertMethod = LogPilotGrpcService.class
                    .getDeclaredMethod("convertStringToLogLevel", String.class);
            convertMethod.setAccessible(true);

            LogLevel debugResult = (LogLevel) convertMethod.invoke(grpcService, "DEBUG");
            LogLevel infoResult = (LogLevel) convertMethod.invoke(grpcService, "info");
            LogLevel warnResult = (LogLevel) convertMethod.invoke(grpcService, "warn");
            LogLevel errorResult = (LogLevel) convertMethod.invoke(grpcService, "ERROR");

            assertEquals(LogLevel.DEBUG, debugResult);
            assertEquals(LogLevel.INFO, infoResult);
            assertEquals(LogLevel.WARN, warnResult);
            assertEquals(LogLevel.ERROR, errorResult);
        } catch (Exception e) {
            fail("Should be able to call convertStringToLogLevel method");
        }
    }

    @Test
    void convertStringToLogLevel_WithInvalidLevel_ShouldDefaultToInfo() {
        try {
            Method convertMethod = LogPilotGrpcService.class
                    .getDeclaredMethod("convertStringToLogLevel", String.class);
            convertMethod.setAccessible(true);

            LogLevel invalidResult = (LogLevel) convertMethod.invoke(grpcService, "INVALID_LEVEL");
            LogLevel nullResult = (LogLevel) convertMethod.invoke(grpcService, (String) null);

            assertEquals(LogLevel.INFO, invalidResult);
            assertEquals(LogLevel.INFO, nullResult);
        } catch (Exception e) {
            fail("Should be able to call convertStringToLogLevel method");
        }
    }
}