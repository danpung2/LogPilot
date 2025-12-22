# LogPilot Spring Boot Starter

이 스타터는 Spring Boot 애플리케이션에서 LogPilot Client를 자동으로 설정해줍니다.

## 설치 (Installation)

`build.gradle`에 의존성을 추가하세요:

```groovy
implementation project(':logpilot-spring-boot-starter')
```

## 설정 (Configuration - application.yml)

`application.yml`에 다음과 같이 속성을 추가하면 됩니다:

```yaml
logpilot:
  client:
    server-url: http://localhost:8080
    enable-batching: true
    batch-size: 100
    flush-interval-millis: 5000
```

## 사용법 (Usage)

`LogPilotClient` 빈(Bean)이 자동으로 생성됩니다. 직접 주입받아 사용할 수 있습니다:

```java
@Autowired
private LogPilotClient logPilotClient;

public void doSomething() {
    logPilotClient.log("my-channel", LogLevel.INFO, "Hello World!");
}
```

또는 `logpilot-logback`과 결합하여 표준 `log.info()`를 사용하는 것을 권장합니다.

## Logback 연동 (Spring Profile 사용)

`logback-spring.xml`을 사용하는 경우, 설정된 속성을 다음과 같이 참조할 수 있습니다:

```xml
<springProperty scope="context" name="serverUrl" source="logpilot.client.server-url"/>

<appender name="LOGPILOT" class="com.logpilot.logback.LogPilotAppender">
    <serverUrl>${serverUrl}</serverUrl>
    ...
</appender>
```
