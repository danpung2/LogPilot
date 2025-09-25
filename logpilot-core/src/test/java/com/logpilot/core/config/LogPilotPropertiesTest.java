package com.logpilot.core.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class LogPilotPropertiesTest {

    private LogPilotProperties properties;

    @BeforeEach
    void setUp() {
        properties = new LogPilotProperties();
    }

    @Test
    void defaultValues_ShouldBeSet() {
        // Storage defaults
        assertNotNull(properties.getStorage());
        assertEquals(LogPilotProperties.StorageType.SQLITE, properties.getStorage().getType());
        assertEquals("./data/logs", properties.getStorage().getDirectory());

        // SQLite defaults
        assertNotNull(properties.getStorage().getSqlite());
        assertEquals("./data/logpilot.db", properties.getStorage().getSqlite().getPath());

        // Server defaults
        assertNotNull(properties.getServer());
        assertEquals(8080, properties.getServer().getPort());

        // gRPC defaults
        assertNotNull(properties.getGrpc());
        assertEquals(50051, properties.getGrpc().getPort());
    }

    @Test
    void setStorage_ShouldUpdateStorageConfiguration() {
        LogPilotProperties.Storage newStorage = new LogPilotProperties.Storage();
        newStorage.setType(LogPilotProperties.StorageType.FILE);
        newStorage.setDirectory("/custom/path");

        properties.setStorage(newStorage);

        assertEquals(LogPilotProperties.StorageType.FILE, properties.getStorage().getType());
        assertEquals("/custom/path", properties.getStorage().getDirectory());
    }

    @Test
    void setServer_ShouldUpdateServerConfiguration() {
        LogPilotProperties.Server newServer = new LogPilotProperties.Server();
        newServer.setPort(9090);

        properties.setServer(newServer);

        assertEquals(9090, properties.getServer().getPort());
    }

    @Test
    void setGrpc_ShouldUpdateGrpcConfiguration() {
        LogPilotProperties.Grpc newGrpc = new LogPilotProperties.Grpc();
        newGrpc.setPort(60051);

        properties.setGrpc(newGrpc);

        assertEquals(60051, properties.getGrpc().getPort());
    }

    @Test
    void storageClass_ShouldSupportAllOperations() {
        LogPilotProperties.Storage storage = new LogPilotProperties.Storage();

        // Test type
        storage.setType(LogPilotProperties.StorageType.FILE);
        assertEquals(LogPilotProperties.StorageType.FILE, storage.getType());

        // Test directory
        String customDirectory = "/custom/directory/path";
        storage.setDirectory(customDirectory);
        assertEquals(customDirectory, storage.getDirectory());

        // Test SQLite
        LogPilotProperties.Storage.Sqlite customSqlite = new LogPilotProperties.Storage.Sqlite();
        customSqlite.setPath("/custom/db.sqlite");
        storage.setSqlite(customSqlite);
        assertEquals("/custom/db.sqlite", storage.getSqlite().getPath());
    }

    @Test
    void sqliteClass_ShouldSupportPathConfiguration() {
        LogPilotProperties.Storage.Sqlite sqlite = new LogPilotProperties.Storage.Sqlite();

        String customPath = "/custom/database.db";
        sqlite.setPath(customPath);
        assertEquals(customPath, sqlite.getPath());
    }

    @Test
    void serverClass_ShouldSupportPortConfiguration() {
        LogPilotProperties.Server server = new LogPilotProperties.Server();

        server.setPort(3000);
        assertEquals(3000, server.getPort());

        server.setPort(65535);
        assertEquals(65535, server.getPort());

        server.setPort(1);
        assertEquals(1, server.getPort());
    }

    @Test
    void grpcClass_ShouldSupportPortConfiguration() {
        LogPilotProperties.Grpc grpc = new LogPilotProperties.Grpc();

        grpc.setPort(9000);
        assertEquals(9000, grpc.getPort());

        grpc.setPort(65535);
        assertEquals(65535, grpc.getPort());

        grpc.setPort(1);
        assertEquals(1, grpc.getPort());
    }

    @Test
    void storageType_ShouldContainAllExpectedValues() {
        LogPilotProperties.StorageType[] types = LogPilotProperties.StorageType.values();

        assertEquals(2, types.length);
        assertEquals(LogPilotProperties.StorageType.FILE, types[0]);
        assertEquals(LogPilotProperties.StorageType.SQLITE, types[1]);
    }

    @Test
    void storageType_ValueOf_ShouldWorkCorrectly() {
        assertEquals(LogPilotProperties.StorageType.FILE,
                    LogPilotProperties.StorageType.valueOf("FILE"));
        assertEquals(LogPilotProperties.StorageType.SQLITE,
                    LogPilotProperties.StorageType.valueOf("SQLITE"));
    }

    @Test
    void storageType_ValueOf_WithInvalidValue_ShouldThrowException() {
        assertThrows(IllegalArgumentException.class,
                    () -> LogPilotProperties.StorageType.valueOf("INVALID"));
        assertThrows(IllegalArgumentException.class,
                    () -> LogPilotProperties.StorageType.valueOf("file"));
        assertThrows(IllegalArgumentException.class,
                    () -> LogPilotProperties.StorageType.valueOf("sqlite"));
    }

    @Test
    void nestedClasses_ShouldHaveIndependentInstances() {
        LogPilotProperties properties1 = new LogPilotProperties();
        LogPilotProperties properties2 = new LogPilotProperties();

        properties1.getStorage().setType(LogPilotProperties.StorageType.FILE);
        properties1.getStorage().setDirectory("/path1");

        properties2.getStorage().setType(LogPilotProperties.StorageType.SQLITE);
        properties2.getStorage().setDirectory("/path2");

        // Changes to one instance should not affect another
        assertEquals(LogPilotProperties.StorageType.FILE, properties1.getStorage().getType());
        assertEquals("/path1", properties1.getStorage().getDirectory());

        assertEquals(LogPilotProperties.StorageType.SQLITE, properties2.getStorage().getType());
        assertEquals("/path2", properties2.getStorage().getDirectory());
    }

    @Test
    void chainedConfiguration_ShouldWork() {
        LogPilotProperties.Storage storage = new LogPilotProperties.Storage();
        LogPilotProperties.Storage.Sqlite sqlite = new LogPilotProperties.Storage.Sqlite();
        LogPilotProperties.Server server = new LogPilotProperties.Server();
        LogPilotProperties.Grpc grpc = new LogPilotProperties.Grpc();

        // Configure nested objects
        sqlite.setPath("/custom/database.sqlite");
        storage.setSqlite(sqlite);
        storage.setType(LogPilotProperties.StorageType.SQLITE);
        storage.setDirectory("/custom/logs");

        server.setPort(9080);
        grpc.setPort(59051);

        // Set on main properties
        properties.setStorage(storage);
        properties.setServer(server);
        properties.setGrpc(grpc);

        // Verify all configurations
        assertEquals(LogPilotProperties.StorageType.SQLITE, properties.getStorage().getType());
        assertEquals("/custom/logs", properties.getStorage().getDirectory());
        assertEquals("/custom/database.sqlite", properties.getStorage().getSqlite().getPath());
        assertEquals(9080, properties.getServer().getPort());
        assertEquals(59051, properties.getGrpc().getPort());
    }

    @Test
    void nullSafety_ShouldHandleNullAssignments() {
        // These should not throw exceptions
        assertDoesNotThrow(() -> {
            properties.setStorage(null);
            properties.setServer(null);
            properties.setGrpc(null);
        });

        // But the getters might return null now
        assertNull(properties.getStorage());
        assertNull(properties.getServer());
        assertNull(properties.getGrpc());
    }
}