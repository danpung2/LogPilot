package com.logpilot.core.config;

import com.logpilot.core.storage.FileLogStorage;
import com.logpilot.core.storage.LogStorage;
import com.logpilot.core.storage.SqliteLogStorage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

public class LogStorageFactory {

    private static final Logger logger = LoggerFactory.getLogger(LogStorageFactory.class);

    @SuppressWarnings("resource")
    public static LogStorage createLogStorage(LogPilotProperties properties) {
        if (properties == null) {
            throw new IllegalArgumentException("LogPilotProperties cannot be null");
        }

        LogStorage storage = switch (properties.getStorage().getType()) {
            case SQLITE -> createSqliteStorage(properties);
            case FILE -> createFileStorage(properties);
        };
        storage.initialize();
        logger.info("Created and initialized {} storage", properties.getStorage().getType());

        return storage;
    }

    private static LogStorage createSqliteStorage(LogPilotProperties properties) {
        String dbPath = properties.getStorage().getSqlite().getPath();
        ensureParentDirectoryExists(dbPath);

        logger.debug("Creating SQLite storage at: {}", dbPath);
        return new SqliteLogStorage(properties.getStorage().getSqlite());
    }

    private static LogStorage createFileStorage(LogPilotProperties properties) {
        String directory = properties.getStorage().getDirectory();
        ensureDirectoryExists(directory);

        logger.debug("Creating file storage in directory: {}", directory);
        return new FileLogStorage(directory);
    }

    private static void ensureDirectoryExists(String directoryPath) {
        File directory = new File(directoryPath);
        if (!directory.exists()) {
            boolean created = directory.mkdirs();
            if (!created) {
                throw new RuntimeException("Failed to create directory: " + directory.getAbsolutePath());
            }
            logger.debug("Created directory: {}", directory.getAbsolutePath());
        }
    }

    private static void ensureParentDirectoryExists(String filePath) {
        File file = new File(filePath);
        File parentDir = file.getParentFile();

        if (parentDir != null && !parentDir.exists()) {
            boolean created = parentDir.mkdirs();
            if (!created) {
                throw new RuntimeException("Failed to create directory: " + parentDir.getAbsolutePath());
            }
            logger.debug("Created parent directory: {}", parentDir.getAbsolutePath());
        }
    }
}