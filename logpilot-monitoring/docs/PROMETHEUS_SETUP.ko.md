# Prometheus 배포 가이드

## 📋 개요

LogPilot 모니터링을 위한 Prometheus 메트릭 수집 및 저장 시스템입니다.

---

## 🚀 배포 방법

### Step 1: Prometheus 리소스 배포

```bash
# Prometheus 리소스 배포
kubectl apply -f logpilot-monitoring/k8s/prometheus/

# 출력 예시:
# serviceaccount/prometheus created
# clusterrole.rbac.authorization.k8s.io/prometheus created
# clusterrolebinding.rbac.authorization.k8s.io/prometheus created
# configmap/prometheus-config created
# persistentvolumeclaim/prometheus-data created
# deployment.apps/prometheus created
# service/prometheus created
```

---

### Step 2: 배포 확인

```bash
# Pod 상태 확인
kubectl get pods -n logpilot -l app=prometheus

# 예상 출력:
# NAME                          READY   STATUS    RESTARTS   AGE
# prometheus-xxxxxxxxxx-xxxxx   1/1     Running   0          2m

# Service 확인
kubectl get svc -n logpilot -l app=prometheus

# 예상 출력:
# NAME         TYPE        CLUSTER-IP      EXTERNAL-IP   PORT(S)    AGE
# prometheus   ClusterIP   10.96.xxx.xxx   <none>        9090/TCP   2m

# PVC 상태 확인
kubectl get pvc -n logpilot prometheus-data

# 예상 출력:
# NAME              STATUS   VOLUME                                     CAPACITY   ACCESS MODES   STORAGECLASS   AGE
# prometheus-data   Bound    pvc-xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx   10Gi       RWO            standard       2m
```

---

### Step 3: Prometheus UI 접근

```bash
# Port-forward로 로컬 접근
kubectl port-forward svc/prometheus 9090:9090 -n logpilot

# 브라우저에서 열기
open http://localhost:9090
```

---

### Step 4: 메트릭 수집 확인

Prometheus UI에서:

1. **Status → Targets** 메뉴로 이동
2. 다음 타겟들이 `UP` 상태인지 확인:
   - `kubernetes-apiservers`
   - `kubernetes-nodes`
   - `kubernetes-pods` (LogPilot pods)
   - `kubernetes-service-endpoints`

3. **Graph** 메뉴에서 간단한 쿼리 테스트:
   ```promql
   up{namespace="logpilot"}
   ```

---

## 📊 수집 메트릭

Prometheus가 수집하는 주요 메트릭:

### Kubernetes 메트릭
- `up` - 타겟 상태 (1: UP, 0: DOWN)
- `kube_pod_status_phase` - Pod 상태
- `kube_pod_container_status_restarts_total` - 컨테이너 재시작 횟수
- `container_memory_working_set_bytes` - 컨테이너 메모리 사용량
- `container_cpu_usage_seconds_total` - 컨테이너 CPU 사용량

### 애플리케이션 메트릭 (LogPilot)
- `http_server_requests_seconds_*` - HTTP 요청 메트릭
- `grpc_server_*` - gRPC 요청 메트릭
- `jvm_memory_*` - JVM 메모리 메트릭
- `jvm_gc_*` - GC 메트릭
- `logpilot_logs_processed_total` - 로그 처리 메트릭

---

## 📈 Recording Rules

Prometheus에 설정된 사전 계산 규칙:

### HTTP 메트릭
```promql
logpilot:http_request_rate
logpilot:http_request_duration_seconds:p50
logpilot:http_request_duration_seconds:p95
logpilot:http_request_duration_seconds:p99
logpilot:http_error_rate
```

### gRPC 메트릭
```promql
logpilot:grpc_request_rate
logpilot:grpc_request_duration_seconds:p50
logpilot:grpc_request_duration_seconds:p95
logpilot:grpc_request_duration_seconds:p99
logpilot:grpc_error_rate
```

### 비즈니스 메트릭
```promql
logpilot:logs_total_by_level
logpilot:logs_total_by_channel
logpilot:log_processing_rate
logpilot:logs_rate_by_channel
logpilot:error_log_rate
```

### 리소스 메트릭
```promql
logpilot:memory_usage_percent
logpilot:cpu_usage_percent
logpilot:jvm_heap_usage_percent
logpilot:gc_pause_ratio
```

---

## 🔧 설정 변경

### Scrape Interval 변경

`logpilot-monitoring/k8s/prometheus/configmap.yaml` 수정:

```yaml
global:
  scrape_interval: 15s      # 기본값, 변경 가능
  evaluation_interval: 15s  # 기본값, 변경 가능
```

적용:
```bash
kubectl apply -f logpilot-monitoring/k8s/prometheus/configmap.yaml
kubectl rollout restart deployment/prometheus -n logpilot
```

---

### Recording Rules 추가/수정

`logpilot-monitoring/k8s/prometheus/configmap.yaml`의 `recording_rules.yaml` 섹션 수정:

```yaml
groups:
  - name: your_custom_rules
    interval: 30s
    rules:
      - record: your:custom:metric
        expr: sum(rate(your_metric[5m]))
```

적용:
```bash
kubectl apply -f logpilot-monitoring/k8s/prometheus/configmap.yaml
kubectl rollout restart deployment/prometheus -n logpilot
```

---

### Scrape Config 추가

새로운 서비스에서 메트릭 수집:

```yaml
scrape_configs:
  - job_name: 'your-service'
    kubernetes_sd_configs:
      - role: pod
        namespaces:
          names:
            - your-namespace
    relabel_configs:
      - source_labels: [__meta_kubernetes_pod_label_app]
        action: keep
        regex: your-app
      - source_labels: [__meta_kubernetes_pod_annotation_prometheus_io_scrape]
        action: keep
        regex: true
      - source_labels: [__meta_kubernetes_pod_annotation_prometheus_io_port]
        action: replace
        target_label: __address__
        regex: ([^:]+)(?::\d+)?;(\d+)
        replacement: $1:$2
```

---

### 데이터 보관 기간 변경

`logpilot-monitoring/k8s/prometheus/deployment.yaml` 수정:

```yaml
args:
  - '--config.file=/etc/prometheus/prometheus.yml'
  - '--storage.tsdb.path=/prometheus/'
  - '--storage.tsdb.retention.time=15d'  # 기본 15일, 변경 가능
  - '--web.enable-lifecycle'
```

적용:
```bash
kubectl apply -f logpilot-monitoring/k8s/prometheus/deployment.yaml
```

---

### 스토리지 용량 변경

`logpilot-monitoring/k8s/prometheus/pvc.yaml` 수정:

```yaml
spec:
  resources:
    requests:
      storage: 20Gi  # 기본 10Gi, 변경 가능
```

⚠️ **주의**: PVC 용량은 생성 후 변경이 어렵습니다. 미리 적절한 크기로 설정하세요.

---

## 🔍 유용한 PromQL 쿼리

### 리소스 사용량
```promql
# CPU 사용률 상위 5개 Pod
topk(5, rate(container_cpu_usage_seconds_total{namespace="logpilot"}[5m]) * 100)

# 메모리 사용률 상위 5개 Pod
topk(5, container_memory_working_set_bytes{namespace="logpilot"} / 1024 / 1024)

# JVM Heap 사용률
jvm_memory_used_bytes{namespace="logpilot",area="heap"} / jvm_memory_max_bytes{namespace="logpilot",area="heap"} * 100
```

### HTTP 요청
```promql
# HTTP 요청률 (req/sec)
rate(http_server_requests_seconds_count{namespace="logpilot"}[5m])

# HTTP P95 응답 시간
histogram_quantile(0.95, rate(http_server_requests_seconds_bucket{namespace="logpilot"}[5m]))

# HTTP 에러율 (%)
sum(rate(http_server_requests_seconds_count{namespace="logpilot",status=~"5.."}[5m])) / sum(rate(http_server_requests_seconds_count{namespace="logpilot"}[5m])) * 100
```

### 로그 메트릭
```promql
# 레벨별 로그 수
sum by (level) (logpilot_logs_processed_total{namespace="logpilot"})

# 채널별 로그 처리율
rate(logpilot_logs_processed_total{namespace="logpilot"}[5m])

# ERROR 로그 비율
rate(logpilot_logs_processed_total{namespace="logpilot",level="ERROR"}[5m])
```

---

## 🔐 보안 설정

### 1. RBAC 권한 검토

현재 Prometheus는 다음 권한을 가집니다:
- Pods, Services, Endpoints 조회
- Nodes 조회
- ConfigMaps 조회

필요시 `logpilot-monitoring/k8s/prometheus/rbac.yaml`에서 권한 조정 가능합니다.

### 2. Basic Auth 추가 (선택)

Prometheus에 기본 인증 추가:

```yaml
# ConfigMap에 web.yml 추가
web.yml: |
  basic_auth_users:
    admin: $2y$10$...  # bcrypt 해시
```

### 3. Network Policy 설정 (권장)

특정 네임스페이스에서만 Prometheus 접근 허용:

```yaml
apiVersion: networking.k8s.io/v1
kind: NetworkPolicy
metadata:
  name: prometheus-network-policy
  namespace: logpilot
spec:
  podSelector:
    matchLabels:
      app: prometheus
  policyTypes:
    - Ingress
  ingress:
    - from:
        - namespaceSelector:
            matchLabels:
              name: logpilot
        - podSelector:
            matchLabels:
              app: grafana
      ports:
        - protocol: TCP
          port: 9090
```

---

## 🎯 성능 최적화

### 1. Scrape Interval 조정

메트릭 수가 많은 경우 scrape interval을 늘려 부하 감소:

```yaml
global:
  scrape_interval: 30s  # 기본 15s → 30s
```

### 2. 메트릭 필터링

불필요한 메트릭 제외:

```yaml
scrape_configs:
  - job_name: 'kubernetes-pods'
    metric_relabel_configs:
      - source_labels: [__name__]
        regex: 'go_.*|process_.*'  # Go runtime 메트릭 제외
        action: drop
```

### 3. 데이터 보관 기간 최적화

장기 보관이 필요 없는 경우 기간 단축:

```yaml
args:
  - '--storage.tsdb.retention.time=7d'  # 15d → 7d
```

---
