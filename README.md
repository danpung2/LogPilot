# LogPilot

**LogPilot is a robust and production-ready log collection system** built with **Java 17** and **Spring Boot 3**. Designed for microservices and distributed systems, it provides enterprise-grade log aggregation with dual-protocol support (gRPC + REST), comprehensive observability, and cloud-native deployment capabilities.

[![License](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)
[![Java](https://img.shields.io/badge/Java-17-orange)](https://www.oracle.com/java/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.3.4-green)](https://spring.io/projects/spring-boot)
[![gRPC](https://img.shields.io/badge/gRPC-1.63.0-blue)](https://grpc.io/)
[![Kubernetes](https://img.shields.io/badge/Kubernetes-Ready-326CE5)](https://kubernetes.io/)

---

### Why LogPilot?

Traditional logging solutions like ELK Stack are powerful but come with significant operational overhead. LogPilot offers a **lightweight, self-contained alternative** that's ready to run in minutes, not days.

```
Traditional Stack          LogPilot
┌──────────────┐           ┌─────────────┐
│ Logstash     │           │             │
├──────────────┤           │  LogPilot   │
│ Elasticsearch│    VS     │   Server    │
├──────────────┤           │             │
│ Kibana       │           │  (All-in-1) │
└──────────────┘           └─────────────┘
  ~2GB RAM                  ~256MB RAM
  Complex Setup             Single Binary
```

### 🚀 Key Features (Currently Implemented)

#### Production-Ready Architecture
- ✅ **Dual Protocol Support**: High-performance gRPC (50051) + REST API (8080)
- ✅ **Pluggable Storage**: SQLite (embedded) or File System with interface-based design
- ✅ **Batch Processing**: Optimized bulk ingestion with JDBC batch operations
- ✅ **Consumer Offset Tracking**: Kafka-style offset management for reliable log consumption
- ✅ **Comprehensive Testing**: 18 test files covering unit, integration, and performance scenarios

#### Cloud-Native & Observable
- 📊 **Prometheus Metrics**: Built-in metrics for logs received, error rates, and latency
- 🐳 **Multi-Stage Docker Build**: Optimized container images (~100MB)
- ☸️ **Kubernetes Ready**: Complete manifests (Deployment, Service, Ingress, ConfigMap)
- 🔧 **Spring Actuator**: Health checks, info endpoints, and runtime metrics

#### Developer Experience
- 🔌 **Client SDK**: Java client library for seamless integration
- 📝 **Protobuf Definitions**: Strongly-typed gRPC contracts
- 🎯 **Flexible Configuration**: Environment-based configuration with Spring Profiles
- 🔄 **Hot Reload**: Profile-based protocol switching (REST-only, gRPC-only, or both)

### 📐 Architecture

```
┌──────────────────────────────────────────────────────────┐
│                     Client Applications                  │
│  (Java SDK, cURL, grpcurl, any HTTP/gRPC client)         │
└────────────┬─────────────────────────┬───────────────────┘
             │                         │
             ▼                         ▼
      ┌─────────────┐          ┌─────────────┐
      │  REST API   │          │  gRPC API   │
      │   :8080     │          │   :50051    │
      └──────┬──────┘          └──────┬──────┘
             │                        │
             └────────┬───────────────┘
                      ▼
             ┌─────────────────┐
             │   LogService    │
             │  (Core Logic)   │
             └────────┬────────┘
                      │
          ┌───────────┴───────────┐
          ▼                       ▼
   ┌─────────────┐        ┌─────────────┐
   │   SQLite    │   OR   │ File System │
   │  Storage    │        │  Storage    │
   └─────────────┘        └─────────────┘
```

**Module Structure:**
- **logpilot-server**: REST/gRPC endpoints, metrics, configuration
- **logpilot-core**: Business logic, storage abstraction, domain models
- **logpilot-client**: Java SDK for log producers
- **logpilot-monitoring**: Observability extensions

### 🏃 Quick Start

#### Option 1: Docker (Recommended)
You need to build the Docker image locally first, or use the provided script.

**Using script (Automatic build & run):**
```bash
chmod +x docker-build-run.sh
./docker-build-run.sh
```

**Manual steps:**
```bash
# 1. Build the image locally
docker build -t logpilot:latest .

# 2. Run the container
docker run -d \
  --name logpilot \
  -p 8080:8080 \
  -p 50051:50051 \
  -v $(pwd)/data:/data \
  logpilot:latest
```

#### REST API Examples

**1. Send a single log**
```bash
curl -X POST http://localhost:8080/api/logs \
  -H 'Content-Type: application/json' \
  -d '{
    "channel": "my-app",
    "level": "INFO",
    "message": "Application started successfully",
    "timestamp": "2025-09-25T05:00:00"
  }'
```

**2. Send batch logs**
```bash
curl -X POST http://localhost:8080/api/logs/batch \
  -H 'Content-Type: application/json' \
  -d '[
    {
      "channel": "my-app",
      "level": "INFO",
      "message": "First log entry"
    },
    {
      "channel": "my-app",
      "level": "ERROR",
      "message": "Second log entry"
    }
  ]'
```

**3. Retrieve logs**
```bash
# Get all logs
curl http://localhost:8080/api/logs

# Get logs by channel
curl http://localhost:8080/api/logs/my-app?limit=50

# Get logs by channel with Consumer Offset (Reliable consumption)
# Providing consumerId updates the offset, so you don't read the same logs twice.
curl "http://localhost:8080/api/logs/my-app?consumerId=consumer-1&limit=10"
```

#### Option 2: Kubernetes
*Note: Ensure your Kubernetes cluster (Minikube, Docker Desktop, etc.) is running. For a comprehensive guide on setting up a local cluster and troubleshooting, see [K8S.md](K8S.md).*

```bash
# Deploy using script (handles namespace and ordering)
chmod +x k8s-deploy.sh
./k8s-deploy.sh

# Or manual deploy
kubectl apply -f k8s/

# Port forward to access locally
kubectl port-forward svc/logpilot 8080:8080 50051:50051

# Check Prometheus metrics
curl http://localhost:8081/actuator/prometheus
```

#### Option 3: Build from Source
```bash
# Build all modules
./gradlew clean build

# Run server
java -jar logpilot-server/build/libs/logpilot-server-*.jar
```

### ⚙️ Configuration

Configure via environment variables or `application.yml`:

| Variable | Default | Description |
|----------|---------|-------------|
| `LOGPILOT_PROTOCOL` | `all` | Protocol mode: `rest`, `grpc`, or `all` |
| `LOGPILOT_HTTP_PORT` | `8080` | REST API port |
| `LOGPILOT_GRPC_PORT` | `50051` | gRPC server port |
| `LOGPILOT_STORAGE_TYPE` | `sqlite` | Storage backend: `sqlite` or `file` |
| `LOGPILOT_SQLITE_PATH` | `./data/logpilot.db` | SQLite database path |
| `LOGPILOT_STORAGE_DIR` | `./data/logs` | File storage directory |
| `LOGPILOT_MANAGEMENT_PORT` | `8081` | Actuator/metrics port |

### 📡 API Reference

#### REST API Endpoints
- `POST /api/logs`: Send a single log entry.
- `POST /api/logs/batch`: Send a batch of log entries.
- `GET /api/logs`: Retrieve all logs (with limit).
- `GET /api/logs/{channel}`: Retrieve logs for a specific channel. Supports `consumerId` param for offset tracking.

#### LogEntry Fields
- `channel` (String): **Required**. The category or source of the log (e.g., 'payment-service').
- `level` (String): **Required**. Log severity (DEBUG, INFO, WARN, ERROR).
- `message` (String): **Required**. The actual log content.
- `timestamp` (String): Optional. ISO 8601 format. Defaults to server time if omitted.
- `meta` (Object): Optional Key-Value pairs for extra context (e.g., userId, requestId).

#### Error Response
In case of an error (4xx or 5xx), the API returns a JSON response with the following structure:
```json
{
  "errorCode": "STORAGE_ERROR",
  "message": "Failed to store log entry due to database lock",
  "timestamp": "2025-09-25T05:01:00"
}
```
- `errorCode` (String): A unique error code (e.g., `INTERNAL_SERVER_ERROR`, `VALIDATION_ERROR`, `STORAGE_ERROR`).
- `message` (String): Descriptive error message.
- `timestamp` (String): Time when the error occurred.

#### gRPC API
See [`logpilot.proto`](logpilot-server/src/main/proto/logpilot.proto) for full service definition.

### 🧪 Testing
```bash
# Run all tests
./gradlew test
```

### 📄 License
MIT License
