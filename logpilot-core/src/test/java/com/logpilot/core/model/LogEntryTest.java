package com.logpilot.core.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class LogEntryTest {

    private LocalDateTime testTimestamp;

    @BeforeEach
    void setUp() {
        testTimestamp = LocalDateTime.of(2025, 9, 25, 10, 30, 0);
    }

    @Test
    void constructor_ShouldCreateLogEntryWithRequiredFields() {
        LogEntry entry = new LogEntry("channel", LogLevel.DEBUG, "message");

        assertEquals("channel", entry.getChannel());
        assertEquals(LogLevel.DEBUG, entry.getLevel());
        assertEquals("message", entry.getMessage());
        assertNotNull(entry.getTimestamp());
        assertNull(entry.getMeta());
    }

    @Test
    void constructor_WithMeta_ShouldCreateLogEntryWithAllFields() {
        Map<String, Object> meta = new HashMap<>();
        meta.put("userId", 123);
        meta.put("sessionId", "abc-123");

        LogEntry entry = new LogEntry("channel", LogLevel.ERROR, "message", meta);

        assertEquals("channel", entry.getChannel());
        assertEquals(LogLevel.ERROR, entry.getLevel());
        assertEquals("message", entry.getMessage());
        assertEquals(meta, entry.getMeta());
        assertNotNull(entry.getTimestamp());
    }

    @Test
    void defaultConstructor_ShouldCreateEmptyLogEntryWithTimestamp() {
        LogEntry entry = new LogEntry();

        assertNull(entry.getChannel());
        assertNull(entry.getLevel());
        assertNull(entry.getMessage());
        assertNull(entry.getMeta());
        assertNotNull(entry.getTimestamp());
    }

    @Test
    void settersAndGetters_ShouldWorkCorrectly() {
        LogEntry entry = new LogEntry();
        Map<String, Object> meta = new HashMap<>();
        meta.put("key", "value");

        entry.setChannel("test-channel");
        entry.setLevel(LogLevel.WARN);
        entry.setMessage("test message");
        entry.setMeta(meta);
        entry.setTimestamp(testTimestamp);

        assertEquals("test-channel", entry.getChannel());
        assertEquals(LogLevel.WARN, entry.getLevel());
        assertEquals("test message", entry.getMessage());
        assertEquals(meta, entry.getMeta());
        assertEquals(testTimestamp, entry.getTimestamp());
    }

    @Test
    void builder_ShouldCreateLogEntryCorrectly() {
        Map<String, Object> meta = new HashMap<>();
        meta.put("traceId", "trace-123");

        LogEntry entry = LogEntry.builder()
                .channel("builder-channel")
                .level(LogLevel.ERROR)
                .message("Builder test message")
                .meta(meta)
                .timestamp(testTimestamp)
                .build();

        assertEquals("builder-channel", entry.getChannel());
        assertEquals(LogLevel.ERROR, entry.getLevel());
        assertEquals("Builder test message", entry.getMessage());
        assertEquals(meta, entry.getMeta());
        assertEquals(testTimestamp, entry.getTimestamp());
    }

    @Test
    void builder_WithoutTimestamp_ShouldUseCurrentTime() {
        LogEntry entry = LogEntry.builder()
                .channel("test")
                .level(LogLevel.INFO)
                .message("test")
                .build();

        assertNotNull(entry.getTimestamp());
    }

    @Test
    void builder_WithoutOptionalFields_ShouldCreateMinimalEntry() {
        LogEntry entry = LogEntry.builder()
                .channel("minimal")
                .level(LogLevel.DEBUG)
                .message("minimal message")
                .build();

        assertEquals("minimal", entry.getChannel());
        assertEquals(LogLevel.DEBUG, entry.getLevel());
        assertEquals("minimal message", entry.getMessage());
        assertNull(entry.getMeta());
        assertNotNull(entry.getTimestamp());
    }

    @Test
    void equals_WithSameValues_ShouldReturnTrue() {
        LogEntry entry1 = new LogEntry("channel", LogLevel.INFO, "message");
        entry1.setTimestamp(testTimestamp);

        LogEntry entry2 = new LogEntry("channel", LogLevel.INFO, "message");
        entry2.setTimestamp(testTimestamp);

        assertEquals(entry1, entry2);
    }

    @Test
    void equals_WithDifferentValues_ShouldReturnFalse() {
        LogEntry entry1 = new LogEntry("channel1", LogLevel.INFO, "message");
        LogEntry entry2 = new LogEntry("channel2", LogLevel.INFO, "message");

        assertNotEquals(entry1, entry2);
    }

    @Test
    void equals_WithNull_ShouldReturnFalse() {
        LogEntry entry = new LogEntry("channel", LogLevel.INFO, "message");

        assertNotEquals(entry, null);
    }

    @Test
    void equals_WithSameReference_ShouldReturnTrue() {
        LogEntry entry = new LogEntry("channel", LogLevel.INFO, "message");

        assertEquals(entry, entry);
    }

    @Test
    void equals_WithDifferentClass_ShouldReturnFalse() {
        LogEntry entry = new LogEntry("channel", LogLevel.INFO, "message");
        String notLogEntry = "not a log entry";

        assertNotEquals(entry, notLogEntry);
    }

    @Test
    void hashCode_WithSameValues_ShouldBeEqual() {
        LogEntry entry1 = new LogEntry("channel", LogLevel.INFO, "message");
        entry1.setTimestamp(testTimestamp);

        LogEntry entry2 = new LogEntry("channel", LogLevel.INFO, "message");
        entry2.setTimestamp(testTimestamp);

        assertEquals(entry1.hashCode(), entry2.hashCode());
    }

    @Test
    void hashCode_WithDifferentValues_ShouldBeDifferent() {
        LogEntry entry1 = new LogEntry("channel1", LogLevel.INFO, "message");
        LogEntry entry2 = new LogEntry("channel2", LogLevel.INFO, "message");

        assertNotEquals(entry1.hashCode(), entry2.hashCode());
    }

    @Test
    void toString_ShouldContainAllFields() {
        Map<String, Object> meta = new HashMap<>();
        meta.put("key", "value");

        LogEntry entry = new LogEntry("test-channel", LogLevel.WARN, "test message", meta);
        entry.setTimestamp(testTimestamp);

        String result = entry.toString();

        assertTrue(result.contains("test-channel"));
        assertTrue(result.contains("WARN"));
        assertTrue(result.contains("test message"));
        assertTrue(result.contains("meta={key=value}"));
        assertTrue(result.contains(testTimestamp.toString()));
    }

    @Test
    void toString_WithNullMeta_ShouldHandleGracefully() {
        LogEntry entry = new LogEntry("channel", LogLevel.INFO, "message");
        entry.setTimestamp(testTimestamp);

        String result = entry.toString();

        assertTrue(result.contains("channel"));
        assertTrue(result.contains("INFO"));
        assertTrue(result.contains("message"));
        assertTrue(result.contains("meta=null"));
    }
}