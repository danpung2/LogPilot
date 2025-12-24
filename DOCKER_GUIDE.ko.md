# LogPilot Docker 사용 가이드

LogPilot은 Docker Hub를 통해 경량 이벤트 스트리밍 브러커로 배포됩니다. 두 가지 실행 방식을 지원합니다.

## 📦 Docker 이미지

- **Standalone (`danpung2/logpilot-server:latest`)**: LogPilot 서버만 포함된 이미지입니다. 기존 인프라에 통합하기 좋습니다.
- **Full-Stack (`danpung2/logpilot-server:fullstack`)**: LogPilot + Prometheus + Grafana가 단일 이미지에 포함된 버전입니다. 테스트 및 데모에 최적화되어 있습니다.

---

## 📥 이미지 가져오기 (Pull)

실행 전 이미지를 미리 내려받으려면 다음 명령어를 사용하세요:

```bash
# Standalone 이미지 가져오기
docker pull danpung2/logpilot-server:latest

# Full-Stack 이미지 가져오기
docker pull danpung2/logpilot-server:fullstack
```

## 🚀 빠른 시작 (Standalone)

다음 명령어로 서버만 즉시 실행할 수 있습니다:

```bash
docker run -d -p 8080:8080 -p 50051:50051 danpung2/logpilot-server:latest
```

## 🚀 빠른 시작 (Full-Stack)

서버와 모니터링 스택(메트릭 + 대시보드)을 한 번에 실행합니다:

```bash
docker run -d \
  -p 8080:8080 -p 50051:50051 \
  -p 9090:9090 -p 3000:3000 \
  danpung2/logpilot-server:fullstack
```

- **REST API**: http://localhost:8080
- **Prometheus**: http://localhost:9090
- **Grafana**: http://localhost:3000 (대시보드 사전 탑재)

---

## 🛠 Docker Compose 활용

### 1. 단독 배포 (Standalone)

```bash
docker-compose up -d
```

### 2. 모니터링 스택 포함 배포

```bash
docker-compose -f docker-compose.monitoring.yml up -d
```

---

## ⚙️ 설정 프로퍼티

| 변수명 | 설명 | 기본값 |
|----------|-------------|---------|
| `LOGPILOT_API_KEY` | 보안 연동을 위한 API Key | `null` |
| `SERVER_PORT` | REST API 포트 | `8080` |
| `GRPC_PORT` | gRPC 포트 | `50051` |
| `DATA_PATH` | SQLite 저장소 경로 | `/app/data` |
