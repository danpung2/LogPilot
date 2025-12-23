# LogPilot Client (Java)

The Java client implementation for LogPilot, designed to send logs to the LogPilot Server reliably and efficiently.

## Features
- **gRPC Based**: Uses high-performance gRPC for log transmission.
- **Reliability**: Features a persistent local buffer (SQLite) to prevent data loss during network failures.
- **Async Batching**: Sends logs in batches asynchronously to avoid blocking application threads.
- **Automatic Retry**: Automatically retries failed transmissions.

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

