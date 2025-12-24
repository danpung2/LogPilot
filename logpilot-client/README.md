# LogPilot Client (Java)

The Java SDK for LogPilot, providing high-performance tools for both **Producers** (event publishing) and **Consumers** (event processing).

## Features
- **gRPC & REST**: Multi-protocol support for both publishing and consuming events.
- **Reliability**: Integrated local buffer (SQLite) for Producers to prevent data loss during network failures.
- **Kafka-style Consumption**: Consumer-side offset management for reliable message processing.
- **Async Batching**: High-throughput event publishing with background batching.

## Installation

### Gradle
```groovy
implementation project(':logpilot-client')
```

## Usage

```java
LogPilotClient client = LogPilotClient.builder()
    .pilotServerAddress("localhost", 50051)
    .serviceName("my-service")
    .build();

client.send("info", "Hello LogPilot!");
```

