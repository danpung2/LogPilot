# Grafana Deployment Guide

## 📋 Overview

Grafana dashboard system for LogPilot monitoring.

---

## 🚀 Deployment Methods

### Step 1: Create Dashboard ConfigMap

Create ConfigMaps from dashboard JSON files:

```bash
./logpilot-monitoring/scripts/create-dashboard-configmap.sh
```

Example output:
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

### Step 2: Deploy Grafana

```bash
# Deploy Grafana resources
kubectl apply -f logpilot-monitoring/k8s/grafana/

# Example output:
# configmap/grafana-datasource created
# configmap/grafana-dashboards-config created
# persistentvolumeclaim/grafana-data created
# deployment.apps/grafana created
# service/grafana created
```

---

### Step 3: Verify Deployment

```bash
# Check Pod status
kubectl get pods -n logpilot -l app=grafana

# Expected output:
# NAME                      READY   STATUS    RESTARTS   AGE
# grafana-xxxxxxxxx-xxxxx   1/1     Running   0          2m

# Check Service
kubectl get svc -n logpilot -l app=grafana

# Check ConfigMap
kubectl get configmap -n logpilot | grep grafana

# Expected output:
# grafana-datasource           1      2m
# grafana-dashboards           4      2m
# grafana-dashboards-config    1      2m
```

---

### Step 4: Access Grafana UI

```bash
# Local access via Port-forward
kubectl port-forward svc/grafana 3000:3000 -n logpilot

# Open in browser
open http://localhost:3000
```

**Default Credentials**:
- Username: `admin`
- Password: `admin`

⚠️ **Change the password after the first login!**

---

## 📊 Dashboard List

Dashboards automatically provisioned in Grafana:

### 1. LogPilot Overview
- **Path**: Dashboards → LogPilot → LogPilot Overview
- **Content**:
  - Total Requests (HTTP + gRPC)
  - Average Response Time (P95)
  - Error Rate
  - Active Pods
  - Log Processing Rate
  - Storage Usage
  - Request Rate by Protocol
  - Logs by Level

### 2. LogPilot Performance Metrics
- **Path**: Dashboards → LogPilot → LogPilot Performance Metrics
- **Content**:
  - HTTP Request Latency (P50, P95, P99)
  - gRPC Request Latency (P50, P95, P99)
  - JVM Memory Usage (Heap, Non-Heap)
  - JVM Heap Usage %
  - CPU Usage %
  - GC Pause Time
  - Thread Count
  - HTTP/gRPC Request Rate

### 3. LogPilot Business Metrics
- **Path**: Dashboards → LogPilot → LogPilot Business Metrics
- **Content**:
  - Total Logs by Level
  - Log Processing Rate by Level
  - Logs Distribution by Level (Pie Chart)
  - Top 10 Channels by Log Volume
  - Log Rate by Channel
  - ERROR Log Rate
  - Storage Distribution by Type
  - Log Timeline Heatmap

### 4. LogPilot Infrastructure Metrics
- **Path**: Dashboards → LogPilot → LogPilot Infrastructure Metrics
- **Content**:
  - Pod Status
  - Pod Restarts
  - Container Memory/CPU Usage
  - Network I/O
  - Disk I/O
  - PVC Usage
  - Node Resource Distribution
  - Pod Uptime Table

---

## 🔧 Configuration Changes

### Changing Prometheus Datasource

Edit `logpilot-monitoring/k8s/grafana/configmap-datasource.yaml`:

```yaml
data:
  datasource.yaml: |
    apiVersion: 1
    datasources:
      - name: Prometheus
        type: prometheus
        url: http://prometheus:9090  # Change if needed
        isDefault: true
```

Apply:
```bash
kubectl apply -f logpilot-monitoring/k8s/grafana/configmap-datasource.yaml
kubectl rollout restart deployment/grafana -n logpilot
```

---

### Updating Dashboards

After modifying dashboard JSON files (`logpilot-monitoring/dashboards/*.json`):

```bash
# Recreate ConfigMap
sh logpilot-monitoring/scripts/create-dashboard-configmap.sh

# Restart Grafana Pod (Reload dashboards)
kubectl rollout restart deployment/grafana -n logpilot
```

---

### Changing Admin Password

**Method 1: Change via Grafana UI (Recommended)**

After first login:
1. Left Menu → Administration → Users
2. Click admin user
3. Click "Change password"

**Method 2: Using Secret**

```bash
# Create Secret
kubectl create secret generic grafana-admin \
  --from-literal=admin-password=your-secure-password \
  -n logpilot

# Edit Deployment to reference Secret
kubectl edit deployment grafana -n logpilot

# Edit env section:
# - name: GF_SECURITY_ADMIN_PASSWORD
#   valueFrom:
#     secretKeyRef:
#       name: grafana-admin
#       key: admin-password
```

---

## 📈 Monitoring Metrics

Key metrics used in Grafana dashboards:

### HTTP Metrics
- `logpilot:http_request_rate` - HTTP Request Rate
- `logpilot:http_request_duration_seconds:p50/p95/p99` - HTTP Response Time
- `logpilot:http_error_rate` - HTTP Error Rate

### gRPC Metrics
- `logpilot:grpc_request_rate` - gRPC Request Rate
- `logpilot:grpc_request_duration_seconds:p50/p95/p99` - gRPC Response Time
- `logpilot:grpc_error_rate` - gRPC Error Rate

### Business Metrics
- `logpilot:logs_total_by_level` - Total logs by level
- `logpilot:logs_total_by_channel` - Total logs by channel
- `logpilot:log_processing_rate` - Log processing rate
- `logpilot:error_log_rate` - ERROR log rate

### Resource Metrics
- `logpilot:memory_usage_percent` - Memory usage %
- `logpilot:cpu_usage_percent` - CPU usage %
- `logpilot:jvm_heap_usage_percent` - JVM Heap usage %
- `logpilot:gc_pause_ratio` - GC pause ratio

---

## 🔐 Security Settings

### 1. Change Admin Password (Required)

After first login:
1. Left Menu → Administration → Users
2. Click admin user
3. Click "Change password"

### 2. Disable Anonymous Access

Add environment variable to Deployment:
```yaml
env:
  - name: GF_AUTH_ANONYMOUS_ENABLED
    value: "false"
```

### 3. Enable HTTPS (Production)

Recommended to use Ingress:
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
