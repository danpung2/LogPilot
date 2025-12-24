package com.logpilot.server.config;

import com.logpilot.core.config.LogPilotProperties;
import com.logpilot.core.storage.FileLogStorage;
import com.logpilot.core.storage.LogStorage;
import com.logpilot.core.storage.SqliteLogStorage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

public class ServerConfigTest {

    @TempDir
    Path tempDir;

    private ServerConfig serverConfig;
    private LogPilotProperties properties;

    @BeforeEach
    void setUp() {
        serverConfig = new ServerConfig();
        properties = new LogPilotProperties();
    }

    @Test
    void serverConfig_ShouldHaveCorrectAnnotations() {
        Class<ServerConfig> configClass = ServerConfig.class;

        assertTrue(configClass.isAnnotationPresent(Configuration.class));
        assertTrue(configClass.isAnnotationPresent(EnableConfigurationProperties.class));

        EnableConfigurationProperties enableProps = configClass.getAnnotation(EnableConfigurationProperties.class);

        Class<?>[] propertiesClasses = enableProps.value();
        assertEquals(1, propertiesClasses.length);
        assertEquals(LogPilotProperties.class, propertiesClasses[0]);
    }

    @Test
    void logStorage_WithSqliteProperties_ShouldReturnSqliteLogStorage() {
        properties.getStorage().setType(LogPilotProperties.StorageType.SQLITE);
        properties.getStorage().getSqlite().setPath(tempDir.resolve("test.db").toString());

        try (LogStorage storage = serverConfig.logStorage(properties)) {
            assertNotNull(storage);
            assertInstanceOf(SqliteLogStorage.class, storage);
        }
    }

    @Test
    void logStorage_WithFileProperties_ShouldReturnFileLogStorage() {
        properties.getStorage().setType(LogPilotProperties.StorageType.FILE);
        properties.getStorage().setDirectory(tempDir.toString());

        try (LogStorage storage = serverConfig.logStorage(properties)) {
            assertNotNull(storage);
            assertInstanceOf(FileLogStorage.class, storage);
        }
    }

    @Test
    void logStorage_WithNullProperties_ShouldThrowException() {
        assertThrows(IllegalArgumentException.class, () -> {
            serverConfig.logStorage(null);
        });
    }

    @Test
    void logStorage_WithDefaultProperties_ShouldReturnValidStorage() {
        try (LogStorage storage = serverConfig.logStorage(properties)) {
            assertNotNull(storage);
            assertInstanceOf(SqliteLogStorage.class, storage);
        }
    }

    @Test
    void logStorageBean_ShouldHaveCorrectAnnotations() throws NoSuchMethodException {
        java.lang.reflect.Method logStorageMethod = ServerConfig.class.getMethod("logStorage",
                LogPilotProperties.class);

        assertTrue(logStorageMethod.isAnnotationPresent(Bean.class));

        Bean beanAnnotation = logStorageMethod.getAnnotation(Bean.class);
        assertEquals("close", beanAnnotation.destroyMethod());

        assertNotNull(logStorageMethod);
    }

    @Test
    void logStorage_ShouldCreateFunctionalStorage() {
        properties.getStorage().setType(LogPilotProperties.StorageType.FILE);
        properties.getStorage().setDirectory(tempDir.toString());

        try (LogStorage storage = serverConfig.logStorage(properties)) {
            assertNotNull(storage);

            assertDoesNotThrow(() -> {
                // Test basic storage functionality
                storage.retrieve("test-channel", "consumer1", 1);
            });
        }
    }

    @Test
    void logStorage_WithDifferentConfigurations_ShouldCreateDifferentInstances() {
        LogPilotProperties sqliteProps = new LogPilotProperties();
        sqliteProps.getStorage().setType(LogPilotProperties.StorageType.SQLITE);
        sqliteProps.getStorage().getSqlite().setPath(tempDir.resolve("sqlite.db").toString());

        LogPilotProperties fileProps = new LogPilotProperties();
        fileProps.getStorage().setType(LogPilotProperties.StorageType.FILE);
        fileProps.getStorage().setDirectory(tempDir.resolve("file-storage").toString());

        try (LogStorage sqliteStorage = serverConfig.logStorage(sqliteProps);
                LogStorage fileStorage = serverConfig.logStorage(fileProps)) {

            assertNotNull(sqliteStorage);
            assertNotNull(fileStorage);
            assertNotSame(sqliteStorage, fileStorage);
            assertInstanceOf(SqliteLogStorage.class, sqliteStorage);
            assertInstanceOf(FileLogStorage.class, fileStorage);
        }
    }

    @Test
    void logStorage_ShouldCreateNewInstanceEachTime() {
        properties.getStorage().setType(LogPilotProperties.StorageType.FILE);
        properties.getStorage().setDirectory(tempDir.toString());

        try (LogStorage storage1 = serverConfig.logStorage(properties);
                LogStorage storage2 = serverConfig.logStorage(properties)) {

            assertNotNull(storage1);
            assertNotNull(storage2);
            assertNotSame(storage1, storage2);
        }
    }
}