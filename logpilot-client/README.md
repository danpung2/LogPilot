# LogPilot Java Client

The official Java client for LogPilot, supporting both REST and gRPC (coming soon) protocols.

## Installation

Add the dependency to your `build.gradle`:

```gradle
implementation 'com.logpilot:logpilot-client:1.0.0'
```

## Usage

### Basic Usage (REST)

```java
// Create a client instance
LogPilotClient client = LogPilotClient.builder()
    .serverUrl("http://localhost:8080")
    .build();

// Log a message
client.log("my-channel", LogLevel.INFO, "Hello, LogPilot!");

// Log with metadata
Map<String, Object> meta = Map.of("userId", "12345", "action", "login");
client.log("my-channel", LogLevel.INFO, "User logged in", meta);

// Store using Async (Non-blocking)
client.logAsync("my-channel", LogLevel.DEBUG, "Async log message");

// Close the client when done
client.close();
```

### Advanced Usage: Async Batching

For high-throughput applications, enable asynchronous batching to reduce network overhead. Logs will be buffered internally and sent in batches.

```java
LogPilotClient client = LogPilotClient.builder()
    .serverUrl("http://localhost:8080")
    .enableBatching(true)          // Enable batching
    .batchSize(500)                // Flush when 500 logs are queued (default: 100)
    .flushIntervalMillis(1000)     // Flush every 1 second (default: 5000ms)
    .apiKey("logpilot-secret-key-123") // Server API Key
    .build();

// Note: If you are using 'logpilot-spring-boot-starter', 'enableBatching' is set to TRUE by default.
// You can override it in application.yml: logpilot.client.enable-batching=false

// Logs are queued and sent automatically in background
client.log("high-volume-channel", LogLevel.INFO, "This is a batched log");
```

### Graceful Shutdown

The client implements `AutoCloseable`. When `close()` is called, any remaining logs in the buffer will be flushed before the client shuts down.

```java
try (LogPilotClient client = LogPilotClient.builder()
        .serverUrl("http://localhost:8080")
        .enableBatching(true)
        .build()) {
    
    // Application logic...
    client.log("app-channel", LogLevel.INFO, " Application stopping...");
}
// Client closes automatically, flushing pending logs.
```
