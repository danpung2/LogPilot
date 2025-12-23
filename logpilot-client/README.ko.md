# LogPilot Client (Java)

LogPilot Server로 로그를 안정적이고 효율적으로 전송하기 위한 Java 클라이언트 구현체입니다.

## 주요 기능
- **gRPC 기반**: 고성능 gRPC 프로토콜을 사용하여 로그를 전송합니다.
- **안정성 (Reliability)**: 네트워크 장애 시 데이터 유실을 방지하기 위해 로컬 영구 버퍼(SQLite)를 내장하고 있습니다.
- **비동기 배칭 (Async Batching)**: 애플리케이션 스레드를 차단하지 않고 백그라운드에서 로그를 모아 비동기로 전송합니다.
- **자동 재시도**: 전송 실패 시 자동으로 재시도를 수행합니다.

## 설치 방법

### Gradle
```groovy
implementation project(':logpilot-client')
```

## 사용법

```java
LogPilotClient client = LogPilotClient.builder()
    .pilotServerAddress("localhost", 50051)
    .serviceName("my-service")
    .build();

client.send("info", "Hello LogPilot!");
```

