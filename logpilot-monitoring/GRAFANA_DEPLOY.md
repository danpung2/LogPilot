# Grafana 배포 가이드

## 📋 개요

LogPilot 모니터링을 위한 Grafana 대시보드 시스템입니다.

---

## 🚀 배포 방법

### Step 1: Dashboard ConfigMap 생성

대시보드 JSON 파일들을 ConfigMap으로 생성합니다:

```bash
./logpilot-monitoring/scripts/create-dashboard-configmap.sh
```

출력 예시:
```
Creating Grafana Dashboard ConfigMap...
configmap/grafana-dashboards created
configmap/grafana-dashboards labeled
✅ Dashboard ConfigMap created successfully!

Dashboards included:
  - logpilot-overview.json
  - logpilot-performance.json
  - logpilot-business.json
  - logpilot-infrastructure.json
```

---

### Step 2: Grafana 배포

```bash
# Grafana 리소스 배포
kubectl apply -f logpilot-monitoring/k8s/grafana/

# 출력 예시:
# configmap/grafana-datasource created
# configmap/grafana-dashboards-config created
# persistentvolumeclaim/grafana-data created
# deployment.apps/grafana created
# service/grafana created
```

---

### Step 3: 배포 확인

```bash
# Pod 상태 확인
kubectl get pods -n logpilot -l app=grafana

# 예상 출력:
# NAME                      READY   STATUS    RESTARTS   AGE
# grafana-xxxxxxxxx-xxxxx   1/1     Running   0          2m

# Service 확인
kubectl get svc -n logpilot -l app=grafana

# ConfigMap 확인
kubectl get configmap -n logpilot | grep grafana

# 예상 출력:
# grafana-datasource           1      2m
# grafana-dashboards           4      2m
# grafana-dashboards-config    1      2m
```

---

### Step 4: Grafana UI 접근

```bash
# Port-forward로 로컬 접근
kubectl port-forward svc/grafana 3000:3000 -n logpilot

# 브라우저에서 열기
open http://localhost:3000
```

**기본 로그인 정보**:
- Username: `admin`
- Password: `admin`

⚠️ **첫 로그인 후 비밀번호를 변경하세요!**

---

## 📊 대시보드 목록

Grafana에 자동으로 프로비저닝되는 대시보드:

### 1. LogPilot Overview
- **경로**: Dashboards → LogPilot → LogPilot Overview
- **내용**:
  - Total Requests (HTTP + gRPC)
  - Average Response Time (P95)
  - Error Rate
  - Active Pods
  - Log Processing Rate
  - Storage Usage
  - Request Rate by Protocol
  - Logs by Level

### 2. LogPilot Performance Metrics
- **경로**: Dashboards → LogPilot → LogPilot Performance Metrics
- **내용**:
  - HTTP Request Latency (P50, P95, P99)
  - gRPC Request Latency (P50, P95, P99)
  - JVM Memory Usage (Heap, Non-Heap)
  - JVM Heap Usage %
  - CPU Usage %
  - GC Pause Time
  - Thread Count
  - HTTP/gRPC Request Rate

### 3. LogPilot Business Metrics
- **경로**: Dashboards → LogPilot → LogPilot Business Metrics
- **내용**:
  - Total Logs by Level
  - Log Processing Rate by Level
  - Logs Distribution by Level (Pie Chart)
  - Top 10 Channels by Log Volume
  - Log Rate by Channel
  - ERROR Log Rate
  - Storage Distribution by Type
  - Log Timeline Heatmap

### 4. LogPilot Infrastructure Metrics
- **경로**: Dashboards → LogPilot → LogPilot Infrastructure Metrics
- **내용**:
  - Pod Status
  - Pod Restarts
  - Container Memory/CPU Usage
  - Network I/O
  - Disk I/O
  - PVC Usage
  - Node Resource Distribution
  - Pod Uptime Table

---

## 🔧 설정 변경

### Prometheus 데이터소스 변경

`k8s/grafana/configmap-datasource.yaml` 수정:

```yaml
data:
  datasource.yaml: |
    apiVersion: 1
    datasources:
      - name: Prometheus
        type: prometheus
        url: http://prometheus:9090  # 변경 가능
        isDefault: true
```

적용:
```bash
kubectl apply -f logpilot-monitoring/k8s/grafana/configmap-datasource.yaml
kubectl rollout restart deployment/grafana -n logpilot
```

---

### 대시보드 업데이트

대시보드 JSON 파일(`logpilot-monitoring/dashboards/*.json`) 수정 후:

```bash
# ConfigMap 재생성
sh logpilot-monitoring/scripts/create-dashboard-configmap.sh

# Grafana Pod 재시작 (대시보드 새로고침)
kubectl rollout restart deployment/grafana -n logpilot
```

---

### Admin 비밀번호 변경

**방법 1: Grafana UI에서 변경 (권장)**

첫 로그인 후:
1. 좌측 메뉴 → Administration → Users
2. admin 사용자 클릭
3. "Change password" 클릭

**방법 2: Secret 사용**

```bash
# Secret 생성
kubectl create secret generic grafana-admin \
  --from-literal=admin-password=your-secure-password \
  -n logpilot

# Deployment 수정하여 Secret 참조
kubectl edit deployment grafana -n logpilot

# env 섹션 수정:
# - name: GF_SECURITY_ADMIN_PASSWORD
#   valueFrom:
#     secretKeyRef:
#       name: grafana-admin
#       key: admin-password
```

---

## 📈 모니터링 메트릭

Grafana 대시보드에서 사용하는 주요 메트릭:

### HTTP 메트릭
- `logpilot:http_request_rate` - HTTP 요청률
- `logpilot:http_request_duration_seconds:p50/p95/p99` - HTTP 응답 시간
- `logpilot:http_error_rate` - HTTP 에러율

### gRPC 메트릭
- `logpilot:grpc_request_rate` - gRPC 요청률
- `logpilot:grpc_request_duration_seconds:p50/p95/p99` - gRPC 응답 시간
- `logpilot:grpc_error_rate` - gRPC 에러율

### 비즈니스 메트릭
- `logpilot:logs_total_by_level` - 레벨별 총 로그 수
- `logpilot:logs_total_by_channel` - 채널별 총 로그 수
- `logpilot:log_processing_rate` - 로그 처리율
- `logpilot:error_log_rate` - ERROR 로그 비율

### 리소스 메트릭
- `logpilot:memory_usage_percent` - 메모리 사용률
- `logpilot:cpu_usage_percent` - CPU 사용률
- `logpilot:jvm_heap_usage_percent` - JVM Heap 사용률
- `logpilot:gc_pause_ratio` - GC 일시정지 비율

---

## 🔐 보안 설정

### 1. Admin 비밀번호 변경 (필수)

첫 로그인 후:
1. 좌측 메뉴 → Administration → Users
2. admin 사용자 클릭
3. "Change password" 클릭

### 2. 익명 접근 비활성화

Deployment 환경 변수 추가:
```yaml
env:
  - name: GF_AUTH_ANONYMOUS_ENABLED
    value: "false"
```

### 3. HTTPS 활성화 (프로덕션)

Ingress 사용 권장:
```yaml
apiVersion: networking.k8s.io/v1
kind: Ingress
metadata:
  name: grafana-ingress
  namespace: logpilot
  annotations:
    cert-manager.io/cluster-issuer: letsencrypt-prod
spec:
  tls:
    - hosts:
        - grafana.example.com
      secretName: grafana-tls
  rules:
    - host: grafana.example.com
      http:
        paths:
          - path: /
            pathType: Prefix
            backend:
              service:
                name: grafana
                port:
                  number: 3000
```

---
