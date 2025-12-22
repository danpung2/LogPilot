# LogPilot

**LogPilot은 Java 17 및 Spring Boot 3로 구축된 견실하고 프로덕션 준비가 된 로그 수집 시스템입니다.** 마이크로서비스 및 분산 시스템을 위해 설계되었으며, gRPC와 REST 프로토콜을 동시에 지원하며 엔터프라이즈급 로그 집계, 포괄적인 관측 가능성 및 클라우드 네이티브 배포 기능을 제공합니다.

[![License](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)
[![Java](https://img.shields.io/badge/Java-17-orange)](https://www.oracle.com/java/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.3.4-green)](https://spring.io/projects/spring-boot)

---

### LogPilot을 선택해야 하는 이유

ELK Stack과 같은 기존 로깅 솔루션은 강력하지만 운영 부담이 큽니다. LogPilot은 며칠이 아닌 몇 분 만에 실행할 수 있는 **가볍고 독립적인 대안**을 제공합니다.

```
기존 스택                   LogPilot
┌──────────────┐           ┌─────────────┐
│ Logstash     │           │             │
├──────────────┤           │  LogPilot   │
│ Elasticsearch│    VS     │   Server    │
├──────────────┤           │             │
│ Kibana       │           │  (All-in-1) │
└──────────────┘           └─────────────┘
  ~2GB RAM                  ~256MB RAM
  복잡한 설정                단일 바이너리
```

### 🚀 주요 기능

#### 프로덕션 레디 아키텍처
- ✅ **이중 프로토콜 지원**: 고성능 gRPC (50051) 및 REST API (8080)
- ✅ **플러그형 저장소**: 인터페이스 기반 설계로 SQLite(내장) 또는 파일 시스템 지원
- ✅ **배치 처리**: JDBC 배치 연산을 통한 대량 수신 최적화
- ✅ **소비자 오프셋 추적**: 안정적인 로그 소비를 위한 Kafka 스타일의 오프셋 관리
- ✅ **철저한 테스트**: 단위, 통합 및 성능 시나리오를 아우르는 18개의 테스트 파일

#### 클라우드 네이티브 및 관측 가능성
- 📊 **Prometheus 메트릭**: 로그 수신량, 에러율 및 지연 시간에 대한 메트릭 내장
- 🐳 **멀티 스테이지 Docker 빌드**: 최적화된 컨테이너 이미지 (~100MB)
- ☸️ **Kubernetes 지원**: 전체 매니페스트 제공 (Deployment, Service, Ingress, ConfigMap 등)
- 🔧 **Spring Actuator**: 상태 확인, 정보 엔드포인트 및 런타임 메트릭

#### 개발자 경험
- 🔌 **클라이언트 SDK**: 원활한 통합을 위한 Java 클라이언트 라이브러리
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

**1. 단일 로그 전송**
```bash
curl -X POST http://localhost:8080/api/logs \
  -H 'Content-Type: application/json' \
  -d '{
    "channel": "my-app",
    "level": "INFO",
    "message": "애플리케이션이 시작되었습니다.",
    "timestamp": "2025-09-25T05:00:00"
  }'
```

**2. 배치 로그 전송**
```bash
curl -X POST http://localhost:8080/api/logs/batch \
  -H 'Content-Type: application/json' \
  -d '[
    {
      "channel": "my-app",
      "level": "INFO",
      "message": "첫 번째 로그"
    },
    {
      "channel": "my-app",
      "level": "ERROR",
      "message": "두 번째 로그"
    }
  ]'
```

**3. 로그 조회**
```bash
# 전체 로그 조회
curl http://localhost:8080/api/logs

# 채널별 로그 조회
curl http://localhost:8080/api/logs/my-app?limit=50

# Consumer Offset을 사용한 로그 조회 (안정적 소비)
# consumerId를 제공하면 오프셋이 업데이트되어, 이미 읽은 로그를 중복해서 읽지 않습니다.
curl "http://localhost:8080/api/logs/my-app?consumerId=consumer-1&limit=10"
```

#### REST API 엔드포인트
- `POST /api/logs`: 단일 로그 전송
- `POST /api/logs/batch`: 배치 로그 전송
- `GET /api/logs`: 로그 조회
- `GET /api/logs/{channel}`: 채널별 로그 조회 (`consumerId` 파라미터로 오프셋 관리 가능)

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
| `LOGPILOT_SQLITE_MIN_IDLE` | `2` | HikariCP 최소 유휴 연결 수 |
| `LOGPILOT_SQLITE_CONN_TIMEOUT` | `30000` | 연결 타임아웃 (ms) |
| `LOGPILOT_SQLITE_IDLE_TIMEOUT` | `600000` | 유휴 타임아웃 (ms) |
| `LOGPILOT_STORAGE_DIR` | `./data/logs` | 파일 저장소 디렉토리 |
| `LOGPILOT_MANAGEMENT_PORT` | `8081` | Actuator/메트릭 포트 |

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
