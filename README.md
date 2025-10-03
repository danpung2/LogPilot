# LogPilot

🛡️ **LogPilot** is a **robust and production-ready log collection system**.

It is designed for organizations and systems that require **scalable, reliable, and high-performance** log collection and analysis.

---

## ✨ Features

* ✅ REST API and gRPC support
* ✅ File-based and relational database (SQLite, PostgreSQL, MySQL) storage
* ✅ Spring Boot 3.3.4 backend with GraalVM Native Image support (optimized for Java 17)
* ✅ Horizontal scalability and high-availability design
* ✅ Consumer ID support for offset-based log streaming and continuation
* ✅ Production-ready Docker & Kubernetes deployment
* ✅ Prometheus & Grafana monitoring with 4 pre-built dashboards
* ✅ Production-ready metrics and alerting
* 🚧 Advanced features (webhook delivery, retention policies, log search API) coming soon

---

## 🛠️ Tech Stack

* **Java 17**
* **Spring Boot 3.3.4**
* **gRPC**
* **GraalVM Native Image** (for fast startup & low memory footprint)
* **Prometheus** 
* **Grafana** 

---

## 📄 Log Entry Format

When sending logs to **LogPilot**, use the following JSON structure:

```json
{
  "channel": "payment",
  "level": "ERROR",
  "message": "Payment failed",
  "meta": {
    "userId": "xyz789",
    "transactionId": "tx_123456"
  },
  "storage": "postgres"
}
```

### Field Details

| Field     | Type                                          | Required | Description                                                              |
| --------- | --------------------------------------------- | -------- | ------------------------------------------------------------------------ |
| `channel` | `string`                                      | ✅ Yes    | The source/category of the log (e.g., `"auth"`, `"payment"`, `"system"`) |
| `level`   | `string`                                      | ✅ Yes    | Severity level (`"DEBUG"`, `"INFO"`, `"WARN"`, `"ERROR"`)                |
| `message` | `string`                                      | ✅ Yes    | Human-readable log message                                               |
| `meta`    | `object` (key-value map)                      | ❌ No     | Optional metadata (user ID, IP, transaction ID, etc.)                    |
| `storage` | `"file"`, `"sqlite"` | ❌ No     | Determines how logs are stored. Defaults to `"file"` if omitted          |

---

## 📡 Send Log Example

### REST Version

```bash
curl -X POST http://localhost:8080/api/logs \
  -H "Content-Type: application/json" \
  -d '{
    "channel": "auth",
    "level": "INFO",
    "message": "User logged in",
    "meta": { "userId": "abc123", "ip": "192.168.0.1" },
    "storage": "sqlite"
  }'
```

### gRPC Version

```java
// Example Java client using gRPC
LogServiceGrpc.LogServiceBlockingStub client = 
    LogServiceGrpc.newBlockingStub(channel);

LogRequest request = LogRequest.newBuilder()
    .setChannel("auth")
    .setLevel("INFO")
    .setMessage("User logged in")
    .putMeta("userId", "abc123")
    .putMeta("ip", "192.168.0.1")
    .setStorage("sqlite")
    .build();

LogResponse response = client.sendLog(request);
System.out.println("✅ Log sent: " + response.getStatus());
```

---

## 🐳 Docker Deployment

LogPilot provides multiple Docker deployment options to suit different needs:

### 🚀 All-in-One (REST + gRPC)

Run both REST API and gRPC services in a single container:

```bash
# Build and run (default)
./docker-build-run.sh

# Other commands
./docker-build-run.sh build      # Build image only
./docker-build-run.sh run        # Run container only
./docker-build-run.sh stop       # Stop container
./docker-build-run.sh clean      # Clean up resources
./docker-build-run.sh logs       # View logs
```

**Exposed Ports:**
- REST API: `http://localhost:8080`
- gRPC Service: `localhost:50051`
- Health Check: `http://localhost:8080/actuator/health`

### 🌐 REST-Only Mode

Run only the REST API service:

```bash
# Build and run REST-only
./docker-rest.sh

# Test REST endpoints
./docker-rest.sh test
```

**Features:**
- Lightweight container with only REST API
- Port: `8080`
- Spring Profile: `rest`
- Example API calls included in script

### ⚡ gRPC-Only Mode

Run only the gRPC service:

```bash
# Build and run gRPC-only
./docker-grpc.sh

# Test gRPC service
./docker-grpc.sh test
```

**Features:**
- Optimized container with only gRPC service
- Port: `50051`
- Spring Profile: `grpc`
- Includes grpcurl for testing

### 🔧 Docker Configuration

Each deployment mode uses specific Dockerfiles:

| Mode | Dockerfile | Script | Ports | Profile |
|------|------------|--------|-------|---------|
| All-in-One | `Dockerfile` | `docker-build-run.sh` | 8080, 50051 | `all` |
| REST-Only | `Dockerfile.rest` | `docker-rest.sh` | 8080 | `rest` |
| gRPC-Only | `Dockerfile.grpc` | `docker-grpc.sh` | 50051 | `grpc` |

### 🧪 Testing Your Deployment

#### REST API Testing
```bash
# Health check
curl http://localhost:8080/actuator/health

# Send log
curl -X POST http://localhost:8080/api/logs \
  -H "Content-Type: application/json" \
  -d '{"channel":"test","level":"INFO","message":"Hello from Docker"}'

# Get logs
curl http://localhost:8080/api/logs?channel=test&limit=10
```

#### gRPC Testing
```bash
# Install grpcurl (if not already installed)
brew install grpcurl  # macOS
# or
apt-get install grpcurl  # Linux

# Health check
grpcurl -plaintext localhost:50051 grpc.health.v1.Health/Check

# List available services
grpcurl -plaintext localhost:50051 list
```

### 🛠️ Environment Variables

You can customize the deployment using environment variables:

```bash
# Port configuration
docker run -e LOGPILOT_HTTP_PORT=8080 \
           -e LOGPILOT_GRPC_PORT=50051 \
           -e LOGPILOT_PROTOCOL=all \
           logpilot:latest

# Storage configuration
docker run -e LOGPILOT_STORAGE_TYPE=sqlite \
           -e LOGPILOT_SQLITE_PATH=/data/logpilot.db \
           -v /host/data:/data \
           logpilot:latest
```

---

## ☸️ Kubernetes Deployment

LogPilot supports production-ready Kubernetes deployment with monitoring.

### 🚀 Quick Start (Minikube)

Deploy LogPilot to minikube in all-in-one mode:

```bash
# Deploy LogPilot (automatically starts minikube, builds image, deploys)
./k8s-deploy.sh
```

**What it does:**
- Starts minikube if not running
- Builds Docker image and loads to minikube
- Deploys namespace, ConfigMap, Deployment, Service
- Waits for pods to be ready
- Shows access URLs

**Access the service:**
```bash
# Get service URL
minikube service logpilot-nodeport -n logpilot --url

# Or use the helper script
./k8s-port-forward.sh
```

### 📊 Monitoring Setup (Prometheus & Grafana)

LogPilot includes comprehensive monitoring with Prometheus and Grafana.

#### Deploy Monitoring Stack

```bash
# 1. Deploy Prometheus
kubectl apply -f logpilot-monitoring/k8s/prometheus/

# 2. Create Grafana dashboards ConfigMap
./logpilot-monitoring/scripts/create-dashboard-configmap.sh

# 3. Deploy Grafana
kubectl apply -f logpilot-monitoring/k8s/grafana/

# 4. Wait for pods to be ready
kubectl wait --for=condition=ready pod -l app=prometheus -n logpilot --timeout=60s
kubectl wait --for=condition=ready pod -l app=grafana -n logpilot --timeout=60s
```

#### Access Grafana

```bash
# Port forward to Grafana
kubectl port-forward svc/grafana -n logpilot 3000:3000

# Open browser
open http://localhost:3000

# Default credentials
# Username: admin
# Password: admin
```

#### Available Dashboards

LogPilot provides 4 pre-configured Grafana dashboards:

1. **LogPilot Overview** - System overview with request rates, error rates, active pods
2. **LogPilot Performance** - HTTP/gRPC latency, JVM metrics, GC stats
3. **LogPilot Business** - Log volume by level/channel, error rates
4. **LogPilot Infrastructure** - Container resources, network I/O, disk usage

See [GRAFANA_DEFAULT_DASHBOARD.md](./GRAFANA_DEFAULT_DASHBOARD.md) for detailed dashboard documentation.

#### Generate Test Traffic

```bash
# Generate HTTP and gRPC traffic for testing
./logpilot-monitoring/scripts/wrk-load-test.sh
```

See [logpilot-monitoring/GENERATE_TRAFFIC.md](./logpilot-monitoring/GENERATE_TRAFFIC.md) for more options.

### 🔧 Helper Scripts

```bash
# Port forwarding utilities
./k8s-port-forward.sh          # Forward all LogPilot ports
./k8s-tunnel.sh               # Create minikube tunnel

# Prometheus standalone deployment
./deploy-prometheus.sh        # Deploy only Prometheus
```

### 🌐 Access Methods

#### Via Port Forward (Recommended for Development)
```bash
# LogPilot REST API
kubectl port-forward svc/logpilot-nodeport 8080:8080 -n logpilot
curl http://localhost:8080/actuator/health

# LogPilot gRPC Service
kubectl port-forward svc/logpilot-nodeport 50051:50051 -n logpilot
grpcurl -plaintext localhost:50051 list

# Prometheus
kubectl port-forward svc/prometheus -n logpilot 9090:9090
open http://localhost:9090

# Grafana
kubectl port-forward svc/grafana -n logpilot 3000:3000
open http://localhost:3000
```

#### Via NodePort (Minikube)
```bash
# Get service URL
minikube service logpilot-nodeport -n logpilot --url

# Access via browser or curl
curl $(minikube service logpilot-nodeport -n logpilot --url)/actuator/health
```

#### Via Ingress (Production)
```bash
# REST API
curl http://logpilot.local/actuator/health

# With custom domains (update ingress.yaml)
curl https://logpilot.example.com/api/logs
```

### 📊 Metrics & Alerts

**Prometheus Metrics Available:**
- HTTP request rates and latencies (P50, P95, P99)
- gRPC service metrics
- JVM metrics (memory, CPU, GC)
- Container resource metrics (via cAdvisor)
- Custom application metrics

**Pre-configured Alerts:**
- High memory/CPU usage (>80%)
- Pod down alerts
- High error rate (>10%)

**Recording Rules:**
- `logpilot:http_request_rate`
- `logpilot:grpc_request_rate`
- `logpilot:http_request_duration_seconds:p95`
- `logpilot:cpu_usage_percent`
- `logpilot:memory_usage_percent`
- `logpilot:jvm_heap_usage_percent`

See [logpilot-monitoring/PROMETHEUS_DEPLOY.md](./logpilot-monitoring/PROMETHEUS_DEPLOY.md) for details.

### 🔄 Management Operations

```bash
# Check deployment status
kubectl get pods -n logpilot -o wide
kubectl get svc -n logpilot

# View logs
kubectl logs -f deployment/logpilot-all -n logpilot
kubectl logs -f deployment/prometheus -n logpilot
kubectl logs -f deployment/grafana -n logpilot

# Scale deployment
kubectl scale deployment logpilot-all --replicas=5 -n logpilot

# Delete all resources
kubectl delete namespace logpilot
```

### 🛡️ Security Features

- **Non-root containers**: All containers run as non-root user (uid 472 for Grafana, appuser for LogPilot)
- **Resource limits**: Memory and CPU limits configured
- **Health checks**: Liveness, readiness, and startup probes
- **RBAC**: Prometheus uses ServiceAccount with minimal permissions
- **Persistent storage**: Data persisted in PVCs

### 🏗️ Production Deployment

For production deployments (non-Minikube), manually apply the resources:

```bash
# 1. Create namespace
kubectl apply -f k8s/namespace.yaml

# 2. Apply ConfigMaps
kubectl apply -f k8s/configmap.yaml

# 3. Deploy application
kubectl apply -f k8s/deployment-all.yaml
kubectl apply -f k8s/service.yaml

# 4. Deploy monitoring
kubectl apply -f logpilot-monitoring/k8s/prometheus/
./logpilot-monitoring/scripts/create-dashboard-configmap.sh
kubectl apply -f logpilot-monitoring/k8s/grafana/

# 5. Apply ingress (optional)
kubectl apply -f k8s/ingress.yaml
```

**Production Considerations:**
1. **Storage**: Use production-grade StorageClass (not `standard`)
2. **Monitoring**: Configure AlertManager for notifications
3. **Scaling**: Use HorizontalPodAutoscaler
4. **Backup**: Regular backups of PVCs
5. **Security**: Enable TLS, configure NetworkPolicies
6. **Resource sizing**: Adjust requests/limits based on workload

---

## 📦 Integration

* Use any HTTP client (Axios, OkHttp, Fetch, etc.) for REST
* Official gRPC client libraries are available for Java, Node.js, Go, and Python

---

## 📜 License

MIT License

Copyright (c) 2025 @danpung2

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

\[Full MIT License text remains the same]
