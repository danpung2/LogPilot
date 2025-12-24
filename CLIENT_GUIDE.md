# LogPilot Client Guide

This guide explains how to integrate and use the LogPilot Client for **Event/Message Streaming**. It references the **Producer** (`logpilot-demo-produce`) and **Consumer** (`logpilot-demo-consume`) demo modules to demonstrate the Lightweight Kafka mechanism.

> **⚠️ Prerequisite**: The **LogPilot Server** must be running before starting any client.
>
> **Option A: Run with Gradle**
> ```bash
> ./gradlew :logpilot-server:bootRun
> ```
>
> **Option B: Run with Docker**
> ```bash
> docker build -t logpilot-server .
> docker run -p 8080:8080 -p 50051:50051 logpilot-server
> ```
>
> **Option C: Run with Kubernetes**
> ```bash
> # See K8S.md for details
> kubectl apply -f k8s/
> ```
> Default Server URL: `localhost:50051` (gRPC) / `http://localhost:8080` (REST)

---

## 1. Mode Selection: REST vs gRPC

LogPilot supports two communication protocols. Choose the one that best fits your requirements.

| Feature | REST (HTTP/1.1) | gRPC (HTTP/2 + Protobuf) |
| :--- | :--- | :--- |
| **Performance** | Moderate (Text-based JSON) | **High** (Binary Protobuf) |
| **Throughput** | Standard | **High** (Multiplexing) |
| **Type Safety** | Loose (JSON) | **Strict** (IDL defined) |
| **Setup** | Simple (Standard HTTP) | Requires HTTP/2 support |
| **Recommended For** | Simple logging, firewalls blocking non-80 ports | **High-volume Producers**, **Analytics Consumers** |

### 🚀 Recommendation
*   **Use gRPC** for production environments, high-traffic event streams, and real-time consumers.
*   **Use REST** for simple debugging, lightweight producers, or environments where gRPC is restricted.

---

## 2. Producer Integration (Event Publishing)

The **Producer** generates events or messages and sends them to the server. The easiest way to integrate for application logging is using the **Logback Appender**. For custom event streams, use the `LogPilotClient`.

The **Producer** generates logs and sends them to the server. The easiest way to integrate is using the **Logback Appender**.

### Example: `logpilot-demo-produce`
This module simulates a high-traffic recruitment site. It uses the `LogPilotAppender` to asynchronously ship logs in batches.

**Configuration (`logback-spring.xml`):**
```xml
<appender name="LOGPILOT" class="com.logpilot.logback.LogPilotAppender">
    <!-- REST is the default for Appender, but can be configured -->
    <serverUrl>http://localhost:8080</serverUrl>
    <serviceName>my-service</serviceName>
    <apiKey>your-api-key</apiKey>

    <!-- Performance Tuning -->
    <enableBatching>true</enableBatching>
    <batchSize>100</batchSize>
    <flushIntervalMillis>5000</flushIntervalMillis>
</appender>
```

> **Tip**: Enable `batching` (as shown above) to prevent blocking your application's main thread and to reduce network overhead.

---

## 3. Consumer Integration (Message Consumption)

The **Consumer** retrieves messages from the server for processing or real-time action. LogPilot provides Kafka-style **Offset Management** to ensure each message is processed exactly once.

### Example: `logpilot-demo-consume`
This module polls logs to calculate real-time recruiting statistics. It uses **gRPC** for efficient data retrieval.

**Code Example (`AnalyticsService.java`):**
```java
// Initialize Client (gRPC Mode)
LogPilotClient client = LogPilotClient.builder()
        .serverUrl("localhost:50051")
        .clientType(LogPilotClient.ClientType.GRPC)
        .build();

// Fetch Events with Consumer ID (Reliable Offset Tracking)
// getLogs(channel, consumerId, limit)
List<LogEntry> events = client.getLogs("orders", "inventory-service", 100);

for (LogEntry event : events) {
    process(event);
    // Offset is automatically committed by default unless autoCommit=false
}
```

> **Why gRPC here?**: When polling for logs every second (like in the demo), gRPC's persistent connection and binary format significantly reduce CPU usage and latency compared to opening new HTTP connections for every REST call.

---

## 4. Running the Demos

To see these clients in action:

1.  **Start Server**: `./gradlew :logpilot-server:bootRun`
2.  **Start Producer**: `./gradlew :logpilot-demo-produce:bootRun` (Port 8082)
    *   Generate traffic(STEADY mode): `curl -X POST "http://localhost:8082/simulation/start?mode=STEADY"`
    *   Stop traffic: `curl -X POST "http://localhost:8082/simulation/stop"`
3.  **Start Consumer**: `./gradlew :logpilot-demo-consume:bootRun` (Port 8083)
    *   View stats: `curl http://localhost:8083/analytics/stats`
