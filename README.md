# LogPilot

**LogPilot is a lightweight, cloud-native Event Streaming Broker** built with **Java 17** and **Spring Boot 3**. Inspired by **Apache Kafka**, it provides a simplified yet powerful message delivery system with dual-protocol support (gRPC + REST), persistent storage, and reliable consumer offset management.

> [!WARNING]
> **Production Safety Warning**: The default `LOGPILOT_API_KEY` is set to an example value. **You MUST change this** before deploying to any shared or production environment to prevent unauthorized access.
>
> **Compatibility Note**: This project strictly requires **Java 17+** and **Spring Boot 3.x**. Older versions (Java 8/11, Spring Boot 2.x) are **NOT supported**.

[![License](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)
[![Java](https://img.shields.io/badge/Java-17-orange)](https://www.oracle.com/java/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.3.4-green)](https://spring.io/projects/spring-boot)
[![gRPC](https://img.shields.io/badge/gRPC-1.63.0-blue)](https://grpc.io/)
[![Kubernetes](https://img.shields.io/badge/Kubernetes-Ready-326CE5)](https://kubernetes.io/)

---

### Why LogPilot?

Traditional event streaming platforms like **Apache Kafka** are powerful but extremely heavy to operate for small-to-medium projects. LogPilot offers a **lightweight, self-contained alternative** that's ready to run in minutes, not days. It's perfect for microservice event notification, lightweight audit logs, and distributed event pipelines.

```
Traditional Kafka            LogPilot
┌──────────────┐           ┌─────────────┐
│ ZooKeeper    │           │             │
├──────────────┤           │  LogPilot   │
│ Kafka Broker │    VS     │   Server    │
├──────────────┤           │             │
│ Schema Reg   │           │  (All-in-1) │
└──────────────┘           └─────────────┘
  ~4GB+ RAM                  ~256MB RAM
  Complex Ops                Single Binary
```

### 🚀 Key Features (Currently Implemented)

#### Event Streaming Engine
- ✅ **Dual Protocol Support**: High-performance gRPC (50051) + REST API (8080)
- ✅ **Pluggable Storage**: SQLite (embedded) or Append-only File System
- ✅ **Kafka-style Offset Tracking**: Reliable consumer offset management to ensure zero data loss
- ✅ **Batch Ingestion**: Optimized bulk event publishing with JDBC batch operations
- ✅ **Stream Navigation**: Support for `seek` (Earliest/Latest/Specific) to replay or skip events

#### Cloud-Native & Observable
- 📊 **Prometheus Metrics**: Built-in metrics for logs received, error rates, and latency
- 🐳 **Multi-Stage Docker Build**: Optimized container images (~100MB)
- ☸️ **Kubernetes Ready**: Complete manifests (Deployment, Service, Ingress, ConfigMap)
- 🔧 **Spring Actuator**: Health checks, info endpoints, and runtime metrics

#### Developer Experience
- 🔌 **Client SDK**: 
    - [Java Client](logpilot-client/README.md) with **Asynchronous Batching**
    - [Logback Appender](logpilot-logback/README.md) (New!)
    - [Spring Boot Starter](logpilot-spring-boot-starter/README.md) (New!)
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

**1. Publish a single event**
```bash
curl -X POST http://localhost:8080/api/logs \
  -H 'Content-Type: application/json' \
  -d '{
    "channel": "orders",
    "level": "INFO",
    "message": "{\"orderId\": \"ORD-123\", \"status\": \"CREATED\"}",
    "timestamp": "2025-09-25T05:00:00"
  }'
```

**2. Publish batch events**
```bash
curl -X POST http://localhost:8080/api/logs/batch \
  -H 'Content-Type: application/json' \
  -d '[
    { "channel": "orders", "level": "INFO", "message": "First event" },
    { "channel": "orders", "level": "INFO", "message": "Second event" }
  ]'
```

**3. Consume events**
```bash
# Fetch events with Consumer Offset (Reliable consumption)
# Providing consumerId updates the offset, so you don't read the same events twice.
curl "http://localhost:8080/api/logs/orders?consumerId=inventory-service&limit=10"

# Peek at events without updating offset
curl "http://localhost:8080/api/logs/orders?autoCommit=false&limit=5"
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
| `LOGPILOT_SQLITE_POOL_SIZE` | `10` | HikariCP max pool size |
| `LOGPILOT_RATE_LIMIT_CAPACITY` | `100` | Rate limit bucket capacity |
| `LOGPILOT_RATE_LIMIT_REFILL_TOKENS` | `100` | Tokens added per refill duration |
| `LOGPILOT_RATE_LIMIT_REFILL_DURATION` | `60` | Duration for refill (seconds) |
| `LOGPILOT_SQLITE_MIN_IDLE` | `2` | HikariCP min idle connections |
| `LOGPILOT_SQLITE_CONN_TIMEOUT` | `30000` | Connection timeout (ms) |
| `LOGPILOT_SQLITE_IDLE_TIMEOUT` | `600000` | Idle timeout (ms) |
| `LOGPILOT_STORAGE_DIR` | `./data/logs` | File storage directory |
| `LOGPILOT_MANAGEMENT_PORT` | `8081` | Actuator/metrics port |
| `LOGPILOT_API_KEY` | `logpilot-secret-key-123` | Server API Key for Authentication |

### 📡 API Reference

#### REST API Endpoints
- `POST /api/logs`: Send a single log entry.
- `POST /api/logs/batch`: Send a batch of log entries.
- `GET /api/logs`: Retrieve all logs (with limit).
- `GET /api/logs/{channel}`: Retrieve logs for a specific channel. Supports `consumerId` param for offset tracking.
  - Query Params:
    - `limit` (default: 100)
    - `autoCommit` (default: true): If false, offset is NOT updated. Use for "Peak & Commit" pattern.
- `POST /api/logs/commit`: Manually commit offset for a consumer.
  - Body: `{ "channel": "...", "consumerId": "...", "lastLogId": 123 }`
- `POST /api/logs/seek`: Seek offset for a consumer (Kafka-style).
  - Body: `{ "channel": "...", "consumerId": "...", "operation": "EARLIEST|LATEST|SPECIFIC", "logId": 123 }`

#### Reliability (Manual Ack)
To ensure zero data loss, use the "Fetch & Commit" pattern:
1. Fetch logs with `autoCommit=false`.
2. Process logs successfully.
3. Call `/api/logs/commit` with the highest `id` processed.
This ensures that if processing fails, the same logs will be delivered again on the next fetch.

#### Offset Management (Seek)
You can manually move the consumer's position using the `/api/logs/seek` API:
- **EARLIEST**: Replay all logs from the beginning.
- **LATEST**: Skip all current logs and only receive new logs.
- **SPECIFIC**: Jump to a specific log ID (or line number) for targeted reprocessing.

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
