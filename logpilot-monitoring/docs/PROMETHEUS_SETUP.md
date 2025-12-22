# Prometheus Deployment Guide

## 📋 Overview

Prometheus metric collection and storage system for LogPilot monitoring.

---

## 🚀 Deployment Methods

### Step 1: Deploy Prometheus Resources

```bash
# Deploy Prometheus resources
kubectl apply -f logpilot-monitoring/k8s/prometheus/

# Example output:
# serviceaccount/prometheus created
# clusterrole.rbac.authorization.k8s.io/prometheus created
# clusterrolebinding.rbac.authorization.k8s.io/prometheus created
# configmap/prometheus-config created
# persistentvolumeclaim/prometheus-data created
# deployment.apps/prometheus created
# service/prometheus created
```

---

### Step 2: Verify Deployment

```bash
# Check Pod status
kubectl get pods -n logpilot -l app=prometheus

# Expected output:
# NAME                          READY   STATUS    RESTARTS   AGE
# prometheus-xxxxxxxxxx-xxxxx   1/1     Running   0          2m

# Check Service
kubectl get svc -n logpilot -l app=prometheus

# Expected output:
# NAME         TYPE        CLUSTER-IP      EXTERNAL-IP   PORT(S)    AGE
# prometheus   ClusterIP   10.96.xxx.xxx   <none>        9090/TCP   2m

# Check PVC status
kubectl get pvc -n logpilot prometheus-data

# Expected output:
# NAME              STATUS   VOLUME                                     CAPACITY   ACCESS MODES   STORAGECLASS   AGE
# prometheus-data   Bound    pvc-xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx   10Gi       RWO            standard       2m
```

---

### Step 3: Access Prometheus UI

```bash
# Local access via Port-forward
kubectl port-forward svc/prometheus 9090:9090 -n logpilot

# Open in browser
open http://localhost:9090
```

---

### Step 4: Verify Metric Collection

In Prometheus UI:

1. Go to **Status → Targets** menu.
2. Verify the following targets are `UP`:
   - `kubernetes-apiservers`
   - `kubernetes-nodes`
   - `kubernetes-pods` (LogPilot pods)
   - `kubernetes-service-endpoints`

3. Test simple query in **Graph** menu:
   ```promql
   up{namespace="logpilot"}
   ```

---

## 📊 Collected Metrics

Key metrics collected by Prometheus:

### Kubernetes Metrics
- `up` - Target status (1: UP, 0: DOWN)
- `kube_pod_status_phase` - Pod status
- `kube_pod_container_status_restarts_total` - Container restart count
- `container_memory_working_set_bytes` - Container memory usage
- `container_cpu_usage_seconds_total` - Container CPU usage

### Application Metrics (LogPilot)
- `http_server_requests_seconds_*` - HTTP request metrics
- `grpc_server_*` - gRPC request metrics
- `jvm_memory_*` - JVM memory metrics
- `jvm_gc_*` - GC metrics
- `logpilot_logs_processed_total` - Log processing metrics

---

## 📈 Recording Rules

Pre-calculated rules configured in Prometheus:

### HTTP Metrics
```promql
logpilot:http_request_rate
logpilot:http_request_duration_seconds:p50
logpilot:http_request_duration_seconds:p95
logpilot:http_request_duration_seconds:p99
logpilot:http_error_rate
```

### gRPC Metrics
```promql
logpilot:grpc_request_rate
logpilot:grpc_request_duration_seconds:p50
logpilot:grpc_request_duration_seconds:p95
logpilot:grpc_request_duration_seconds:p99
logpilot:grpc_error_rate
```

### Business Metrics
```promql
logpilot:logs_total_by_level
logpilot:logs_total_by_channel
logpilot:log_processing_rate
logpilot:logs_rate_by_channel
logpilot:error_log_rate
```

### Resource Metrics
```promql
logpilot:memory_usage_percent
logpilot:cpu_usage_percent
logpilot:jvm_heap_usage_percent
logpilot:gc_pause_ratio
```

---

## 🔧 Configuration Changes

### Changing Scrape Interval

Edit `logpilot-monitoring/k8s/prometheus/configmap.yaml`:

```yaml
global:
  scrape_interval: 15s      # Default, change as needed
  evaluation_interval: 15s  # Default, change as needed
```

Apply:
```bash
kubectl apply -f logpilot-monitoring/k8s/prometheus/configmap.yaml
kubectl rollout restart deployment/prometheus -n logpilot
```

---

### Adding/Modifying Recording Rules

Edit `recording_rules.yaml` section in `logpilot-monitoring/k8s/prometheus/configmap.yaml`:

```yaml
groups:
  - name: your_custom_rules
    interval: 30s
    rules:
      - record: your:custom:metric
        expr: sum(rate(your_metric[5m]))
```

Apply:
```bash
kubectl apply -f logpilot-monitoring/k8s/prometheus/configmap.yaml
kubectl rollout restart deployment/prometheus -n logpilot
```

---

### Adding Scrape Config

Collect metrics from new services:

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

### Changing Data Retention

Edit `logpilot-monitoring/k8s/prometheus/deployment.yaml`:

```yaml
args:
  - '--config.file=/etc/prometheus/prometheus.yml'
  - '--storage.tsdb.path=/prometheus/'
  - '--storage.tsdb.retention.time=15d'  # Default 15 days
  - '--web.enable-lifecycle'
```

Apply:
```bash
kubectl apply -f logpilot-monitoring/k8s/prometheus/deployment.yaml
```

---

### Changing Storage Capacity

Edit `logpilot-monitoring/k8s/prometheus/pvc.yaml`:

```yaml
spec:
  resources:
    requests:
      storage: 20Gi  # Default 10Gi
```

⚠️ **Note**: PVC capacity is difficult to change after creation. Set an appropriate size beforehand.

---

## 🔍 Useful PromQL Queries

### Resource Usage
```promql
# Top 5 Pods by CPU usage
topk(5, rate(container_cpu_usage_seconds_total{namespace="logpilot"}[5m]) * 100)

# Top 5 Pods by Memory usage
topk(5, container_memory_working_set_bytes{namespace="logpilot"} / 1024 / 1024)

# JVM Heap usage
jvm_memory_used_bytes{namespace="logpilot",area="heap"} / jvm_memory_max_bytes{namespace="logpilot",area="heap"} * 100
```

### HTTP Requests
```promql
# HTTP Request Rate (req/sec)
rate(http_server_requests_seconds_count{namespace="logpilot"}[5m])

# HTTP P95 Response Time
histogram_quantile(0.95, rate(http_server_requests_seconds_bucket{namespace="logpilot"}[5m]))

# HTTP Error Rate (%)
sum(rate(http_server_requests_seconds_count{namespace="logpilot",status=~"5.."}[5m])) / sum(rate(http_server_requests_seconds_count{namespace="logpilot"}[5m])) * 100
```

### Log Metrics
```promql
# Log count by level
sum by (level) (logpilot_logs_processed_total{namespace="logpilot"})

# Log processing rate by channel
rate(logpilot_logs_processed_total{namespace="logpilot"}[5m])

# ERROR log rate
rate(logpilot_logs_processed_total{namespace="logpilot",level="ERROR"}[5m])
```

---

## 🔐 Security Configuration

### 1. RBAC Permissions Review

Prometheus currently has the following permissions:
- Read Pods, Services, Endpoints
- Read Nodes
- Read ConfigMaps

Adjust permissions in `logpilot-monitoring/k8s/prometheus/rbac.yaml` if necessary.

### 2. Add Basic Auth (Optional)

Add basic authentication to Prometheus:

```yaml
# Add web.yml to ConfigMap
web.yml: |
  basic_auth_users:
    admin: $2y$10$...  # bcrypt hash
```

### 3. Network Policy (Recommended)

Allow Prometheus access only from specific namespaces:

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

## 🎯 Performance Optimization

### 1. Adjust Scrape Interval

Increase scrape interval to reduce load if metric count is high:

```yaml
global:
  scrape_interval: 30s  # 15s -> 30s
```

### 2. Metric Filtering

Exclude unnecessary metrics:

```yaml
scrape_configs:
  - job_name: 'kubernetes-pods'
    metric_relabel_configs:
      - source_labels: [__name__]
        regex: 'go_.*|process_.*'  # Exclude Go runtime metrics
        action: drop
```

### 3. Optimize Data Retention

Reduce retention period if long-term storage is not needed:

```yaml
args:
  - '--storage.tsdb.retention.time=7d'  # 15d -> 7d
```
