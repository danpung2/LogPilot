# Grafana 기본 대시보드 가이드

LogPilot은 Grafana에 4개의 기본 대시보드를 제공합니다. 각 대시보드는 시스템의 다양한 측면을 모니터링하도록 설계되었습니다.

## 📊 대시보드 목록

1. [LogPilot Overview](#1-logpilot-overview) - 시스템 전체 개요
2. [LogPilot Performance Metrics](#2-logpilot-performance-metrics) - 성능 및 응답 시간
3. [LogPilot Business Metrics](#3-logpilot-business-metrics) - 비즈니스 메트릭 및 로그 분석
4. [LogPilot Infrastructure Metrics](#4-logpilot-infrastructure-metrics) - 인프라 리소스 모니터링

---

## 1. LogPilot Overview

**UID**: `logpilot-overview`
**갱신 주기**: 30초
**기본 시간 범위**: 최근 1시간

### 📈 패널 구성 (총 8개)

#### Row 1: 핵심 지표 (Stat Panels)
1. **Total Requests (HTTP + gRPC)** - 전체 요청률
   - 메트릭: `sum(logpilot:http_request_rate) + sum(logpilot:grpc_request_rate)`
   - 단위: req/sec
   - 임계값:
     - 녹색 (0-50 req/sec): 정상
     - 노란색 (50-100 req/sec): 주의
     - 빨간색 (100+ req/sec): 경고
   - **예시 데이터**: `45.2 req/sec`

2. **Average Response Time** - 평균 응답 시간 (P95)
   - 메트릭: `avg(logpilot:http_request_duration_seconds:p95)`
   - 단위: seconds
   - 임계값:
     - 녹색 (0-0.5s): 정상
     - 노란색 (0.5-1s): 주의
     - 빨간색 (1s+): 경고
   - **예시 데이터**: `0.234s`

3. **Error Rate** - 오류율
   - 메트릭: `(sum(logpilot:http_error_rate) + sum(logpilot:grpc_error_rate)) * 100`
   - 단위: percent
   - 임계값:
     - 녹색 (0-5%): 정상
     - 노란색 (5-10%): 주의
     - 빨간색 (10%+): 경고
   - **예시 데이터**: `2.5%`

4. **Active Pods** - 활성 Pod 수
   - 메트릭: `count(up{namespace="logpilot",job=~"logpilot.*"} == 1)`
   - 단위: 개수
   - 임계값:
     - 빨간색 (0개): Pod 없음
     - 녹색 (1개 이상): 정상
   - **예시 데이터**: `2 Pods`

#### Row 2: 로그 처리 & 저장소
5. **Log Processing Rate** - 로그 처리율 (그래프)
   - 메트릭: `sum(logpilot:log_processing_rate)`
   - 단위: logs/sec
   - **예시 데이터**: 시간에 따라 100-500 logs/sec 변동

6. **Storage Usage** - 저장소 사용률 (게이지)
   - 메트릭: `logpilot_storage_size_bytes / logpilot_storage_capacity_bytes * 100`
   - 단위: percent
   - 임계값:
     - 녹색 (0-70%): 정상
     - 노란색 (70-90%): 주의
     - 빨간색 (90%+): 경고
   - **예시 데이터**: `42%`

#### Row 3: 프로토콜 분석
7. **Request Rate by Protocol** - 프로토콜별 요청률 (그래프)
   - 메트릭:
     - HTTP: `sum(logpilot:http_request_rate)`
     - gRPC: `sum(logpilot:grpc_request_rate)`
   - **예시 데이터**:
     - HTTP: 25 req/sec
     - gRPC: 20 req/sec

8. **Logs by Level** - 로그 레벨별 분포 (파이 차트)
   - 메트릭: `sum by (level) (logpilot:logs_total_by_level)`
   - **예시 데이터**:
     - INFO: 65%
     - DEBUG: 20%
     - WARN: 10%
     - ERROR: 5%

---

## 2. LogPilot Performance Metrics

**UID**: `logpilot-performance`
**갱신 주기**: 30초
**기본 시간 범위**: 최근 1시간

### 📈 패널 구성 (총 9개)

#### Row 1: 응답 시간 분석
1. **HTTP Request Latency (P50, P95, P99)** - HTTP 지연 시간
   - 메트릭:
     - P50: `logpilot:http_request_duration_seconds:p50`
     - P95: `logpilot:http_request_duration_seconds:p95`
     - P99: `logpilot:http_request_duration_seconds:p99`
   - 단위: seconds
   - **예시 데이터**:
     - P50: 0.045s
     - P95: 0.234s
     - P99: 0.567s

2. **gRPC Request Latency (P50, P95, P99)** - gRPC 지연 시간
   - 메트릭:
     - P50: `logpilot:grpc_request_duration_seconds:p50`
     - P95: `logpilot:grpc_request_duration_seconds:p95`
     - P99: `logpilot:grpc_request_duration_seconds:p99`
   - **예시 데이터**:
     - P50: 0.032s
     - P95: 0.189s
     - P99: 0.423s

#### Row 2: JVM 메모리 & 리소스
3. **JVM Memory Usage** - JVM 메모리 사용량 (그래프)
   - 메트릭:
     - Heap Used: `jvm_memory_used_bytes{area="heap"}`
     - Heap Max: `jvm_memory_max_bytes{area="heap"}`
     - Non-Heap Used: `jvm_memory_used_bytes{area="nonheap"}`
   - 단위: bytes
   - **예시 데이터**:
     - Heap Used: 256 MB
     - Heap Max: 512 MB
     - Non-Heap Used: 128 MB

4. **JVM Heap Usage %** - 힙 메모리 사용률 (게이지)
   - 메트릭: `avg(logpilot:jvm_heap_usage_percent)`
   - 단위: percent
   - 임계값:
     - 녹색 (0-70%): 정상
     - 노란색 (70-85%): 주의
     - 빨간색 (85%+): 경고
   - **예시 데이터**: `58%`

5. **CPU Usage %** - CPU 사용률 (게이지)
   - 메트릭: `avg(logpilot:cpu_usage_percent)`
   - 단위: percent
   - 임계값:
     - 녹색 (0-70%): 정상
     - 노란색 (70-85%): 주의
     - 빨간색 (85%+): 경고
   - **예시 데이터**: `45%`

#### Row 3: GC & 스레드
6. **GC Pause Time** - 가비지 컬렉션 일시정지 시간
   - 메트릭: `rate(jvm_gc_pause_seconds_sum{namespace="logpilot"}[5m])`
   - 단위: percentunit
   - **예시 데이터**: 0.5% (총 시간의 0.5%를 GC에 사용)

7. **Thread Count** - 스레드 수
   - 메트릭:
     - Current: `jvm_threads_current{namespace="logpilot"}`
     - Peak: `jvm_threads_peak{namespace="logpilot"}`
   - **예시 데이터**:
     - Current: 45 threads
     - Peak: 52 threads

#### Row 4: 요청률 분석
8. **HTTP Request Rate** - HTTP 요청률 (Pod별)
   - 메트릭: `sum by (pod) (logpilot:http_request_rate)`
   - 단위: req/sec
   - **예시 데이터**:
     - logpilot-all-64c4c65cc8-2d5kz: 15 req/sec
     - logpilot-all-64c4c65cc8-c8ftx: 18 req/sec

9. **gRPC Request Rate** - gRPC 요청률 (Pod별)
   - 메트릭: `sum by (pod) (logpilot:grpc_request_rate)`
   - 단위: req/sec
   - **예시 데이터**:
     - logpilot-all-64c4c65cc8-2d5kz: 12 req/sec
     - logpilot-all-64c4c65cc8-c8ftx: 10 req/sec

---

## 3. LogPilot Business Metrics

**UID**: `logpilot-business`
**갱신 주기**: 30초
**기본 시간 범위**: 최근 6시간

### 📈 패널 구성 (총 8개)

#### Row 1: 로그 레벨 통계
1. **Total Logs by Level** - 로그 레벨별 총계 (Stat)
   - 메트릭: `sum by (level) (logpilot:logs_total_by_level)`
   - 색상:
     - ERROR: 빨간색
     - WARN: 노란색
     - INFO: 녹색
     - DEBUG: 파란색
   - **예시 데이터**:
     - INFO: 1,245,678
     - DEBUG: 456,789
     - WARN: 123,456
     - ERROR: 12,345

#### Row 2: 로그 처리 분석
2. **Log Processing Rate by Level** - 레벨별 로그 처리율 (스택 그래프)
   - 메트릭: `sum by (level) (rate(logpilot_logs_processed_total{namespace="logpilot"}[5m]))`
   - **예시 데이터**:
     - INFO: 50 logs/sec
     - DEBUG: 20 logs/sec
     - WARN: 8 logs/sec
     - ERROR: 2 logs/sec

3. **Logs Distribution by Level** - 로그 레벨별 분포 (도넛 차트)
   - 메트릭: `sum by (level) (logpilot:logs_total_by_level)`
   - **예시 데이터**:
     - INFO: 68%
     - DEBUG: 22%
     - WARN: 7%
     - ERROR: 3%

#### Row 3: 채널 분석
4. **Top 10 Channels by Log Volume** - 로그량 상위 10개 채널 (바 게이지)
   - 메트릭: `topk(10, sum by (channel) (logpilot:logs_total_by_channel))`
   - **예시 데이터**:
     - payment-service: 567,890
     - user-service: 432,109
     - order-service: 345,678
     - auth-service: 234,567
     - notification-service: 123,456

5. **Log Rate by Channel** - 채널별 로그율 (그래프)
   - 메트릭: `topk(10, sum by (channel) (logpilot:logs_rate_by_channel))`
   - **예시 데이터**: 시간에 따라 각 채널별 로그 유입량 변화

#### Row 4: 오류 & 저장소 분석
6. **ERROR Log Rate** - ERROR 로그율 (알림 설정됨)
   - 메트릭: `sum(logpilot:error_log_rate)`
   - 단위: ERROR logs/sec
   - 알림: 5분간 평균 100 logs/sec 초과 시
   - **예시 데이터**: 2.5 ERROR logs/sec

7. **Storage Distribution by Type** - 저장소 유형별 분포 (파이 차트)
   - 메트릭: `sum by (storage) (rate(logpilot_logs_processed_total{namespace="logpilot"}[5m]))`
   - **예시 데이터**:
     - SQLite: 100%

#### Row 5: 로그 타임라인
8. **Log Timeline Heatmap** - 로그 타임라인 히트맵
   - 메트릭: `sum(rate(logpilot_logs_processed_total{namespace="logpilot"}[1m]))`
   - 색상: Spectral (128 단계)
   - **설명**: 시간대별 로그 유입량을 색상으로 표시 (밝을수록 많은 로그)

---

## 4. LogPilot Infrastructure Metrics

**UID**: `logpilot-infrastructure`
**갱신 주기**: 30초
**기본 시간 범위**: 최근 1시간

### 📈 패널 구성 (총 11개)

#### Row 1: Pod 상태
1. **Pod Status** - Pod 상태
   - 메트릭: `count(up{namespace="logpilot",job=~"logpilot.*"} == 1)`
   - 임계값:
     - 빨간색 (0개): 모든 Pod 다운
     - 녹색 (1개 이상): 정상
   - **예시 데이터**: `2 Running Pods`

2. **Pod Restarts (Last 1h)** - 최근 1시간 재시작 횟수
   - 메트릭: `sum(increase(kube_pod_container_status_restarts_total{namespace="logpilot"}[1h]))`
   - 임계값:
     - 녹색 (0회): 정상
     - 노란색 (1-4회): 주의
     - 빨간색 (5회+): 경고
   - **예시 데이터**: `0 Restarts`

3. **Container Memory Usage** - 컨테이너 메모리 사용률
   - 메트릭: `avg(logpilot:memory_usage_percent)`
   - 단위: percent
   - 임계값:
     - 녹색 (0-70%): 정상
     - 노란색 (70-85%): 주의
     - 빨간색 (85%+): 경고
   - **예시 데이터**: `52.3%`

4. **Container CPU Usage** - 컨테이너 CPU 사용률
   - 메트릭: `avg(logpilot:cpu_usage_percent)`
   - 단위: percent
   - 임계값:
     - 녹색 (0-70%): 정상
     - 노란색 (70-85%): 주의
     - 빨간색 (85%+): 경고
   - **예시 데이터**: `38.7%`

#### Row 2: 리소스 사용량
5. **Memory Usage by Pod** - Pod별 메모리 사용량
   - 메트릭: `container_memory_working_set_bytes{namespace="logpilot",container="logpilot"}`
   - 단위: bytes
   - **예시 데이터**:
     - logpilot-all-64c4c65cc8-2d5kz: 268 MB
     - logpilot-all-64c4c65cc8-c8ftx: 275 MB

6. **CPU Usage by Pod** - Pod별 CPU 사용률
   - 메트릭: `rate(container_cpu_usage_seconds_total{namespace="logpilot",container="logpilot"}[5m]) * 100`
   - 단위: percent
   - **예시 데이터**:
     - logpilot-all-64c4c65cc8-2d5kz: 35%
     - logpilot-all-64c4c65cc8-c8ftx: 42%

#### Row 3: 네트워크 & 디스크 I/O
7. **Network I/O** - 네트워크 입출력
   - 메트릭:
     - RX: `rate(container_network_receive_bytes_total{namespace="logpilot"}[5m])`
     - TX: `rate(container_network_transmit_bytes_total{namespace="logpilot"}[5m])`
   - 단위: Bytes/sec
   - **예시 데이터**:
     - RX: 1.2 MB/sec
     - TX: -0.8 MB/sec (음수는 송신)

8. **Disk I/O** - 디스크 입출력
   - 메트릭:
     - Read: `rate(container_fs_reads_bytes_total{namespace="logpilot"}[5m])`
     - Write: `rate(container_fs_writes_bytes_total{namespace="logpilot"}[5m])`
   - 단위: Bytes/sec
   - **예시 데이터**:
     - Read: 500 KB/sec
     - Write: 1.5 MB/sec

#### Row 4: 스토리지 & 노드
9. **PVC Usage** - PersistentVolumeClaim 사용률 (게이지)
   - 메트릭: `kubelet_volume_stats_used_bytes{namespace="logpilot"} / kubelet_volume_stats_capacity_bytes{namespace="logpilot"} * 100`
   - 단위: percent
   - 임계값:
     - 녹색 (0-70%): 정상
     - 노란색 (70-90%): 주의
     - 빨간색 (90%+): 경고
   - **예시 데이터**:
     - logpilot-data: 35%

10. **Node Resource Distribution** - 노드별 리소스 분배
    - 메트릭:
      - Memory: `sum by (node) (container_memory_working_set_bytes{namespace="logpilot"})`
      - CPU: `sum by (node) (rate(container_cpu_usage_seconds_total{namespace="logpilot"}[5m]))`
    - **예시 데이터**: Minikube 단일 노드에서 실행 중

#### Row 5: Pod 가동 시간
11. **Pod Uptime** - Pod 가동 시간 (테이블)
    - 메트릭: `(time() - kube_pod_start_time{namespace="logpilot"})`
    - 단위: seconds
    - **예시 데이터**:
      | Pod Name | Uptime (seconds) |
      |----------|------------------|
      | logpilot-all-64c4c65cc8-2d5kz | 3,456 (57분) |
      | logpilot-all-64c4c65cc8-c8ftx | 3,234 (53분) |

---

## 🔧 대시보드 배포 방법

### 1. 스크립트를 사용한 배포 (권장)

```bash
# ConfigMap 생성
./logpilot-monitoring/scripts/create-dashboard-configmap.sh

# Grafana 배포
kubectl apply -f logpilot-monitoring/k8s/grafana/
```

### 2. 수동 배포

```bash
# 대시보드 ConfigMap 생성
kubectl create configmap grafana-dashboards \
  --from-file=logpilot-overview.json=logpilot-monitoring/dashboards/logpilot-overview.json \
  --from-file=logpilot-performance.json=logpilot-monitoring/dashboards/logpilot-performance.json \
  --from-file=logpilot-business.json=logpilot-monitoring/dashboards/logpilot-business.json \
  --from-file=logpilot-infrastructure.json=logpilot-monitoring/dashboards/logpilot-infrastructure.json \
  -n logpilot

# 라벨 추가
kubectl label configmap grafana-dashboards \
  app=grafana \
  component=monitoring \
  -n logpilot

# Grafana 재시작
kubectl rollout restart deployment grafana -n logpilot
```

---

## 📝 메트릭 레이블 가이드

### 주요 Recording Rule 패턴

LogPilot은 다음과 같은 recording rules를 사용합니다:

```yaml
# HTTP 요청률
logpilot:http_request_rate

# gRPC 요청률
logpilot:grpc_request_rate

# HTTP 응답 시간 (백분위수)
logpilot:http_request_duration_seconds:p50
logpilot:http_request_duration_seconds:p95
logpilot:http_request_duration_seconds:p99

# 로그 처리율
logpilot:log_processing_rate

# 레벨별 로그 통계
logpilot:logs_total_by_level{level="INFO|DEBUG|WARN|ERROR"}
logpilot:logs_rate_by_channel{channel="service-name"}

# 리소스 사용률
logpilot:cpu_usage_percent
logpilot:memory_usage_percent
logpilot:jvm_heap_usage_percent
```

---

## 🎯 알림 규칙

### Business Metrics 대시보드의 알림

**High ERROR Log Rate**
- 조건: 5분간 평균 ERROR 로그율이 100 logs/sec 초과
- 메트릭: `sum(logpilot:error_log_rate) > 100`
- 용도: 급격한 에러 증가 감지

---

## 💡 사용 팁

1. **Overview 대시보드**: 시스템 전체 상태를 빠르게 파악할 때 사용
2. **Performance 대시보드**: 응답 시간 저하, 메모리 누수 등 성능 문제 분석
3. **Business 대시보드**: 로그 패턴 분석, 특정 채널/레벨의 이상 징후 감지
4. **Infrastructure 대시보드**: 리소스 부족, Pod 재시작 등 인프라 문제 진단

---
