# LogPilot Monitoring Module

Prometheus와 Grafana를 사용한 LogPilot 모니터링 시스템

## 📋 개요

이 모듈은 LogPilot의 종합적인 모니터링 스택을 제공합니다:
- **Prometheus**: 메트릭 수집 및 저장
- **Grafana**: 시각화 대시보드

---

## 🚀 빠른 시작

### Prometheus 배포

```bash
# Prometheus 배포
kubectl apply -f logpilot-monitoring/k8s/prometheus/

# 배포 확인
kubectl get pods -n logpilot -l app=prometheus

# Port-forward로 접근
kubectl port-forward svc/prometheus 9090:9090 -n logpilot
```

Prometheus UI: http://localhost:9090

---

## 🔧 Prometheus 설정

### Scrape Jobs

1. **logpilot-all**: All-in-One 모드 Pod 메트릭
2. **logpilot-rest**: REST 전용 모드 메트릭
3. **logpilot-grpc**: gRPC 전용 모드 메트릭
4. **kubernetes-nodes**: 노드 메트릭
5. **kubernetes-pods**: 모든 Pod 메트릭
6. **kubernetes-services**: 서비스 헬스체크

### Recording Rules

성능 최적화를 위한 사전 계산된 메트릭:

**Performance Metrics**:
- `logpilot:http_request_duration_seconds:p50/p95/p99`
- `logpilot:grpc_request_duration_seconds:p50/p95/p99`
- `logpilot:log_processing_rate`
- `logpilot:http_request_rate`
- `logpilot:http_error_rate`
- `logpilot:grpc_request_rate`
- `logpilot:grpc_error_rate`

**Resource Metrics**:
- `logpilot:memory_usage_percent`
- `logpilot:cpu_usage_percent`
- `logpilot:jvm_heap_usage_percent`
- `logpilot:gc_pause_ratio`

**Business Metrics**:
- `logpilot:logs_total_by_level`
- `logpilot:logs_total_by_channel`
- `logpilot:error_log_rate`
- `logpilot:logs_rate_by_channel`

---

## 📊 메트릭 수집

LogPilot Pod에 다음 어노테이션이 필요합니다:

```yaml
annotations:
  prometheus.io/scrape: "true"
  prometheus.io/port: "8081"      # Management port
  prometheus.io/path: "/actuator/prometheus"
```

현재 LogPilot 서버는 Spring Boot Actuator를 통해 메트릭을 노출합니다:
- **Endpoint**: `/actuator/prometheus`
- **Port**: `8081` (management port)

---

## 🎯 주요 쿼리 예제

### HTTP 요청 P95 지연시간
```promql
logpilot:http_request_duration_seconds:p95
```

### gRPC 요청률 (req/sec)
```promql
logpilot:grpc_request_rate
```

### 로그 처리율 (logs/sec)
```promql
logpilot:log_processing_rate
```

### 에러율
```promql
logpilot:http_error_rate * 100
```

### 메모리 사용률
```promql
logpilot:memory_usage_percent
```

---

## 🔒 보안 설정

### RBAC
- **ServiceAccount**: `prometheus`
- **ClusterRole**: 메트릭 수집에 필요한 최소 권한
  - Nodes, Services, Endpoints, Pods: `get`, `list`, `watch`
  - ServiceMonitor, PodMonitor, PrometheusRule: `get`, `list`, `watch`

### 보안 컨텍스트
- **runAsNonRoot**: `true`
- **runAsUser**: `65534` (nobody)
- **fsGroup**: `65534`

---

## 💾 데이터 보존

- **Retention Time**: 15일
- **Retention Size**: 9GB
- **Storage**: 10Gi PVC

필요에 따라 `deployment.yaml`에서 조정 가능:
```yaml
args:
  - '--storage.tsdb.retention.time=15d'
  - '--storage.tsdb.retention.size=9GB'
```

---

## 🔄 고가용성 (HA)

- **Replicas**: 2
- **Anti-Affinity**: Pod를 다른 노드에 분산 배치
- **Rolling Update**: 무중단 업데이트

---

## 📈 리소스 설정

### Requests
- CPU: 500m
- Memory: 1Gi

### Limits
- CPU: 2000m
- Memory: 2Gi

---

## 🛠️ 관리 명령어

### Prometheus 재시작
```bash
kubectl rollout restart deployment/prometheus -n logpilot
```

### 로그 확인
```bash
kubectl logs -f deployment/prometheus -n logpilot
```

### ConfigMap 업데이트
```bash
# ConfigMap 수정 후
kubectl apply -f logpilot-monitoring/k8s/prometheus/configmap.yaml

# Prometheus에 설정 리로드 (web.enable-lifecycle 필요)
kubectl exec -n logpilot deployment/prometheus -- \
  curl -X POST http://localhost:9090/-/reload
```

### 스토리지 확인
```bash
kubectl get pvc -n logpilot
```

---
