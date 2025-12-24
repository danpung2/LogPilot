# LogPilot 클라이언트 가이드

이 문서는 애플리케이션에 LogPilot 클라이언트를 연동하고 **이벤트/메시지 스트리밍**에 활용하는 방법을 설명합니다. **Producer**(`logpilot-demo-produce`)와 **Consumer**(`logpilot-demo-consume`) 데모 모듈을 참조하여 '경량 카프카' 메커니즘을 안내합니다.

> **⚠️ 필수 조건**: 클라이언트를 실행하기 전에 **LogPilot Server**가 실행 중이어야 합니다.
>
> **옵션 A: Gradle 실행**
> ```bash
> ./gradlew :logpilot-server:bootRun
> ```
>
> **옵션 B: Docker 실행**
> ```bash
> docker build -t logpilot-server .
> docker run -p 8080:8080 -p 50051:50051 logpilot-server
> ```
>
> **옵션 C: Kubernetes 실행**
> ```bash
> # 상세 가이드는 K8S.ko.md 참조
> kubectl apply -f k8s/
> ```
> 기본 서버 주소: `localhost:50051` (gRPC) / `http://localhost:8080` (REST)

---

## 1. 모드 선택: REST vs gRPC

LogPilot은 두 가지 통신 프로토콜을 지원합니다. 요구 사항에 가장 적합한 방식을 선택하세요.

| 특징 | REST (HTTP/1.1) | gRPC (HTTP/2 + Protobuf) |
| :--- | :--- | :--- |
| **성능** | 보통 (텍스트 기반 JSON) | **높음** (바이너리 Protobuf) |
| **처리량 (Throughput)** | 표준 | **높음** (Multiplexing 지원) |
| **타입 안정성** | 느슨함 (JSON) | **엄격함** (IDL 정의) |
| **설정 난이도** | 쉬움 (표준 HTTP) | HTTP/2 지원 필요 |
| **권장 대상** | 단순 로깅, 방화벽 제약 환경 | **대용량 로그 생성**, **로그 분석/소비** |

### 🚀 추천 사항
*   **gRPC 사용**: 프로덕션 환경, 고성능 이벤트 스트림, 또는 실시간 데이터를 처리해야 하는 Consumer 애플리케이션.
*   **REST 사용**: 간단한 테스트, 경량 생산자(Producer), 또는 gRPC 포트 사용이 제한된 환경.

---

## 2. Producer 연동 (이벤트 발행)

**Producer**는 이벤트나 메시지를 생성하여 서버로 전송합니다. 애플리케이션 로그를 스트림으로 보내려면 **Logback Appender**를, 커스텀 이벤트를 보내려면 `LogPilotClient`를 직접 사용하는 것이 좋습니다.

**Producer**는 로그를 생성하여 서버로 전송합니다. **Logback Appender**를 사용하는 것이 가장 간편합니다.

### 예시: `logpilot-demo-produce`
이 모듈은 트래픽이 많은 채용 사이트를 시뮬레이션합니다. `LogPilotAppender`를 사용하여 로그를 배치(Batch)로 묶어 비동기 전송합니다.

**설정 예시 (`logback-spring.xml`):**
```xml
<appender name="LOGPILOT" class="com.logpilot.logback.LogPilotAppender">
    <!-- Appender는 기본적으로 REST를 사용하지만 설정 가능 -->
    <serverUrl>http://localhost:8080</serverUrl>
    <serviceName>my-service</serviceName>
    <apiKey>your-api-key</apiKey>

    <!-- 성능 최적화 (배치 전송) -->
    <enableBatching>true</enableBatching>
    <batchSize>100</batchSize>
    <flushIntervalMillis>5000</flushIntervalMillis>
</appender>
```

> **Tip**: 위 설정처럼 `batching`을 활성화하면 메인 스레드 차단을 방지하고 네트워크 오버헤드를 획기적으로 줄일 수 있습니다.

---

## 3. Consumer 연동 (메시지 소비)

**Consumer**는 서버에서 메시지를 가져와 처리하거나 실시간 액션을 수행합니다. LogPilot은 Kafka 스타일의 **오프셋 관리(Offset Management)**를 제공하여 각 메시지가 안정적으로 한 번씩 처리되도록 보장합니다.

### 예시: `logpilot-demo-consume`
이 모듈은 매초 로그를 폴링하여 실시간 채용 통계를 계산합니다. 효율적인 데이터 조회를 위해 **gRPC**를 사용합니다.

**코드 예시 (`AnalyticsService.java`):**
```java
// 클라이언트 초기화 (gRPC 모드)
LogPilotClient client = LogPilotClient.builder()
        .serverUrl("localhost:50051")
        .clientType(LogPilotClient.ClientType.GRPC)
        .build();

// 소비자 ID를 사용한 이벤트 조회 (안정적인 오프셋 추적)
// getLogs(channel, consumerId, limit)
List<LogEntry> events = client.getLogs("orders", "inventory-service", 100);

for (LogEntry event : events) {
    process(event);
    // autoCommit=false가 아닌 한 오프셋은 자동으로 커밋됩니다.
}
```

> **왜 gRPC인가?**: 데모처럼 1초마다 빈번하게 로그를 폴링해야 하는 경우, gRPC의 지속 연결(Persistent Connection)과 바이너리 포맷은 REST 방식에 비해 CPU 사용량과 지연 시간(Latency)을 크게 감소시킵니다.

---

## 4. 데모 실행해보기

위의 패턴들이 실제로 동작하는 모습을 확인해보세요:

1.  **서버 시작**: `./gradlew :logpilot-server:bootRun`
2.  **Producer 시작**: `./gradlew :logpilot-demo-produce:bootRun` (Port 8082)
    *   트래픽 생성 (STEADY 모드): `curl -X POST "http://localhost:8082/simulation/start?mode=STEADY"`
    *   트래픽 생성 중지: `curl -X POST "http://localhost:8082/simulation/stop"`
3.  **Consumer 시작**: `./gradlew :logpilot-demo-consume:bootRun` (Port 8083)
    *   통계 확인: `curl http://localhost:8083/analytics/stats`
