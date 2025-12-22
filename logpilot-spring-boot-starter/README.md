# LogPilot Spring Boot Starter

This starter provides auto-configuration for LogPilot Client in Spring Boot applications.

## Installation

Add the dependency to your `build.gradle`:

```groovy
implementation project(':logpilot-spring-boot-starter')
```

## Configuration (application.yml)

Simply add the properties:

```yaml
logpilot:
  client:
    server-url: http://localhost:8080
    enable-batching: true
    batch-size: 100
    flush-interval-millis: 5000
    api-key: "logpilot-secret-key-123"
```

## Usage

The `LogPilotClient` bean is auto-configured. You can inject it directly:

```java
@Autowired
private LogPilotClient logPilotClient;

public void doSomething() {
    logPilotClient.log("my-channel", LogLevel.INFO, "Hello World!");
}
```

Or better yet, combine it with `logpilot-logback` to use standard `log.info()`!

## Integration with Logback (Spring Profile)

If you use `logback-spring.xml`, you can refer to these properties:

```xml
<springProperty scope="context" name="serverUrl" source="logpilot.client.server-url"/>

<appender name="LOGPILOT" class="com.logpilot.logback.LogPilotAppender">
    <serverUrl>${serverUrl}</serverUrl>
    ...
</appender>
```
