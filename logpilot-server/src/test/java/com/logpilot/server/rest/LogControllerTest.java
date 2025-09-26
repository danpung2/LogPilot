package com.logpilot.server.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.logpilot.core.model.LogEntry;
import com.logpilot.core.model.LogLevel;
import com.logpilot.core.service.LogService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(LogController.class)
@ActiveProfiles("test")
public class LogControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean(name = "restLogService")
    private LogService logService;

    @Autowired
    private ObjectMapper objectMapper;

    private LogEntry testLogEntry;
    private List<LogEntry> testLogEntries;

    @BeforeEach
    void setUp() {
        testLogEntry = new LogEntry("test-channel", LogLevel.INFO, "Test message");

        testLogEntries = Arrays.asList(
            new LogEntry("channel1", LogLevel.INFO, "Message 1"),
            new LogEntry("channel2", LogLevel.ERROR, "Message 2")
        );
    }

    @Test
    void logController_ShouldHaveCorrectAnnotations() {
        Class<LogController> controllerClass = LogController.class;

        assertTrue(controllerClass.isAnnotationPresent(RestController.class));
        assertTrue(controllerClass.isAnnotationPresent(RequestMapping.class));
        assertTrue(controllerClass.isAnnotationPresent(ConditionalOnExpression.class));

        RequestMapping requestMapping = controllerClass.getAnnotation(RequestMapping.class);
        String[] mappingValues = requestMapping.value();
        assertEquals(1, mappingValues.length);
        assertEquals("/api", mappingValues[0]);

        ConditionalOnExpression conditional = controllerClass.getAnnotation(ConditionalOnExpression.class);
        String expectedExpression = "'${logpilot.server.protocol:all}' == 'rest' or '${logpilot.server.protocol:all}' == 'all'";
        assertEquals(expectedExpression, conditional.value());
    }

    @Test
    void storeLog_WithValidLogEntry_ShouldReturnCreated() throws Exception {
        String jsonContent = objectMapper.writeValueAsString(testLogEntry);

        mockMvc.perform(post("/api/logs")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonContent))
                .andExpect(status().isCreated());

        verify(logService, times(1)).storeLog(any(LogEntry.class));
    }

    @Test
    void storeLog_WithInvalidJson_ShouldReturnBadRequest() throws Exception {
        String invalidJson = "{\"channel\":\"test\",\"level\":\"INVALID_LEVEL\",\"message\":\"test\"}";

        mockMvc.perform(post("/api/logs")
                .contentType(MediaType.APPLICATION_JSON)
                .content(invalidJson))
                .andExpect(status().isBadRequest());

        verify(logService, never()).storeLog(any(LogEntry.class));
    }

    @Test
    void storeLog_WithEmptyBody_ShouldReturnBadRequest() throws Exception {
        mockMvc.perform(post("/api/logs")
                .contentType(MediaType.APPLICATION_JSON)
                .content(""))
                .andExpect(status().isBadRequest());

        verify(logService, never()).storeLog(any(LogEntry.class));
    }

    @Test
    void storeLogs_WithValidLogEntries_ShouldReturnCreated() throws Exception {
        String jsonContent = objectMapper.writeValueAsString(testLogEntries);

        mockMvc.perform(post("/api/logs/batch")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonContent))
                .andExpect(status().isCreated());

        verify(logService, times(1)).storeLogs(anyList());
    }

    @Test
    void storeLogs_WithEmptyList_ShouldReturnCreated() throws Exception {
        List<LogEntry> emptyList = Collections.emptyList();
        String jsonContent = objectMapper.writeValueAsString(emptyList);

        mockMvc.perform(post("/api/logs/batch")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonContent))
                .andExpect(status().isCreated());

        verify(logService, times(1)).storeLogs(eq(emptyList));
    }

    @Test
    void getLogs_WithChannelAndConsumerId_ShouldReturnLogs() throws Exception {
        when(logService.getLogsForConsumer("test-channel", "consumer1", 100))
            .thenReturn(testLogEntries);

        mockMvc.perform(get("/api/logs/test-channel")
                .param("consumerId", "consumer1")
                .param("limit", "100"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(2));

        verify(logService, times(1)).getLogsForConsumer("test-channel", "consumer1", 100);
        verify(logService, never()).getAllLogs(anyInt());
    }

    @Test
    void getLogs_WithChannelOnly_ShouldReturnAllLogs() throws Exception {
        when(logService.getAllLogs(100)).thenReturn(testLogEntries);

        mockMvc.perform(get("/api/logs/test-channel")
                .param("limit", "100"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(2));

        verify(logService, times(1)).getAllLogs(100);
        verify(logService, never()).getLogsForConsumer(anyString(), anyString(), anyInt());
    }

    @Test
    void getLogs_WithDefaultLimit_ShouldUseDefaultValue() throws Exception {
        when(logService.getAllLogs(100)).thenReturn(testLogEntries);

        mockMvc.perform(get("/api/logs/test-channel"))
                .andExpect(status().isOk());

        verify(logService, times(1)).getAllLogs(100);
    }

    @Test
    void getLogs_WithCustomLimit_ShouldUseCustomValue() throws Exception {
        when(logService.getAllLogs(50)).thenReturn(testLogEntries);

        mockMvc.perform(get("/api/logs/test-channel")
                .param("limit", "50"))
                .andExpect(status().isOk());

        verify(logService, times(1)).getAllLogs(50);
    }

    @Test
    void getAllLogs_ShouldReturnAllLogs() throws Exception {
        when(logService.getAllLogs(100)).thenReturn(testLogEntries);

        mockMvc.perform(get("/api/logs"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(2));

        verify(logService, times(1)).getAllLogs(100);
    }

    @Test
    void getAllLogs_WithCustomLimit_ShouldUseCustomValue() throws Exception {
        when(logService.getAllLogs(25)).thenReturn(testLogEntries);

        mockMvc.perform(get("/api/logs")
                .param("limit", "25"))
                .andExpect(status().isOk());

        verify(logService, times(1)).getAllLogs(25);
    }

    @Test
    void getAllLogs_WithDefaultLimit_ShouldUseDefaultValue() throws Exception {
        when(logService.getAllLogs(100)).thenReturn(testLogEntries);

        mockMvc.perform(get("/api/logs"))
                .andExpect(status().isOk());

        verify(logService, times(1)).getAllLogs(100);
    }

    @Test
    void storeLog_WhenServiceThrowsException_ShouldReturnInternalServerError() throws Exception {
        doThrow(new RuntimeException("Storage error")).when(logService).storeLog(any(LogEntry.class));

        String jsonContent = objectMapper.writeValueAsString(testLogEntry);

        mockMvc.perform(post("/api/logs")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonContent))
                .andExpect(status().isInternalServerError());

        verify(logService, times(1)).storeLog(any(LogEntry.class));
    }

    @Test
    void storeLogs_WhenServiceThrowsException_ShouldReturnInternalServerError() throws Exception {
        doThrow(new RuntimeException("Storage error")).when(logService).storeLogs(anyList());

        String jsonContent = objectMapper.writeValueAsString(testLogEntries);

        mockMvc.perform(post("/api/logs/batch")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonContent))
                .andExpect(status().isInternalServerError());

        verify(logService, times(1)).storeLogs(anyList());
    }

    @Test
    void getLogs_WhenServiceThrowsException_ShouldReturnInternalServerError() throws Exception {
        when(logService.getAllLogs(anyInt())).thenThrow(new RuntimeException("Retrieval error"));

        mockMvc.perform(get("/api/logs/test-channel"))
                .andExpect(status().isInternalServerError());

        verify(logService, times(1)).getAllLogs(100);
    }

    @Test
    void storeLog_WithComplexLogEntry_ShouldHandleCorrectly() throws Exception {
        Map<String, Object> meta = new HashMap<>();
        meta.put("userId", 123);
        meta.put("sessionId", "session-abc");
        meta.put("tags", Arrays.asList("urgent", "customer"));

        LogEntry complexLogEntry = new LogEntry("complex-channel", LogLevel.WARN, "Complex message", meta);
        complexLogEntry.setTimestamp(LocalDateTime.now());

        String jsonContent = objectMapper.writeValueAsString(complexLogEntry);

        mockMvc.perform(post("/api/logs")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonContent))
                .andExpect(status().isCreated());

        verify(logService, times(1)).storeLog(any(LogEntry.class));
    }

    @Test
    void getLogs_WithSpecialCharactersInChannel_ShouldHandleCorrectly() throws Exception {
        when(logService.getAllLogs(100)).thenReturn(testLogEntries);

        mockMvc.perform(get("/api/logs/special-channel!@#$%")
                .param("limit", "100"))
                .andExpect(status().isOk());

        verify(logService, times(1)).getAllLogs(100);
    }

    @Test
    void getLogs_WithLargeLimit_ShouldAcceptValue() throws Exception {
        when(logService.getAllLogs(10000)).thenReturn(testLogEntries);

        mockMvc.perform(get("/api/logs")
                .param("limit", "10000"))
                .andExpect(status().isOk());

        verify(logService, times(1)).getAllLogs(10000);
    }

    @Test
    void getLogs_WithZeroLimit_ShouldAcceptValue() throws Exception {
        when(logService.getAllLogs(0)).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/logs")
                .param("limit", "0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));

        verify(logService, times(1)).getAllLogs(0);
    }

    @Test
    void storeLog_WithMissingRequiredFields_ShouldReturnBadRequest() throws Exception {
        String incompleteJson = "{\"channel\":\"test\"}";

        mockMvc.perform(post("/api/logs")
                .contentType(MediaType.APPLICATION_JSON)
                .content(incompleteJson))
                .andExpect(status().isBadRequest());

        verify(logService, never()).storeLog(any(LogEntry.class));
    }
}