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
* 🚧 First-class Docker & Kubernetes support
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
