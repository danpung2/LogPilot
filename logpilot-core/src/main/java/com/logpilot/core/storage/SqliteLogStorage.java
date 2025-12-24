package com.logpilot.core.storage;

import com.logpilot.core.config.LogPilotProperties;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.logpilot.core.model.LogEntry;
import com.logpilot.core.model.LogLevel;
import com.logpilot.core.exception.StorageException;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class SqliteLogStorage implements LogStorage {

    private static final Logger logger = LoggerFactory.getLogger(SqliteLogStorage.class);
    private final LogPilotProperties.Storage.Sqlite config;
    private final ObjectMapper objectMapper;
    private HikariDataSource dataSource;

    public SqliteLogStorage(LogPilotProperties.Storage.Sqlite config) {
        this.config = config;
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
        initialize();
    }

    @Override
    public void initialize() {
        try {
            HikariConfig hikariConfig = new HikariConfig();
            hikariConfig.setJdbcUrl("jdbc:sqlite:" + config.getPath());
            hikariConfig.setDriverClassName("org.sqlite.JDBC");

            LogPilotProperties.Storage.Pooling pooling = config.getPooling();
            hikariConfig.setMaximumPoolSize(pooling.getMaximumPoolSize());
            hikariConfig.setMinimumIdle(pooling.getMinimumIdle());
            hikariConfig.setConnectionTimeout(pooling.getConnectionTimeout());
            hikariConfig.setIdleTimeout(pooling.getIdleTimeout());

            hikariConfig.setPoolName("LogPilotSQLitePool");
            // 성능 향상을 위해 WAL 모드와 동기화 설정을 최적화합니다.
            // Optimize WAL mode and synchronization settings for performance.
            hikariConfig.setConnectionInitSql("PRAGMA journal_mode=WAL; PRAGMA synchronous=NORMAL;");

            this.dataSource = new HikariDataSource(hikariConfig);

            try (Connection conn = dataSource.getConnection()) {
                createTablesIfNotExists(conn);
            }

            logger.info("SQLite storage initialized at: {} with WAL mode enabled", config.getPath());
        } catch (SQLException e) {
            throw new StorageException("Failed to initialize SQLite storage", e);
        }
    }

    private void createTablesIfNotExists(Connection conn) throws SQLException {
        String createLogsTable = """
                CREATE TABLE IF NOT EXISTS logs (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    channel TEXT NOT NULL,
                    level TEXT NOT NULL,
                    message TEXT NOT NULL,
                    meta TEXT,
                    timestamp DATETIME NOT NULL,
                    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
                )
                """;

        String createConsumerOffsetsTable = """
                CREATE TABLE IF NOT EXISTS consumer_offsets (
                    consumer_id TEXT NOT NULL,
                    channel TEXT NOT NULL,
                    last_log_id INTEGER NOT NULL,
                    PRIMARY KEY (consumer_id, channel)
                )
                """;

        try (Statement stmt = conn.createStatement()) {
            stmt.execute(createLogsTable);
            stmt.execute(createConsumerOffsetsTable);
        }
    }

    @Override
    public void store(LogEntry logEntry) {
        String sql = "INSERT INTO logs (channel, level, message, meta, timestamp) VALUES (?, ?, ?, ?, ?)";

        try (Connection conn = dataSource.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, logEntry.getChannel());
            stmt.setString(2, logEntry.getLevel().name());
            stmt.setString(3, logEntry.getMessage());

            if (logEntry.getMeta() != null && !logEntry.getMeta().isEmpty()) {
                stmt.setString(4, objectMapper.writeValueAsString(logEntry.getMeta()));
            } else {
                stmt.setNull(4, Types.VARCHAR);
            }

            stmt.setTimestamp(5, Timestamp.valueOf(logEntry.getTimestamp()));

            stmt.executeUpdate();
            logger.debug("Stored log entry for channel: {}", logEntry.getChannel());
        } catch (SQLException | JsonProcessingException e) {
            logger.error("Failed to store log entry", e);
            throw new StorageException("Failed to store log entry", e);
        }
    }

    @Override
    public void storeLogs(List<LogEntry> logEntries) {
        if (logEntries == null || logEntries.isEmpty()) {
            return;
        }

        String sql = "INSERT INTO logs (channel, level, message, meta, timestamp) VALUES (?, ?, ?, ?, ?)";

        try (Connection conn = dataSource.getConnection()) {
            conn.setAutoCommit(false);

            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                for (LogEntry logEntry : logEntries) {
                    stmt.setString(1, logEntry.getChannel());
                    stmt.setString(2, logEntry.getLevel().name());
                    stmt.setString(3, logEntry.getMessage());

                    if (logEntry.getMeta() != null && !logEntry.getMeta().isEmpty()) {
                        stmt.setString(4, objectMapper.writeValueAsString(logEntry.getMeta()));
                    } else {
                        stmt.setNull(4, Types.VARCHAR);
                    }

                    stmt.setTimestamp(5, Timestamp.valueOf(logEntry.getTimestamp()));
                    stmt.addBatch();
                }

                stmt.executeBatch();
                conn.commit();
                logger.debug("Stored {} log entries in batch", logEntries.size());
            } catch (SQLException | JsonProcessingException e) {
                try {
                    conn.rollback();
                    logger.error("Transaction rolled back due to error in batch insert", e);
                } catch (SQLException rollbackException) {
                    logger.error("Failed to rollback transaction", rollbackException);
                }
                throw new StorageException("Failed to store log entries in batch", e);
            } finally {
                try {
                    conn.setAutoCommit(true);
                } catch (SQLException e) {
                    logger.error("Failed to reset auto-commit", e);
                }
            }
        } catch (SQLException e) {
            throw new StorageException("Failed to obtain connection for batch insert", e);
        }
    }

    @Override
    public List<LogEntry> retrieve(String channel, String consumerId, int limit) {
        return retrieve(channel, consumerId, limit, true);
    }

    @Override
    public List<LogEntry> retrieve(String channel, String consumerId, int limit, boolean autoCommit) {
        long lastLogId = getConsumerOffset(consumerId, channel);

        String sql = "SELECT id, channel, level, message, meta, timestamp FROM logs " +
                "WHERE channel = ? AND id > ? ORDER BY id ASC LIMIT ?";

        List<LogEntry> entries = new ArrayList<>();
        long maxLogId = lastLogId;

        try (Connection conn = dataSource.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, channel);
            pstmt.setLong(2, lastLogId);
            pstmt.setInt(3, limit);

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    LogEntry entry = mapResultSetToLogEntry(rs);
                    entries.add(entry);
                    maxLogId = rs.getLong("id");
                }
            }

            if (autoCommit && maxLogId > lastLogId) {
                updateConsumerOffset(consumerId, channel, maxLogId);
            }

            logger.debug("Retrieved {} log entries for channel: {} and consumer: {} (autoCommit={})",
                    entries.size(), channel, consumerId, autoCommit);
        } catch (SQLException e) {
            logger.error("Failed to retrieve log entries", e);
            throw new StorageException("Failed to retrieve log entries", e);
        }

        return entries;
    }

    @Override
    public List<LogEntry> retrieve(String channel, int limit) {
        String sql = "SELECT id, channel, level, message, meta, timestamp FROM logs " +
                "WHERE channel = ? ORDER BY id DESC LIMIT ?";

        List<LogEntry> entries = new ArrayList<>();

        try (Connection conn = dataSource.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, channel);
            pstmt.setInt(2, limit);

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    entries.add(mapResultSetToLogEntry(rs));
                }
            }

            logger.debug("Retrieved {} log entries for channel: {} (limit={})", entries.size(), channel, limit);
        } catch (SQLException e) {
            logger.error("Failed to retrieve log entries for channel: " + channel, e);
            throw new StorageException("Failed to retrieve log entries for channel: " + channel, e);
        }

        return entries;
    }

    @Override
    public void commitOffset(String channel, String consumerId, long lastLogId) {
        updateConsumerOffset(consumerId, channel, lastLogId);
        logger.info("Manually committed offset for consumer: {} on channel: {} to logId: {}",
                consumerId, channel, lastLogId);
    }

    @Override
    public void seekToBeginning(String channel, String consumerId) {
        updateConsumerOffset(consumerId, channel, 0L);
        logger.info("Seek to beginning for consumer: {} on channel: {}", consumerId, channel);
    }

    @Override
    public void seekToEnd(String channel, String consumerId) {
        String sql = "SELECT MAX(id) FROM logs WHERE channel = ?";
        long maxId = 0;
        try (Connection conn = dataSource.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, channel);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    maxId = rs.getLong(1);
                }
            }
        } catch (SQLException e) {
            logger.error("Failed to seek to end for channel: {}", channel, e);
            throw new StorageException("Failed to seek to end", e);
        }
        updateConsumerOffset(consumerId, channel, maxId);
        logger.info("Seek to end for consumer: {} on channel: {} (maxId: {})", consumerId, channel, maxId);
    }

    @Override
    public void seekToId(String channel, String consumerId, long logId) {
        // 다음 조회 시 해당 ID부터 시작하도록 오프셋을 ID - 1로 설정합니다.
        // Set offset to logId - 1 so that the next retrieve returns logId.
        updateConsumerOffset(consumerId, channel, logId - 1);
        logger.info("Seek to ID {} for consumer: {} on channel: {}", logId, consumerId, channel);
    }

    private LogEntry mapResultSetToLogEntry(ResultSet rs) throws SQLException {
        LogEntry entry = new LogEntry();
        entry.setChannel(rs.getString("channel"));
        entry.setLevel(LogLevel.valueOf(rs.getString("level")));
        entry.setMessage(rs.getString("message"));
        entry.setTimestamp(rs.getTimestamp("timestamp").toLocalDateTime());

        String metaJson = rs.getString("meta");
        if (metaJson != null) {
            try {
                @SuppressWarnings("unchecked")
                Map<String, Object> meta = objectMapper.readValue(metaJson, Map.class);
                entry.setMeta(meta);
            } catch (JsonProcessingException e) {
                logger.warn("Failed to parse meta JSON: {}", metaJson, e);
            }
        }

        // ResultSet에서 ID를 가져와 설정합니다.
        // Set the ID from ResultSet.
        entry.setId(rs.getLong("id"));

        return entry;
    }

    private Long getConsumerOffset(String consumerId, String channel) {
        String sql = "SELECT last_log_id FROM consumer_offsets WHERE consumer_id = ? AND channel = ?";

        try (Connection conn = dataSource.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, consumerId);
            pstmt.setString(2, channel);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getLong("last_log_id");
                }
            }
        } catch (SQLException e) {
            logger.error("Failed to get consumer offset", e);
        }

        return 0L;
    }

    private void updateConsumerOffset(String consumerId, String channel, Long logId) {
        String sql = """
                INSERT INTO consumer_offsets (consumer_id, channel, last_log_id)
                VALUES (?, ?, ?)
                ON CONFLICT(consumer_id, channel)
                DO UPDATE SET last_log_id = excluded.last_log_id
                """;

        try (Connection conn = dataSource.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, consumerId);
            pstmt.setString(2, channel);
            pstmt.setLong(3, logId);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            logger.error("Failed to update consumer offset", e);
        }
    }

    @Override
    public void close() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
            logger.info("SQLite storage connection pool closed");
        }
    }
}