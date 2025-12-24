# LogPilot Logback Appender

Java 애플리케이션의 로그를 **이벤트 스트림**으로 변환하여 LogPilot 브로커로 전송하는 Logback Appender 구현체입니다.

## 주요 기능
- **간편한 연동**: 표준 `logback.xml` 설정을 통해 쉽게 적용할 수 있습니다.
- **비동기 지원**: Logback의 `AsyncAppender`와 함께 사용하여 성능 저하를 최소화할 수 있습니다.
- **메타데이터**: MDC (Mapped Diagnostic Context) 데이터를 자동으로 수집합니다.

## 사용법

`logback.xml` 파일에 다음 설정을 추가하세요:

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

