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
* 🚧 Advanced features (high availability, webhook delivery, retention policies, log search API, metrics integration) coming soon

---

## 🛠️ Tech Stack

* **Java 17**
* **Spring Boot 3.3.4**
* **gRPC**
* **GraalVM Native Image** (for fast startup & low memory footprint)

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

LogPilot supports production-ready Kubernetes deployment with multiple deployment modes:

### 🚀 Quick Start

Deploy LogPilot to your Kubernetes cluster in all-in-one mode:

```bash
# Deploy with default settings
./k8s-deploy.sh deploy

# Deploy with specific mode
./k8s-deploy.sh -m rest deploy      # REST-only mode
./k8s-deploy.sh -m grpc deploy      # gRPC-only mode
./k8s-deploy.sh -m split deploy     # Separate REST and gRPC services
```

### 📋 Deployment Modes

| Mode | Description | Use Case | Resources |
|------|-------------|----------|-----------|
| **all** | REST + gRPC in single pod | Development, small deployments | 2 replicas |
| **rest** | REST API only | Web applications, HTTP clients | 3 replicas |
| **grpc** | gRPC service only | High-performance clients | 2 replicas |
| **split** | Separate REST and gRPC services | Production, independent scaling | 3+2 replicas |

### 🔧 Advanced Deployment Options

```bash
# Custom image tag
./k8s-deploy.sh -t v1.0.0 deploy

# Custom namespace
./k8s-deploy.sh -n logpilot-prod deploy

# Skip building (use existing images)
./k8s-deploy.sh --no-build deploy

# Skip ingress setup
./k8s-deploy.sh --no-ingress deploy

# Use specific kubectl context
./k8s-deploy.sh -c my-cluster deploy
```

### 📁 Kubernetes Manifests

The `k8s/` directory contains all Kubernetes resources:

```
k8s/
├── namespace.yaml          # Namespaces
├── configmap.yaml         # Application configuration
├── deployment-all.yaml    # All-in-one deployment
├── deployment-rest.yaml   # REST-only deployment
├── deployment-grpc.yaml   # gRPC-only deployment
├── service.yaml           # Services (ClusterIP, LoadBalancer, NodePort)
├── ingress.yaml           # Ingress with TLS and Gateway API
├── monitoring.yaml        # Prometheus monitoring setup
└── kustomization.yaml     # Kustomize configuration
```

### 🌐 Access Methods

#### Via Ingress (Recommended)
```bash
# REST API
curl http://logpilot.local/actuator/health

# With custom domains (update ingress.yaml)
curl https://logpilot.example.com/api/logs
```

#### Via Port Forward
```bash
# REST API
kubectl port-forward svc/logpilot-all 8080:8080 -n logpilot
curl http://localhost:8080/actuator/health

# gRPC Service
kubectl port-forward svc/logpilot-all 50051:50051 -n logpilot
grpcurl -plaintext localhost:50051 list

# Management endpoints
kubectl port-forward svc/logpilot-all 8081:8081 -n logpilot
curl http://localhost:8081/actuator/prometheus
```

#### Via NodePort (Development)
```bash
# Access via node IP
curl http://<node-ip>:30080/actuator/health
grpcurl -plaintext <node-ip>:30051 list
```

### 📊 Monitoring & Observability

LogPilot includes comprehensive monitoring setup:

```bash
# Deploy with monitoring
kubectl apply -f k8s/monitoring.yaml

# View metrics
curl http://localhost:8081/actuator/prometheus

# Grafana dashboard included
# Import the dashboard from k8s/monitoring.yaml
```

**Metrics Available:**
- HTTP request rates and latencies
- gRPC service metrics
- JVM metrics (memory, CPU, GC)
- Custom application metrics
- Storage usage statistics

**Alerts Configured:**
- High memory/CPU usage (>80%)
- Pod down alerts
- High error rate (>10%)

### 🔄 Management Operations

```bash
# Check deployment status
./k8s-deploy.sh status

# View logs
kubectl logs -f deployment/logpilot-all -n logpilot

# Scale deployment
kubectl scale deployment logpilot-all --replicas=5 -n logpilot

# Rolling update
kubectl set image deployment/logpilot-all logpilot=logpilot:v2.0.0 -n logpilot

# Cleanup
./k8s-deploy.sh cleanup
```

### 🎛️ Configuration

Customize deployment via ConfigMaps:

```yaml
# Update k8s/configmap.yaml
logpilot:
  storage:
    type: postgresql  # Change to PostgreSQL
    postgresql:
      host: postgres-service
      database: logpilot
      username: logpilot
```

### 🛡️ Security Features

- **Non-root containers**: All containers run as non-root user
- **Resource limits**: Memory and CPU limits configured
- **Health checks**: Liveness, readiness, and startup probes
- **TLS support**: Ingress with automatic TLS certificates
- **RBAC ready**: Service accounts and RBAC configurations

### 🏗️ Production Considerations

1. **Storage**: Use persistent volumes for data storage
2. **Monitoring**: Deploy with Prometheus and Grafana
3. **Scaling**: Use HorizontalPodAutoscaler for auto-scaling
4. **Backup**: Regular backups of persistent data
5. **Security**: Enable TLS, configure network policies
6. **Resource requests/limits**: Properly sized for your workload

### 🔧 Troubleshooting

```bash
# Check pod status
kubectl get pods -n logpilot -o wide

# View pod logs
kubectl logs -f <pod-name> -n logpilot

# Describe pod for events
kubectl describe pod <pod-name> -n logpilot

# Check service endpoints
kubectl get endpoints -n logpilot

# Test connectivity
kubectl exec -it <pod-name> -n logpilot -- curl localhost:8080/actuator/health
```

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
