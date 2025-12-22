# Grafana Default Dashboard Guide

LogPilot provides 4 default dashboards for Grafana. Each dashboard is designed to monitor different aspects of the system.

## 📊 Dashboard List

1. [LogPilot Overview](#1-logpilot-overview) - System-wide overview
2. [LogPilot Performance Metrics](#2-logpilot-performance-metrics) - Performance and response times
3. [LogPilot Business Metrics](#3-logpilot-business-metrics) - Business metrics and log analysis
4. [LogPilot Infrastructure Metrics](#4-logpilot-infrastructure-metrics) - Infrastructure resource monitoring

---

## 1. LogPilot Overview

**UID**: `logpilot-overview`
**Refresh Rate**: 30s
**Default Time Range**: Last 1 hour

### 📈 Panel Configuration (Total 8)

#### Row 1: Key Metrics (Stat Panels)
1. **Total Requests (HTTP + gRPC)**
   - Metric: `sum(logpilot:http_request_rate) + sum(logpilot:grpc_request_rate)`
   - Unit: req/sec
   - Thresholds:
     - Green (0-50 req/sec): Normal
     - Yellow (50-100 req/sec): Warning
     - Red (100+ req/sec): Critical
   - **Example**: `45.2 req/sec`

2. **Average Response Time** (P95)
   - Metric: `avg(logpilot:http_request_duration_seconds:p95)`
   - Unit: seconds
   - Thresholds:
     - Green (0-0.5s): Normal
     - Yellow (0.5-1s): Warning
     - Red (1s+): Critical
   - **Example**: `0.234s`

3. **Error Rate**
   - Metric: `(sum(logpilot:http_error_rate) + sum(logpilot:grpc_error_rate)) * 100`
   - Unit: percent
   - Thresholds:
     - Green (0-5%): Normal
     - Yellow (5-10%): Warning
     - Red (10%+): Critical
   - **Example**: `2.5%`

4. **Active Pods**
   - Metric: `count(up{namespace="logpilot",job=~"logpilot.*"} == 1)`
   - Unit: count
   - Thresholds:
     - Red (0): No Pods
     - Green (1+): Normal
   - **Example**: `2 Pods`

#### Row 2: Log Processing & Storage
5. **Log Processing Rate** (Graph)
   - Metric: `sum(logpilot:log_processing_rate)`
   - Unit: logs/sec
   - **Example**: Fluctuates between 100-500 logs/sec

6. **Storage Usage** (Gauge)
   - Metric: `logpilot_storage_size_bytes / logpilot_storage_capacity_bytes * 100`
   - Unit: percent
   - Thresholds:
     - Green (0-70%): Normal
     - Yellow (70-90%): Warning
     - Red (90%+): Critical
   - **Example**: `42%`

#### Row 3: Protocol Analysis
7. **Request Rate by Protocol** (Graph)
   - Metrics:
     - HTTP: `sum(logpilot:http_request_rate)`
     - gRPC: `sum(logpilot:grpc_request_rate)`
   - **Example**:
     - HTTP: 25 req/sec
     - gRPC: 20 req/sec

8. **Logs by Level** (Pie Chart)
   - Metric: `sum by (level) (logpilot:logs_total_by_level)`
   - **Example**:
     - INFO: 65%
     - DEBUG: 20%
     - WARN: 10%
     - ERROR: 5%

---

## 2. LogPilot Performance Metrics

**UID**: `logpilot-performance`
**Refresh Rate**: 30s
**Default Time Range**: Last 1 hour

### 📈 Panel Configuration (Total 9)

#### Row 1: Response Time Analysis
1. **HTTP Request Latency (P50, P95, P99)**
   - Metrics:
     - P50: `logpilot:http_request_duration_seconds:p50`
     - P95: `logpilot:http_request_duration_seconds:p95`
     - P99: `logpilot:http_request_duration_seconds:p99`
   - Unit: seconds
   - **Example**:
     - P50: 0.045s
     - P95: 0.234s
     - P99: 0.567s

2. **gRPC Request Latency (P50, P95, P99)**
   - Metrics:
     - P50: `logpilot:grpc_request_duration_seconds:p50`
     - P95: `logpilot:grpc_request_duration_seconds:p95`
     - P99: `logpilot:grpc_request_duration_seconds:p99`
   - **Example**:
     - P50: 0.032s
     - P95: 0.189s
     - P99: 0.423s

#### Row 2: JVM Memory & Resources
3. **JVM Memory Usage** (Graph)
   - Metrics:
     - Heap Used: `jvm_memory_used_bytes{area="heap"}`
     - Heap Max: `jvm_memory_max_bytes{area="heap"}`
     - Non-Heap Used: `jvm_memory_used_bytes{area="nonheap"}`
   - Unit: bytes
   - **Example**:
     - Heap Used: 256 MB
     - Heap Max: 512 MB
     - Non-Heap Used: 128 MB

4. **JVM Heap Usage %** (Gauge)
   - Metric: `avg(logpilot:jvm_heap_usage_percent)`
   - Unit: percent
   - Thresholds:
     - Green (0-70%): Normal
     - Yellow (70-85%): Warning
     - Red (85%+): Critical
   - **Example**: `58%`

5. **CPU Usage %** (Gauge)
   - Metric: `avg(logpilot:cpu_usage_percent)`
   - Unit: percent
   - Thresholds:
     - Green (0-70%): Normal
     - Yellow (70-85%): Warning
     - Red (85%+): Critical
   - **Example**: `45%`

#### Row 3: GC & Threads
6. **GC Pause Time**
   - Metric: `rate(jvm_gc_pause_seconds_sum{namespace="logpilot"}[5m])`
   - Unit: percentunit
   - **Example**: 0.5% (Spending 0.5% of total time in GC)

7. **Thread Count**
   - Metrics:
     - Current: `jvm_threads_current{namespace="logpilot"}`
     - Peak: `jvm_threads_peak{namespace="logpilot"}`
   - **Example**:
     - Current: 45 threads
     - Peak: 52 threads

#### Row 4: Request Rate Analysis
8. **HTTP Request Rate** (Per Pod)
   - Metric: `sum by (pod) (logpilot:http_request_rate)`
   - Unit: req/sec
   - **Example**:
     - logpilot-all-64c4c65cc8-2d5kz: 15 req/sec
     - logpilot-all-64c4c65cc8-c8ftx: 18 req/sec

9. **gRPC Request Rate** (Per Pod)
   - Metric: `sum by (pod) (logpilot:grpc_request_rate)`
   - Unit: req/sec
   - **Example**:
     - logpilot-all-64c4c65cc8-2d5kz: 12 req/sec
     - logpilot-all-64c4c65cc8-c8ftx: 10 req/sec

---

## 3. LogPilot Business Metrics

**UID**: `logpilot-business`
**Refresh Rate**: 30s
**Default Time Range**: Last 6 hours

### 📈 Panel Configuration (Total 8)

#### Row 1: Log Level Statistics
1. **Total Logs by Level** (Stat)
   - Metric: `sum by (level) (logpilot:logs_total_by_level)`
   - Colors:
     - ERROR: Red
     - WARN: Yellow
     - INFO: Green
     - DEBUG: Blue
   - **Example**:
     - INFO: 1,245,678
     - DEBUG: 456,789
     - WARN: 123,456
     - ERROR: 12,345

#### Row 2: Log Processing Analysis
2. **Log Processing Rate by Level** (Stacked Graph)
   - Metric: `sum by (level) (rate(logpilot_logs_processed_total{namespace="logpilot"}[5m]))`
   - **Example**:
     - INFO: 50 logs/sec
     - DEBUG: 20 logs/sec
     - WARN: 8 logs/sec
     - ERROR: 2 logs/sec

3. **Logs Distribution by Level** (Donut Chart)
   - Metric: `sum by (level) (logpilot:logs_total_by_level)`
   - **Example**:
     - INFO: 68%
     - DEBUG: 22%
     - WARN: 7%
     - ERROR: 3%

#### Row 3: Channel Analysis
4. **Top 10 Channels by Log Volume** (Bar Gauge)
   - Metric: `topk(10, sum by (channel) (logpilot:logs_total_by_channel))`
   - **Example**:
     - payment-service: 567,890
     - user-service: 432,109
     - order-service: 345,678
     - auth-service: 234,567
     - notification-service: 123,456

5. **Log Rate by Channel** (Graph)
   - Metric: `topk(10, sum by (channel) (logpilot:logs_rate_by_channel))`
   - **Example**: Log ingestion rate per channel over time

#### Row 4: Error & Storage Analysis
6. **ERROR Log Rate** (Alert Configured)
   - Metric: `sum(logpilot:error_log_rate)`
   - Unit: ERROR logs/sec
   - Alert: Average > 100 logs/sec for 5m
   - **Example**: 2.5 ERROR logs/sec

7. **Storage Distribution by Type** (Pie Chart)
   - Metric: `sum by (storage) (rate(logpilot_logs_processed_total{namespace="logpilot"}[5m]))`
   - **Example**:
     - SQLite: 100%

#### Row 5: Log Timeline
8. **Log Timeline Heatmap**
   - Metric: `sum(rate(logpilot_logs_processed_total{namespace="logpilot"}[1m]))`
   - Color: Spectral (128 steps)
   - **Description**: Visualizes log volume over time (brighter = more logs)

---

## 4. LogPilot Infrastructure Metrics

**UID**: `logpilot-infrastructure`
**Refresh Rate**: 30s
**Default Time Range**: Last 1 hour

### 📈 Panel Configuration (Total 11)

#### Row 1: Pod Status
1. **Pod Status**
   - Metric: `count(up{namespace="logpilot",job=~"logpilot.*"} == 1)`
   - Thresholds:
     - Red (0): All Pods Down
     - Green (1+): Normal
   - **Example**: `2 Running Pods`

2. **Pod Restarts (Last 1h)**
   - Metric: `sum(increase(kube_pod_container_status_restarts_total{namespace="logpilot"}[1h]))`
   - Thresholds:
     - Green (0): Normal
     - Yellow (1-4): Warning
     - Red (5+): Critical
   - **Example**: `0 Restarts`

3. **Container Memory Usage**
   - Metric: `avg(logpilot:memory_usage_percent)`
   - Unit: percent
   - Thresholds:
     - Green (0-70%): Normal
     - Yellow (70-85%): Warning
     - Red (85%+): Critical
   - **Example**: `52.3%`

4. **Container CPU Usage**
   - Metric: `avg(logpilot:cpu_usage_percent)`
   - Unit: percent
   - Thresholds:
     - Green (0-70%): Normal
     - Yellow (70-85%): Warning
     - Red (85%+): Critical
   - **Example**: `38.7%`

#### Row 2: Resource Usage
5. **Memory Usage by Pod**
   - Metric: `container_memory_working_set_bytes{namespace="logpilot",container="logpilot"}`
   - Unit: bytes
   - **Example**:
     - logpilot-all-64c4c65cc8-2d5kz: 268 MB
     - logpilot-all-64c4c65cc8-c8ftx: 275 MB

6. **CPU Usage by Pod**
   - Metric: `rate(container_cpu_usage_seconds_total{namespace="logpilot",container="logpilot"}[5m]) * 100`
   - Unit: percent
   - **Example**:
     - logpilot-all-64c4c65cc8-2d5kz: 35%
     - logpilot-all-64c4c65cc8-c8ftx: 42%

#### Row 3: Network & Disk I/O
7. **Network I/O**
   - Metrics:
     - RX: `rate(container_network_receive_bytes_total{namespace="logpilot"}[5m])`
     - TX: `rate(container_network_transmit_bytes_total{namespace="logpilot"}[5m])`
   - Unit: Bytes/sec
   - **Example**:
     - RX: 1.2 MB/sec
     - TX: -0.8 MB/sec (Negative for transmit)

8. **Disk I/O**
   - Metrics:
     - Read: `rate(container_fs_reads_bytes_total{namespace="logpilot"}[5m])`
     - Write: `rate(container_fs_writes_bytes_total{namespace="logpilot"}[5m])`
   - Unit: Bytes/sec
   - **Example**:
     - Read: 500 KB/sec
     - Write: 1.5 MB/sec

#### Row 4: Storage & Nodes
9. **PVC Usage** (Gauge)
   - Metric: `kubelet_volume_stats_used_bytes{namespace="logpilot"} / kubelet_volume_stats_capacity_bytes{namespace="logpilot"} * 100`
   - Unit: percent
   - Thresholds:
     - Green (0-70%): Normal
     - Yellow (70-90%): Warning
     - Red (90%+): Critical
   - **Example**:
     - logpilot-data: 35%

10. **Node Resource Distribution**
    - Metrics:
      - Memory: `sum by (node) (container_memory_working_set_bytes{namespace="logpilot"})`
      - CPU: `sum by (node) (rate(container_cpu_usage_seconds_total{namespace="logpilot"}[5m]))`
    - **Example**: Running on a single Minikube node

#### Row 5: Pod Uptime
11. **Pod Uptime** (Table)
    - Metric: `(time() - kube_pod_start_time{namespace="logpilot"})`
    - Unit: seconds
    - **Example**:
      | Pod Name | Uptime (seconds) |
      |----------|------------------|
      | logpilot-all-64c4c65cc8-2d5kz | 3,456 (57 mins) |
      | logpilot-all-64c4c65cc8-c8ftx | 3,234 (53 mins) |

---

## 🔧 Dashboard Deployment

### 1. Script Deployment (Recommended)

```bash
# Create ConfigMap
./logpilot-monitoring/scripts/create-dashboard-configmap.sh

# Deploy Grafana
kubectl apply -f logpilot-monitoring/k8s/grafana/
```

### 2. Manual Deployment

```bash
# Create Dashboard ConfigMap
kubectl create configmap grafana-dashboards \
  --from-file=logpilot-overview.json=logpilot-monitoring/dashboards/logpilot-overview.json \
  --from-file=logpilot-performance.json=logpilot-monitoring/dashboards/logpilot-performance.json \
  --from-file=logpilot-business.json=logpilot-monitoring/dashboards/logpilot-business.json \
  --from-file=logpilot-infrastructure.json=logpilot-monitoring/dashboards/logpilot-infrastructure.json \
  -n logpilot

# Label ConfigMap
kubectl label configmap grafana-dashboards \
  app=grafana \
  component=monitoring \
  -n logpilot

# Restart Grafana
kubectl rollout restart deployment grafana -n logpilot
```

---

## 📝 Metric Label Guide

### Common Recording Rule Patterns

LogPilot uses the following recording rules:

```yaml
# HTTP Request Rate
logpilot:http_request_rate

# gRPC Request Rate
logpilot:grpc_request_rate

# HTTP Response Time (Percentiles)
logpilot:http_request_duration_seconds:p50
logpilot:http_request_duration_seconds:p95
logpilot:http_request_duration_seconds:p99

# Log Processing Rate
logpilot:log_processing_rate

# Log Stats by Level
logpilot:logs_total_by_level{level="INFO|DEBUG|WARN|ERROR"}
logpilot:logs_rate_by_channel{channel="service-name"}

# Resource Usage
logpilot:cpu_usage_percent
logpilot:memory_usage_percent
logpilot:jvm_heap_usage_percent
```

---

## 🎯 Alert Rules

### Business Metrics Dashboard Alerts

**High ERROR Log Rate**
- Condition: Average ERROR log rate > 100 logs/sec for 5m
- Metric: `sum(logpilot:error_log_rate) > 100`
- Usage: Detect abnormal error spikes.

---

## 💡 Usage Tips

1. **Overview Dashboard**: Quick check of system-wide health.
2. **Performance Dashboard**: Analyze performance issues like latency or memory leaks.
3. **Business Dashboard**: Analyze log patterns and anomalies in specific channels/levels.
4. **Infrastructure Dashboard**: Diagnose infrastructure issues like resource exhaustion or Pod restarts.
