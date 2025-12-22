# LogPilot Logback Appender

This module provides a [Logback](http://logback.qos.ch/) Appender for LogPilot, allowing you to ship logs directly from standard SLF4J calls.

## Installation

Add the dependency to your `build.gradle`:

```groovy
implementation project(':logpilot-logback')
```

## Configuration (logback.xml)

```xml
<configuration>
    <!-- Define the Appender -->
    <appender name="LOGPILOT" class="com.logpilot.logback.LogPilotAppender">
        <serverUrl>http://localhost:8080</serverUrl>
        <serviceName>my-service</serviceName>
        <enableBatching>true</enableBatching>
        <batchSize>100</batchSize>
        <flushIntervalMillis>5000</flushIntervalMillis>
        <apiKey>logpilot-secret-key-123</apiKey>
    </appender>

    <!-- Configure Root Logger -->
    <root level="INFO">
        <appender-ref ref="LOGPILOT" />
        <appender-ref ref="CONSOLE" />
    </root>
</configuration>
```

## Features
- **Async by Default**: Uses LogPilot Client's async capabilities.
- **Batching Support**: Configurable batch size and flush intervals.
- **Metadata**: Automatically captures `logger` name and `thread` name.
