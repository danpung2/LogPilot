# LogPilot Logback Appender

이 모듈은 LogPilot을 위한 [Logback](http://logback.qos.ch/) Appender를 제공하여, 표준 SLF4J 호출을 통해 로그를 직접 전송할 수 있게 합니다.

## 설치 (Installation)

`build.gradle`에 의존성을 추가하세요:

```groovy
implementation project(':logpilot-logback')
```

## 설정 (Configuration - logback.xml)

```xml
<configuration>
    <!-- Appender 정의 -->
    <appender name="LOGPILOT" class="com.logpilot.logback.LogPilotAppender">
        <serverUrl>http://localhost:8080</serverUrl>
        <serviceName>my-service</serviceName>
        <enableBatching>true</enableBatching>
        <batchSize>100</batchSize>
        <flushIntervalMillis>5000</flushIntervalMillis>
    </appender>

    <!-- Root Logger 설정 -->
    <root level="INFO">
        <appender-ref ref="LOGPILOT" />
        <appender-ref ref="CONSOLE" />
    </root>
</configuration>
```

## 주요 기능
- **기본 비동기 (Async by Default)**: LogPilot Client의 비동기 기능을 활용합니다.
- **배치 지원**: 배치 크기와 전송 주기를 설정할 수 있습니다.
- **메타데이터**: `logger` 이름과 `thread` 이름을 자동으로 수집합니다.
