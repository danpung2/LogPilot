# LogPilot Logback Appender

A Logback Appender that ships application logs as **Event Streams** to the LogPilot Broker.

## Features
- **Seamless Integration**: Works directly with standard `logback.xml` configuration.
- **Async Support**: Can be wrapped with `AsyncAppender` for non-blocking performance.
- **Metadata**: Automatically captures MDC (Mapped Diagnostic Context) data.

## Usage

Add the following to your `logback.xml`:

```xml
<configuration>
    <appender name="LOGPILOT" class="com.logpilot.logback.LogPilotAppender">
        <serviceName>my-service</serviceName>
        <pilotServerAddress>localhost</pilotServerAddress>
        <pilotServerPort>50051</pilotServerPort>
    </appender>

    <root level="INFO">
        <appender-ref ref="LOGPILOT" />
    </root>
</configuration>
```

