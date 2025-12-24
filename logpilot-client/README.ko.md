# LogPilot Client (Java)

LogPilot 서버와 통신하기 위한 Java SDK로, **Producer**(이벤트 발행)와 **Consumer**(이벤트 처리) 모두를 위한 고성능 기능을 제공합니다.

## 주요 기능
- **gRPC 및 REST**: 이벤트 발행 및 소비를 위한 멀티 프로토콜 지원.
- **안정성 (Reliability)**: 네트워크 장애 시 데이터 유실 방지를 위한 Producer 로컬 버퍼(SQLite) 내장.
- **Kafka 스타일 소비**: 안정적인 메시지 처리를 위한 소비자 오프셋 관리 기능.
- **비동기 배칭 (Async Batching)**: 대량의 이벤트를 백그라운드에서 모아 고성능으로 전송.

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

