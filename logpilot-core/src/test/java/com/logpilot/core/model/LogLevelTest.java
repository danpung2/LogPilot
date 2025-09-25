package com.logpilot.core.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class LogLevelTest {

    @Test
    void enum_ShouldContainAllExpectedValues() {
        LogLevel[] levels = LogLevel.values();

        assertEquals(4, levels.length);
        assertEquals(LogLevel.DEBUG, levels[0]);
        assertEquals(LogLevel.INFO, levels[1]);
        assertEquals(LogLevel.WARN, levels[2]);
        assertEquals(LogLevel.ERROR, levels[3]);
    }

    @Test
    void valueOf_WithValidValues_ShouldReturnCorrectEnum() {
        assertEquals(LogLevel.DEBUG, LogLevel.valueOf("DEBUG"));
        assertEquals(LogLevel.INFO, LogLevel.valueOf("INFO"));
        assertEquals(LogLevel.WARN, LogLevel.valueOf("WARN"));
        assertEquals(LogLevel.ERROR, LogLevel.valueOf("ERROR"));
    }

    @Test
    void valueOf_WithInvalidValue_ShouldThrowException() {
        assertThrows(IllegalArgumentException.class, () -> LogLevel.valueOf("INVALID"));
        assertThrows(IllegalArgumentException.class, () -> LogLevel.valueOf("debug"));
        assertThrows(IllegalArgumentException.class, () -> LogLevel.valueOf("info"));
    }

    @Test
    void valueOf_WithNull_ShouldThrowException() {
        assertThrows(NullPointerException.class, () -> LogLevel.valueOf(null));
    }

    @Test
    void name_ShouldReturnCorrectStrings() {
        assertEquals("DEBUG", LogLevel.DEBUG.name());
        assertEquals("INFO", LogLevel.INFO.name());
        assertEquals("WARN", LogLevel.WARN.name());
        assertEquals("ERROR", LogLevel.ERROR.name());
    }

    @Test
    void toString_ShouldReturnCorrectStrings() {
        assertEquals("DEBUG", LogLevel.DEBUG.toString());
        assertEquals("INFO", LogLevel.INFO.toString());
        assertEquals("WARN", LogLevel.WARN.toString());
        assertEquals("ERROR", LogLevel.ERROR.toString());
    }

    @Test
    void ordinal_ShouldReturnCorrectOrder() {
        assertEquals(0, LogLevel.DEBUG.ordinal());
        assertEquals(1, LogLevel.INFO.ordinal());
        assertEquals(2, LogLevel.WARN.ordinal());
        assertEquals(3, LogLevel.ERROR.ordinal());
    }

    @Test
    void compareTo_ShouldOrderCorrectly() {
        assertTrue(LogLevel.DEBUG.compareTo(LogLevel.INFO) < 0);
        assertTrue(LogLevel.INFO.compareTo(LogLevel.WARN) < 0);
        assertTrue(LogLevel.WARN.compareTo(LogLevel.ERROR) < 0);

        assertTrue(LogLevel.ERROR.compareTo(LogLevel.WARN) > 0);
        assertTrue(LogLevel.WARN.compareTo(LogLevel.INFO) > 0);
        assertTrue(LogLevel.INFO.compareTo(LogLevel.DEBUG) > 0);

        assertEquals(0, LogLevel.INFO.compareTo(LogLevel.INFO));
    }

    @Test
    void equals_ShouldWorkCorrectly() {
        assertEquals(LogLevel.DEBUG, LogLevel.DEBUG);
        assertEquals(LogLevel.INFO, LogLevel.INFO);
        assertEquals(LogLevel.WARN, LogLevel.WARN);
        assertEquals(LogLevel.ERROR, LogLevel.ERROR);

        assertNotEquals(LogLevel.DEBUG, LogLevel.INFO);
        assertNotEquals(LogLevel.INFO, LogLevel.WARN);
        assertNotEquals(LogLevel.WARN, LogLevel.ERROR);
    }

    @Test
    void hashCode_ShouldBeConsistent() {
        assertEquals(LogLevel.DEBUG.hashCode(), LogLevel.DEBUG.hashCode());
        assertEquals(LogLevel.INFO.hashCode(), LogLevel.INFO.hashCode());
        assertEquals(LogLevel.WARN.hashCode(), LogLevel.WARN.hashCode());
        assertEquals(LogLevel.ERROR.hashCode(), LogLevel.ERROR.hashCode());

        // Hash codes should be different for different enum values
        assertNotEquals(LogLevel.DEBUG.hashCode(), LogLevel.INFO.hashCode());
        assertNotEquals(LogLevel.INFO.hashCode(), LogLevel.WARN.hashCode());
        assertNotEquals(LogLevel.WARN.hashCode(), LogLevel.ERROR.hashCode());
    }
}