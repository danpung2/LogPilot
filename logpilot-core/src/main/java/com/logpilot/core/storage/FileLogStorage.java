package com.logpilot.core.storage;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.logpilot.core.model.LogEntry;
import com.logpilot.core.model.LogLevel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class FileLogStorage implements LogStorage {

    private static final Logger logger = LoggerFactory.getLogger(FileLogStorage.class);
    private static final String LOG_FILE_EXTENSION = ".log";
    private static final String OFFSET_FILE_EXTENSION = ".offset";
    private static final DateTimeFormatter TIMESTAMP_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    private final String storageDirectory;
    private final ObjectMapper objectMapper;
    private final Map<String, Long> consumerOffsets;
    private final ReentrantReadWriteLock lock;
    private final Path offsetDir;

    public FileLogStorage(String storageDirectory) {
        this.storageDirectory = storageDirectory;
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
        this.consumerOffsets = new ConcurrentHashMap<>();
        this.lock = new ReentrantReadWriteLock();
        this.offsetDir = Paths.get(storageDirectory, ".offsets");
        initialize();
    }

    @Override
    public void initialize() {
        try {
            Path storagePath = Paths.get(storageDirectory);
            Files.createDirectories(storagePath);
            Files.createDirectories(offsetDir);

            loadConsumerOffsets();

            logger.info("File storage initialized at: {}", storageDirectory);
        } catch (IOException e) {
            throw new RuntimeException("Failed to initialize file storage", e);
        }
    }

    @Override
    public void store(LogEntry logEntry) {
        lock.writeLock().lock();
        try {
            Path logFile = getLogFilePath(logEntry.getChannel());

            String logLine = formatLogEntry(logEntry);

            Files.write(logFile, (logLine + System.lineSeparator()).getBytes(),
                       StandardOpenOption.CREATE, StandardOpenOption.APPEND);

            logger.debug("Stored log entry to file: {} for channel: {}",
                        logFile.getFileName(), logEntry.getChannel());
        } catch (IOException e) {
            logger.error("Failed to store log entry to file", e);
            throw new RuntimeException("Failed to store log entry to file", e);
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public void storeLogs(List<LogEntry> logEntries) {
        if (logEntries == null || logEntries.isEmpty()) {
            return;
        }

        lock.writeLock().lock();
        try {
            // 채널별로 로그 엔트리들을 그룹화
            Map<String, List<LogEntry>> entriesByChannel = new HashMap<>();
            for (LogEntry logEntry : logEntries) {
                entriesByChannel.computeIfAbsent(logEntry.getChannel(), k -> new ArrayList<>())
                               .add(logEntry);
            }

            // 채널별로 배치 저장
            for (Map.Entry<String, List<LogEntry>> channelEntry : entriesByChannel.entrySet()) {
                String channel = channelEntry.getKey();
                List<LogEntry> channelEntries = channelEntry.getValue();

                Path logFile = getLogFilePath(channel);

                // 모든 로그 라인을 미리 생성
                StringBuilder batchContent = new StringBuilder();
                for (LogEntry logEntry : channelEntries) {
                    String logLine = formatLogEntry(logEntry);
                    batchContent.append(logLine).append(System.lineSeparator());
                }

                // 한 번의 파일 쓰기로 배치 저장
                Files.write(logFile, batchContent.toString().getBytes(),
                           StandardOpenOption.CREATE, StandardOpenOption.APPEND);

                logger.debug("Stored {} log entries to file: {} for channel: {}",
                            channelEntries.size(), logFile.getFileName(), channel);
            }

            logger.debug("Stored total {} log entries across {} channels",
                        logEntries.size(), entriesByChannel.size());
        } catch (IOException e) {
            logger.error("Failed to store log entries to files", e);
            throw new RuntimeException("Failed to store log entries to files", e);
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public List<LogEntry> retrieve(String channel, String consumerId, int limit) {
        return retrieve(channel, consumerId, limit, true);
    }

    @Override
    public List<LogEntry> retrieve(String channel, String consumerId, int limit, boolean autoCommit) {
        lock.readLock().lock();
        try {
            Path logFile = getLogFilePath(channel);
            if (!Files.exists(logFile)) {
                return new ArrayList<>();
            }

            String offsetKey = consumerId + ":" + channel;
            long lastLineNumber = consumerOffsets.getOrDefault(offsetKey, 0L);

            List<String> allLines = Files.readAllLines(logFile);
            List<LogEntry> entries = new ArrayList<>();

            long currentLineNumber = 0;
            long maxLineNumber = lastLineNumber;

            for (String line : allLines) {
                currentLineNumber++;

                if (currentLineNumber <= lastLineNumber) {
                    continue;
                }

                if (entries.size() >= limit) {
                    break;
                }

                try {
                    LogEntry entry = parseLogEntry(line, currentLineNumber);
                    if (entry != null) {
                        entries.add(entry);
                        maxLineNumber = currentLineNumber;
                    }
                } catch (Exception e) {
                    logger.warn("Failed to parse log line {}: {}", currentLineNumber, line, e);
                }
            }

            if (autoCommit && maxLineNumber > lastLineNumber) {
                consumerOffsets.put(offsetKey, maxLineNumber);
                saveConsumerOffset(offsetKey, maxLineNumber);
            }

            logger.debug("Retrieved {} log entries for channel: {} and consumer: {} (autoCommit={})",
                        entries.size(), channel, consumerId, autoCommit);
            return entries;

        } catch (IOException e) {
            logger.error("Failed to retrieve log entries from file", e);
            throw new RuntimeException("Failed to retrieve log entries from file", e);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public void commitOffset(String channel, String consumerId, long lastLogId) {
        String offsetKey = consumerId + ":" + channel;
        consumerOffsets.put(offsetKey, lastLogId);
        saveConsumerOffset(offsetKey, lastLogId);
        logger.info("Manually committed offset for consumer: {} on channel: {} to logId: {}",
                consumerId, channel, lastLogId);
    }

    @Override
    public List<LogEntry> retrieveAll(int limit) {
        lock.readLock().lock();
        try {
            List<LogEntry> allEntries = new ArrayList<>();
            Path storageDir = Paths.get(storageDirectory);

            List<Path> logFiles;
            try (var pathStream = Files.list(storageDir)) {
                logFiles = pathStream
                        .filter(path -> path.toString().endsWith(LOG_FILE_EXTENSION))
                        .sorted((p1, p2) -> {
                            try {
                                return Files.getLastModifiedTime(p2).compareTo(Files.getLastModifiedTime(p1));
                            } catch (IOException e) {
                                return 0;
                            }
                        })
                        .toList();
            }

            for (Path logFile : logFiles) {
                if (allEntries.size() >= limit) {
                    break;
                }

                try {
                    List<String> lines = Files.readAllLines(logFile);

                    for (int i = lines.size() - 1; i >= 0 && allEntries.size() < limit; i--) {
                        try {
                            LogEntry entry = parseLogEntry(lines.get(i), i + 1);
                            if (entry != null) {
                                allEntries.add(entry);
                            }
                        } catch (Exception e) {
                            logger.warn("Failed to parse log line from file {}: {}",
                                       logFile.getFileName(), lines.get(i), e);
                        }
                    }
                } catch (IOException e) {
                    logger.warn("Failed to read log file: {}", logFile, e);
                }
            }

            logger.debug("Retrieved {} total log entries", allEntries.size());
            return allEntries;

        } catch (IOException e) {
            logger.error("Failed to retrieve all log entries", e);
            throw new RuntimeException("Failed to retrieve all log entries", e);
        } finally {
            lock.readLock().unlock();
        }
    }

    private Path getLogFilePath(String channel) {
        String sanitizedChannel = channel.replaceAll("[^a-zA-Z0-9._-]", "_");
        return Paths.get(storageDirectory, sanitizedChannel + LOG_FILE_EXTENSION);
    }

    private String formatLogEntry(LogEntry logEntry) {
        try {
            Map<String, Object> logData = new HashMap<>();
            logData.put("timestamp", logEntry.getTimestamp().format(TIMESTAMP_FORMATTER));
            logData.put("channel", logEntry.getChannel());
            logData.put("level", logEntry.getLevel().name());
            logData.put("message", logEntry.getMessage());

            if (logEntry.getMeta() != null && !logEntry.getMeta().isEmpty()) {
                logData.put("meta", logEntry.getMeta());
            }

            return objectMapper.writeValueAsString(logData);
        } catch (JsonProcessingException e) {
            logger.error("Failed to format log entry", e);
            throw new RuntimeException("Failed to format log entry", e);
        }
    }

    private LogEntry parseLogEntry(String line, long lineNumber) {
        if (line == null || line.trim().isEmpty()) {
            return null;
        }

        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> logData = objectMapper.readValue(line, Map.class);

            LogEntry entry = new LogEntry();
            entry.setChannel((String) logData.get("channel"));
            entry.setLevel(LogLevel.valueOf((String) logData.get("level")));
            entry.setMessage((String) logData.get("message"));

            String timestampStr = (String) logData.get("timestamp");
            if (timestampStr != null) {
                entry.setTimestamp(LocalDateTime.parse(timestampStr, TIMESTAMP_FORMATTER));
            }

            @SuppressWarnings("unchecked")
            Map<String, Object> meta = (Map<String, Object>) logData.get("meta");
            if (meta != null) {
                entry.setMeta(meta);
            }

            if (meta != null) {
                entry.setMeta(meta);
            }

            // Set the ID (using line number as ID for File storage)
            entry.setId(lineNumber);

            return entry;
        } catch (Exception e) {
            logger.warn("Failed to parse log entry from line {}: {}", lineNumber, line, e);
            return null;
        }
    }

    private void loadConsumerOffsets() {
        try {
            if (!Files.exists(offsetDir)) {
                return;
            }

            try (var pathStream = Files.list(offsetDir)) {
                pathStream
                        .filter(path -> path.toString().endsWith(OFFSET_FILE_EXTENSION))
                        .forEach(offsetFile -> {
                            try {
                                String fileName = offsetFile.getFileName().toString();
                                String offsetKey = fileName.substring(0, fileName.length() - OFFSET_FILE_EXTENSION.length());

                                List<String> lines = Files.readAllLines(offsetFile);
                                if (!lines.isEmpty()) {
                                    long offset = Long.parseLong(lines.get(0).trim());
                                    consumerOffsets.put(offsetKey, offset);
                                    logger.debug("Loaded consumer offset: {} = {}", offsetKey, offset);
                                }
                            } catch (Exception e) {
                                logger.warn("Failed to load consumer offset from file: {}", offsetFile, e);
                            }
                        });
            }

            logger.info("Loaded {} consumer offsets", consumerOffsets.size());
        } catch (IOException e) {
            logger.warn("Failed to load consumer offsets", e);
        }
    }

    private void saveConsumerOffset(String offsetKey, long offset) {
        try {
            String sanitizedKey = offsetKey.replaceAll("[^a-zA-Z0-9._:-]", "_");
            Path offsetFile = offsetDir.resolve(sanitizedKey + OFFSET_FILE_EXTENSION);

            Files.write(offsetFile, String.valueOf(offset).getBytes(),
                       StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);

            logger.debug("Saved consumer offset: {} = {}", offsetKey, offset);
        } catch (IOException e) {
            logger.error("Failed to save consumer offset: {} = {}", offsetKey, offset, e);
        }
    }

    @Override
    public void close() {
        lock.writeLock().lock();
        try {
            for (Map.Entry<String, Long> entry : consumerOffsets.entrySet()) {
                saveConsumerOffset(entry.getKey(), entry.getValue());
            }

            logger.info("File storage closed and consumer offsets saved");
        } finally {
            lock.writeLock().unlock();
        }
    }
}