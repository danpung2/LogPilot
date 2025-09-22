package com.logpilot.core.storage;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.logpilot.core.model.LogEntry;
import com.logpilot.core.model.LogLevel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class SqliteLogStorage implements LogStorage {

    private static final Logger logger = LoggerFactory.getLogger(SqliteLogStorage.class);
    private final String dbPath;
    private final ObjectMapper objectMapper;
    private Connection connection;

    public SqliteLogStorage(String dbPath) {
        this.dbPath = dbPath;
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
    }

    @Override
    public void initialize() {
        try {
            connection = DriverManager.getConnection("jdbc:sqlite:" + dbPath);
            createTablesIfNotExists();
            logger.info("SQLite storage initialized at: {}", dbPath);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to initialize SQLite storage", e);
        }
    }

    private void createTablesIfNotExists() throws SQLException {
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

        try (Statement stmt = connection.createStatement()) {
            stmt.execute(createLogsTable);
            stmt.execute(createConsumerOffsetsTable);
        }
    }

    @Override
    public void store(LogEntry logEntry) {
        String sql = "INSERT INTO logs (channel, level, message, meta, timestamp) VALUES (?, ?, ?, ?, ?)";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
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
            throw new RuntimeException("Failed to store log entry", e);
        }
    }

    @Override
    public List<LogEntry> retrieve(String channel, String consumerId, int limit) {
        long lastLogId = getConsumerOffset(consumerId, channel);

        String sql = "SELECT id, channel, level, message, meta, timestamp FROM logs " +
                    "WHERE channel = ? AND id > ? ORDER BY id ASC LIMIT ?";

        List<LogEntry> entries = new ArrayList<>();
        long maxLogId = lastLogId;

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
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

            if (maxLogId > lastLogId) {
                updateConsumerOffset(consumerId, channel, maxLogId);
            }

            logger.debug("Retrieved {} log entries for channel: {} and consumer: {}",
                        entries.size(), channel, consumerId);
        } catch (SQLException e) {
            logger.error("Failed to retrieve log entries", e);
            throw new RuntimeException("Failed to retrieve log entries", e);
        }

        return entries;
    }

    @Override
    public List<LogEntry> retrieveAll(int limit) {
        String sql = "SELECT id, channel, level, message, meta, timestamp FROM logs " +
                    "ORDER BY id DESC LIMIT ?";

        List<LogEntry> entries = new ArrayList<>();

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, limit);

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    entries.add(mapResultSetToLogEntry(rs));
                }
            }

            logger.debug("Retrieved {} log entries", entries.size());
        } catch (SQLException e) {
            logger.error("Failed to retrieve all log entries", e);
            throw new RuntimeException("Failed to retrieve all log entries", e);
        }

        return entries;
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

        return entry;
    }

    private Long getConsumerOffset(String consumerId, String channel) {
        String sql = "SELECT last_log_id FROM consumer_offsets WHERE consumer_id = ? AND channel = ?";

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
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

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
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
        if (connection != null) {
            try {
                connection.close();
                logger.info("SQLite storage connection closed");
            } catch (SQLException e) {
                logger.error("Failed to close SQLite connection", e);
            }
        }
    }
}