package com.logpilot.core.config;

import com.logpilot.core.storage.FileLogStorage;
import com.logpilot.core.storage.LogStorage;
import com.logpilot.core.storage.SqliteLogStorage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

public class LogStorageFactoryTest {

    @TempDir
    Path tempDir;

    private LogPilotProperties properties;

    @BeforeEach
    void setUp() {
        properties = new LogPilotProperties();
    }

    @Test
    void createLogStorage_WithNullProperties_ShouldThrowException() {
        assertThrows(IllegalArgumentException.class, () -> LogStorageFactory.createLogStorage(null));
    }

    @Test
    void createLogStorage_WithSqliteType_ShouldReturnSqliteStorage() {
        properties.getStorage().setType(LogPilotProperties.StorageType.SQLITE);
        properties.getStorage().getSqlite().setPath(tempDir.resolve("test.db").toString());

        try (LogStorage storage = LogStorageFactory.createLogStorage(properties)) {
            assertNotNull(storage);
            assertInstanceOf(SqliteLogStorage.class, storage);
        }
    }

    @Test
    void createLogStorage_WithFileType_ShouldReturnFileStorage() {
        properties.getStorage().setType(LogPilotProperties.StorageType.FILE);
        properties.getStorage().setDirectory(tempDir.toString());

        try (LogStorage storage = LogStorageFactory.createLogStorage(properties)) {
            assertNotNull(storage);
            assertInstanceOf(FileLogStorage.class, storage);
        }
    }

    @Test
    void createLogStorage_WithSqliteType_ShouldCreateParentDirectories() {
        Path dbFile = tempDir.resolve("nested/deep/directories/test.db");
        properties.getStorage().setType(LogPilotProperties.StorageType.SQLITE);
        properties.getStorage().getSqlite().setPath(dbFile.toString());

        try (LogStorage storage = assertDoesNotThrow(() -> LogStorageFactory.createLogStorage(properties))) {
            assertNotNull(storage);
            assertInstanceOf(SqliteLogStorage.class, storage);

            assertTrue(dbFile.getParent().toFile().exists());
        }
    }

    @Test
    void createLogStorage_WithFileType_ShouldCreateDirectory() {
        Path storageDir = tempDir.resolve("new/storage/directory");
        properties.getStorage().setType(LogPilotProperties.StorageType.FILE);
        properties.getStorage().setDirectory(storageDir.toString());

        try (LogStorage storage = assertDoesNotThrow(() -> LogStorageFactory.createLogStorage(properties))) {
            assertNotNull(storage);
            assertInstanceOf(FileLogStorage.class, storage);

            assertTrue(storageDir.toFile().exists());
            assertTrue(storageDir.toFile().isDirectory());
        }
    }

    @Test
    void createLogStorage_ShouldInitializeStorage() {
        properties.getStorage().setType(LogPilotProperties.StorageType.FILE);
        properties.getStorage().setDirectory(tempDir.toString());

        try (LogStorage storage = LogStorageFactory.createLogStorage(properties)) {
            assertNotNull(storage);
            assertDoesNotThrow(() -> storage.retrieveAll(1));
        }
    }

    @Test
    void createLogStorage_WithExistingSqliteParentDirectory_ShouldNotFail() {
        Path dbFile = tempDir.resolve("existing/test.db");

        assertTrue(dbFile.getParent().toFile().mkdirs());
        assertTrue(dbFile.getParent().toFile().exists());

        properties.getStorage().setType(LogPilotProperties.StorageType.SQLITE);
        properties.getStorage().getSqlite().setPath(dbFile.toString());

        try (LogStorage storage = assertDoesNotThrow(() -> LogStorageFactory.createLogStorage(properties))) {
            assertNotNull(storage);
            assertInstanceOf(SqliteLogStorage.class, storage);
        }
    }

    @Test
    void createLogStorage_WithExistingFileDirectory_ShouldNotFail() {
        Path storageDir = tempDir.resolve("existing");

        assertTrue(storageDir.toFile().mkdirs());
        assertTrue(storageDir.toFile().exists());

        properties.getStorage().setType(LogPilotProperties.StorageType.FILE);
        properties.getStorage().setDirectory(storageDir.toString());

        try (LogStorage storage = assertDoesNotThrow(() -> LogStorageFactory.createLogStorage(properties))) {
            assertNotNull(storage);
            assertInstanceOf(FileLogStorage.class, storage);
        }
    }

    @Test
    void createLogStorage_WithDifferentConfigurations_ShouldRespectSettings() {
        LogPilotProperties sqliteProps = new LogPilotProperties();
        sqliteProps.getStorage().setType(LogPilotProperties.StorageType.SQLITE);
        sqliteProps.getStorage().getSqlite().setPath(tempDir.resolve("custom.sqlite").toString());

        try (LogStorage sqliteStorage = LogStorageFactory.createLogStorage(sqliteProps)) {
            assertInstanceOf(SqliteLogStorage.class, sqliteStorage);

            LogPilotProperties fileProps = new LogPilotProperties();
            fileProps.getStorage().setType(LogPilotProperties.StorageType.FILE);
            fileProps.getStorage().setDirectory(tempDir.resolve("custom-logs").toString());

            try (LogStorage fileStorage = LogStorageFactory.createLogStorage(fileProps)) {
                assertInstanceOf(FileLogStorage.class, fileStorage);

                assertNotSame(sqliteStorage, fileStorage);
            }
        }
    }

    @Test
    void createLogStorage_SqliteWithNullPath_ShouldUseDefaultPath() {
        properties.getStorage().setType(LogPilotProperties.StorageType.SQLITE);

        try (LogStorage storage = assertDoesNotThrow(() -> LogStorageFactory.createLogStorage(properties))) {
            assertNotNull(storage);
            assertInstanceOf(SqliteLogStorage.class, storage);
        }
    }

    @Test
    void createLogStorage_FileWithNullDirectory_ShouldUseDefaultDirectory() {
        properties.getStorage().setType(LogPilotProperties.StorageType.FILE);

        try (LogStorage storage = assertDoesNotThrow(() -> LogStorageFactory.createLogStorage(properties))) {
            assertNotNull(storage);
            assertInstanceOf(FileLogStorage.class, storage);
        }
    }

    @Test
    void createLogStorage_SqliteWithFileInRootDirectory_ShouldWork() {
        properties.getStorage().setType(LogPilotProperties.StorageType.SQLITE);
        properties.getStorage().getSqlite().setPath(tempDir.resolve("root.db").toString());

        try (LogStorage storage = assertDoesNotThrow(() -> LogStorageFactory.createLogStorage(properties))) {
            assertNotNull(storage);
            assertInstanceOf(SqliteLogStorage.class, storage);
        }
    }
}