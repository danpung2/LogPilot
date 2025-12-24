# LogPilot

**LogPilot은 Java 17 및 Spring Boot 3로 구축된 경량 클라우드 네이티브 이벤트 스트리밍 브로커(Lightweight, Cloud-Native Event Streaming Broker)입니다.** **Apache Kafka**에서 영감을 받아 설계되었으며, 마이크로서비스 간의 이벤트 기반 통신을 위해 이중 프로토콜(gRPC + REST), 영구 저장소, 그리고 안정적인 소비자 오프셋(Consumer Offset) 관리 기능을 제공합니다.

> [!WARNING]
> **프로덕션 보안 경고**: 기본 설정된 `LOGPILOT_API_KEY`는 예시 값입니다. 공유 환경이나 프로덕션 환경에 배포하기 전에 **반드시 이 값을 변경해야 합니다**. 변경하지 않을 경우 무단 접근의 위험이 있습니다.
>
> **호환성 참고**: 이 프로젝트는 **Java 17 이상** 및 **Spring Boot 3.x**를 엄격하게 요구합니다. 이전 버전(Java 8/11, Spring Boot 2.x)은 **지원하지 않습니다**.

[![License](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)
[![Java](https://img.shields.io/badge/Java-17-orange)](https://www.oracle.com/java/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.3.4-green)](https://spring.io/projects/spring-boot)

---

### LogPilot을 선택해야 하는 이유

**Apache Kafka**와 같은 기존 이벤트 스트리밍 플랫폼은 강력하지만, 중소규모 프로젝트에서 운영하기에는 매우 무겁고 복잡합니다. LogPilot은 몇 분 만에 실행할 수 있는 **가볍고 독립적인 대안**을 제공합니다. 마이크로서비스 간의 이벤트 알림, 가벼운 감사 로그(Audit Log), 분산 이벤트 파이프라인 구축에 최적화되어 있습니다.

```
기존 Kafka 스택                LogPilot
┌──────────────┐           ┌─────────────┐
│ ZooKeeper    │           │             │
├──────────────┤           │  LogPilot   │
│ Kafka Broker │    VS     │   Server    │
├──────────────┤           │             │
│ Schema Reg   │           │  (All-in-1) │
└──────────────┘           └─────────────┘
  ~4GB+ RAM                 ~256MB RAM
  복잡한 운영                단일 바이너리
```

### 🚀 주요 기능

#### 이벤트 스트리밍 엔진
- ✅ **이중 프로토콜 지원**: 고성능 gRPC (50051) 및 REST API (8080)
- ✅ **플러그형 저장소**: SQLite(내장) 또는 Append-only 파일 시스템 지원
- ✅ **Kafka 스타일 오프셋 추적**: 데이터 손실 없는 안정적인 소비를 위한 오프셋 관리
- ✅ **배치 수신**: JDBC 배치 연산을 통한 대량 이벤트 발행 최적화
- ✅ **스트림 탐색**: `seek`(시작/최신/특정 위치) 기능을 통한 이벤트 재생 및 건너뛰기

#### 클라우드 네이티브 및 관측 가능성
- 📊 **Prometheus 메트릭**: 로그 수신량, 에러율 및 지연 시간에 대한 메트릭 내장
- 🐳 **멀티 스테이지 Docker 빌드**: 최적화된 컨테이너 이미지 (~100MB)
- ☸️ **Kubernetes 지원**: 전체 매니페스트 제공 (Deployment, Service, Ingress, ConfigMap 등)
- 🔧 **Spring Actuator**: 상태 확인, 정보 엔드포인트 및 런타임 메트릭

#### 개발자 경험
- 🔌 **클라이언트 SDK**: 
    - [Java Client](logpilot-client/README.ko.md): **비동기 배치(Asynchronous Batching)** 지원
    - [Logback Appender](logpilot-logback/README.ko.md): Logback 연동 모듈
    - [Spring Boot Starter](logpilot-spring-boot-starter/README.ko.md): Spring Boot 자동 구성 스타터
- 📝 **Protobuf 정의**: 강력한 타입의 gRPC 계약
- 🎯 **유연한 설정**: Spring Profile을 통한 환경별 설정
- 🔄 **핫 리로드**: 프로필 기반의 프로토콜 전환 (REST 전용, gRPC 전용 또는 둘 다)

### 📐 아키텍처

```
┌──────────────────────────────────────────────────────────┐
│                     Client Applications                  │
│  (Java SDK, cURL, grpcurl, any HTTP/gRPC client)         │
└────────────┬─────────────────────────┬───────────────────┘
             │                         │
             ▼                         ▼
      ┌─────────────┐          ┌─────────────┐
      │  REST API   │          │  gRPC API   │
      │   :8080     │          │   :50051    │
      └──────┬──────┘          └──────┬──────┘
             │                        │
             └────────┬───────────────┘
                      ▼
             ┌─────────────────┐
             │   LogService    │
             │  (Core Logic)   │
             └────────┬────────┘
                      │
          ┌───────────┴───────────┐
          ▼                       ▼
   ┌─────────────┐        ┌─────────────┐
   │   SQLite    │   OR   │ File System │
   │  Storage    │        │  Storage    │
   └─────────────┘        └─────────────┘
```

**모듈 구조:**
- **logpilot-server**: REST/gRPC 엔드포인트, 메트릭, 설정
- **logpilot-core**: 비즈니스 로직, 저장소 추상화, 도메인 모델
- **logpilot-client**: 로그 생산자를 위한 Java SDK
- **logpilot-monitoring**: 관측 가능성 확장 기능

### 🏃 빠른 시작

#### 방식 1: Docker
Docker 이미지를 먼저 로컬에서 빌드해야 합니다. 제공된 스크립트를 사용하는 것이 가장 간편합니다.

**스크립트 사용 (빌드 및 실행 자동화):**
```bash
chmod +x docker-build-run.sh
./docker-build-run.sh
```

**수동 단계:**
```bash
# 1. 이미지 빌드
docker build -t logpilot:latest .

# 2. 컨테이너 실행
docker run -d \
  --name logpilot \
  -p 8080:8080 \
  -p 50051:50051 \
  -v $(pwd)/data:/data \
  logpilot:latest
```

**#### REST API 예시

**1. 단일 이벤트 발행**
```bash
curl -X POST http://localhost:8080/api/logs \
  -H 'Content-Type: application/json' \
  -d '{
    "channel": "orders",
    "level": "INFO",
    "message": "{\"orderId\": \"ORD-123\", \"status\": \"CREATED\"}",
    "timestamp": "2025-09-25T05:00:00"
  }'
```

**2. 배치 이벤트 발행**
```bash
curl -X POST http://localhost:8080/api/logs/batch \
  -H 'Content-Type: application/json' \
  -d '[
    { "channel": "orders", "level": "INFO", "message": "First event" },
    { "channel": "orders", "level": "INFO", "message": "Second event" }
  ]'
```

**3. 이벤트 소비 (Consume)**
```bash
# 소비자 오프셋을 사용한 이벤트 소비 (안정적 소비)
# consumerId를 제공하면 오프셋이 업데이트되어, 이미 처리한 이벤트를 중복해서 읽지 않습니다.
curl "http://localhost:8080/api/logs/orders?consumerId=inventory-service&limit=10"

# 오프셋 업데이트 없이 이벤트 미리보기 (Peek)
curl "http://localhost:8080/api/logs/orders?autoCommit=false&limit=5"
```

#### REST API 엔드포인트
- `POST /api/logs`: 단일 로그 전송
- `POST /api/logs/batch`: 배치 로그 전송
- `GET /api/logs`: 로그 조회
- `GET /api/logs/{channel}`: 채널별 로그 조회 (`consumerId` 파라미터로 오프셋 관리 가능)
  - 파라미터:
    - `limit` (기본값: 100)
    - `autoCommit` (기본값: true): false일 경우 오프셋을 업데이트하지 않습니다. "Peek & Commit" 패턴에 사용.
- `POST /api/logs/commit`: 수동으로 컨슈머 오프셋 커밋.
  - 본문: `{ "channel": "...", "consumerId": "...", "lastLogId": 123 }`
- `POST /api/logs/seek`: 컨슈머 오프셋 탐색 (Kafka-style Seek).
  - 본문: `{ "channel": "...", "consumerId": "...", "operation": "EARLIEST|LATEST|SPECIFIC", "logId": 123 }`

#### 신뢰성 (Reliability - Manual Ack)
데이터 손실을 방지하려면 "Fetch & Commit" 패턴을 사용하세요:
1. `autoCommit=false`로 로그를 조회합니다.
2. 로그를 성공적으로 처리합니다.
3. 처리된 가장 높은 `id`로 `/api/logs/commit`을 호출합니다.
이 방식은 처리 중 실패하더라도 다음 조회 시 동일한 로그를 다시 전달받을 수 있게 보장합니다.

#### 오프셋 관리 (Seek)
`/api/logs/seek` API를 사용하여 컨슈머의 위치를 수동으로 조절할 수 있습니다:
- **EARLIEST**: 처음부터 모든 로그를 다시 읽습니다.
- **LATEST**: 현재까지의 모든 로그를 건너뛰고 새로운 로그부터 수신합니다.
- **SPECIFIC**: 특정 로그 ID(또는 라인 번호)로 이동하여 해당 지점부터 다시 처리합니다.

#### LogEntry 필드 상세
- `channel` (String): **필수**. 로그의 카테고리나 출처 (예: 'payment-service').
- `level` (String): **필수**. 로그 레벨 (DEBUG, INFO, WARN, ERROR).
- `message` (String): **필수**. 실제 로그 내용.
- `timestamp` (String): 선택사항. ISO 8601 형식. 생략 시 서버 수신 시간으로 설정됨.
- `meta` (Object): 선택사항. 추가적인 Key-Value 메타데이터 (예: userId, requestId).

#### 에러 응답 (Error Response)
에러 발생 시 (4xx 또는 5xx), API는 다음과 같은 JSON 형식의 응답을 반환합니다:
```json
{
  "errorCode": "STORAGE_ERROR",
  "message": "데이터베이스 잠금으로 인해 로그 저장 실패",
  "timestamp": "2025-09-25T05:01:00"
}
```
- `errorCode` (String): 고유 에러 코드 (예: `INTERNAL_SERVER_ERROR`, `VALIDATION_ERROR`, `STORAGE_ERROR`).
- `message` (String): 설명 메시지.
- `timestamp` (String): 에러 발생 시간.

#### 방식 2: Kubernetes
*참고: Minikube, Docker Desktop K8S 등 로컬 클러스터가 구동 중이어야 합니다. 클러스터 설정 및 상세 배포 가이드는 [K8S.md](K8S.md)를 참조하세요.*

```bash
# 배포 스크립트 사용
chmod +x k8s-deploy.sh
./k8s-deploy.sh

# 또는 수동 배포
kubectl apply -f k8s/

# 로컬 접속을 위한 포트 포워딩
kubectl port-forward svc/logpilot 8080:8080 50051:50051

# Prometheus 메트릭 확인
curl http://localhost:8081/actuator/prometheus
```

### ⚙️ 설정 (Configuration)

환경 변수 또는 `application.yml`을 통해 설정할 수 있습니다:

| 변수명 (Variable) | 기본값 (Default) | 설명 (Description) |
|---|---|---|
| `LOGPILOT_PROTOCOL` | `all` | 프로토콜 모드: `rest`, `grpc`, 또는 `all` |
| `LOGPILOT_HTTP_PORT` | `8080` | REST API 포트 |
| `LOGPILOT_GRPC_PORT` | `50051` | gRPC 서버 포트 |
| `LOGPILOT_STORAGE_TYPE` | `sqlite` | 저장소 백엔드: `sqlite` 또는 `file` |
| `LOGPILOT_SQLITE_PATH` | `./data/logpilot.db` | SQLite 데이터베이스 경로 |
| `LOGPILOT_SQLITE_POOL_SIZE` | `10` | HikariCP 최대 풀 크기 |
| `LOGPILOT_RATE_LIMIT_CAPACITY` | `100` | Rate limit 버킷 용량 |
| `LOGPILOT_RATE_LIMIT_REFILL_TOKENS` | `100` | 리필 시 충전되는 토큰 수 |
| `LOGPILOT_RATE_LIMIT_REFILL_DURATION` | `60` | 리필 간격 (초) |
| `LOGPILOT_SQLITE_MIN_IDLE` | `2` | HikariCP 최소 유휴 연결 수 |
| `LOGPILOT_SQLITE_CONN_TIMEOUT` | `30000` | 연결 타임아웃 (ms) |
| `LOGPILOT_SQLITE_IDLE_TIMEOUT` | `600000` | 유휴 타임아웃 (ms) |
| `LOGPILOT_STORAGE_DIR` | `./data/logs` | 파일 저장소 디렉토리 |
| `LOGPILOT_MANAGEMENT_PORT` | `8081` | Actuator/메트릭 포트 |
| `LOGPILOT_API_KEY` | `logpilot-secret-key-123` | 서버 인증을 위한 API Key |

---

### 📡 API 레퍼런스

#### REST API (위의 예시 참조)
상세한 사용법은 [빠른 시작](#-빠른-시작) 섹션의 예시를 참고하세요.

#### gRPC API

#### gRPC API
[`logpilot.proto`](logpilot-server/src/main/proto/logpilot.proto) 파일을 참조하세요.

---

### 🧪 테스트 실행
```bash
./gradlew test
```

### 📄 라이선스
MIT 라이선스
