# LogPilot Java 클라이언트

REST와 gRPC(출시 예정) 프로토콜을 모두 지원하는 공식 LogPilot Java 클라이언트입니다.

## 설치 (Installation)

`build.gradle`에 의존성을 추가하세요:

```gradle
implementation 'com.logpilot:logpilot-client:1.0.0'
```

## 사용법 (Usage)

### 기본 사용법 (Basic Usage - REST)

```java
// 클라이언트 인스턴스 생성
LogPilotClient client = LogPilotClient.builder()
    .serverUrl("http://localhost:8080")
    .build();

// 로그 메시지 전송
client.log("my-channel", LogLevel.INFO, "Hello, LogPilot!");

// 메타데이터와 함께 로그 전송
Map<String, Object> meta = Map.of("userId", "12345", "action", "login");
client.log("my-channel", LogLevel.INFO, "User logged in", meta);

// 비동기(Non-blocking)로 저장
client.logAsync("my-channel", LogLevel.DEBUG, "Async log message");

// 사용 후 클라이언트 종료 (AutoCloseable)
client.close();
```

### 고급 사용법: 비동기 배치 (Async Batching)

높은 처리량이 필요한 애플리케이션의 경우, 네트워크 오버헤드를 줄이기 위해 비동기 배치를 활성화하세요. 로그는 내부적으로 버퍼링되어 배치 단위로 전송됩니다.

```java
LogPilotClient client = LogPilotClient.builder()
    .serverUrl("http://localhost:8080")
    .enableBatching(true)          // 배치 활성화
    .batchSize(500)                // 500개 로그가 쌓이면 전송 (기본값: 100)
    .flushIntervalMillis(1000)     // 1초마다 전송 (기본값: 5000ms)
    .build();

// 로그가 큐에 쌓이고 백그라운드에서 자동으로 전송됨
client.log("high-volume-channel", LogLevel.INFO, "This is a batched log");
```

### 우아한 종료 (Graceful Shutdown)

클라이언트는 `AutoCloseable`을 구현합니다. `close()`가 호출되면 클라이언트가 종료되기 전에 버퍼에 남은 모든 로그가 전송됩니다.

```java
try (LogPilotClient client = LogPilotClient.builder()
        .serverUrl("http://localhost:8080")
        .enableBatching(true)
        .build()) {
    
    // 애플리케이션 로직...
    client.log("app-channel", LogLevel.INFO, " Application stopping...");
}
// 클라이언트가 자동으로 종료되며, 대기 중인 로그를 전송합니다.
```
