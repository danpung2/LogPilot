# LogPilot 모니터링 모듈

Prometheus와 Grafana를 포함한 LogPilot용 선택적 모니터링 스택입니다.

## 📋 개요

이 모듈은 LogPilot을 위한 완전한 관측 가능성(Observability) 스택을 제공합니다:
- **Prometheus**: 메트릭 수집 및 알림.
- **Grafana**: 성능 및 로그 모니터링을 위한 시각적 대시보드.

Core LogPilot 서버와 달리, 이 스택은 **선택 사항(Optional)**입니다. 기존의 중앙 집중식 모니터링 시스템이 없는 경우에만 배포하세요.

## 🚀 빠른 시작 (Quick Start)

### 1. 자동 설치 (권장)
제공된 스크립트를 사용하여 전체 스택(Prometheus + Grafana)을 배포할 수 있습니다:

```bash
# 프로젝트 루트에서 실행
./logpilot-monitoring/install-monitoring.sh
```

### 2. 수동 설치
상세한 수동 설치 단계 및 설정 옵션은 아래 문서를 참조하세요:

- [**Prometheus 설정**](docs/PROMETHEUS_SETUP.ko.md): 배포 및 수집 설정.
- [**Grafana 설정**](docs/GRAFANA_SETUP.ko.md): 대시보드 프로비저닝 및 데이터 소스 설정.
- [**Grafana 대시보드**](docs/GRAFANA_DASHBOARD.ko.md): 포함된 대시보드 상세 설명 (개요, 성능 등).
- [**트래픽 생성**](docs/TRAFFIC_GENERATION.ko.md): 메트릭 테스트를 위한 부하 생성 도구.

---

## 📊 주요 기능

### 사전 구성된 대시보드
이 스택에는 바로 사용할 수 있는 Grafana 대시보드가 포함되어 있습니다:
- **LogPilot Overview**: 고수준 상태, 수집률, 에러 카운트.
- **System Performance**: CPU, 메모리, GC, 네트워크 사용량.
- **Log Analytics**: 채널 및 레벨별 로그 분석.

### 주요 메트릭
| Metric | Type | Description |
|--------|------|-------------|
| `logpilot_logs_received_total` | Counter | 총 수집된 로그 (채널/레벨 태그 포함) |
| `http_requests_total` | Counter | REST API 요청 수 |
| `grpc_server_requests_received` | Counter | gRPC 요청 수 |
| `system_cpu_usage` | Gauge | JVM CPU 사용량 |
| `jvm_memory_used_bytes` | Gauge | Heap/Non-heap 메모리 사용량 |

---

## 🛠 관리 스크립트
`scripts/` 디렉토리에는 유틸리티 스크립트가 포함되어 있습니다:
- `create-dashboard-configmap.sh`: JSON 파일에서 대시보드 ConfigMap을 업데이트합니다.
- `wrk-load-test.sh`: `wrk`를 사용하여 부하를 생성합니다.

## 📂 디렉토리 구조
```
logpilot-monitoring/
├── docs/               # 상세 문서
├── k8s/                # Kubernetes 매니페스트
│   ├── prometheus/     # Prometheus Deployment/Service/ConfigMap
│   ├── grafana/        # Grafana Deployment/Service/ConfigMap
│   └── alertmanager/   # AlertManager (선택 사항)
├── dashboards/         # Raw JSON 대시보드 파일
└── scripts/            # 헬퍼 스크립트
```
