# LogPilot Demo Application

LogPilot 연동 방법과 트래픽 시뮬레이션을 보여주기 위한 예제 Spring Boot 애플리케이션입니다.

## 주요 기능
- **로그 생성**: 다양한 레벨(INFO, WARN, ERROR)과 패턴의 로그를 자동으로 생성합니다.
- **연동 예제**: `logpilot-spring-boot-starter` 및 `logpilot-logback`의 사용법을 보여줍니다.
- **트래픽 시뮬레이터**: LogPilot의 성능 테스트를 위해 실제와 유사한 트래픽 패턴을 시뮬레이션합니다.

## 사용법

Gradle을 사용하여 데모 애플리케이션을 실행합니다:

```bash
./gradlew :logpilot-demo:bootRun
```

애플리케이션이 실행되면 설정된 LogPilot Server(기본값: localhost:50051)로 로그 전송을 시작합니다.

