# LogPilot Docker Guide

LogPilot is available on Docker Hub as a lightweight event streaming broker. We provide two ways to run LogPilot.

## 📦 Docker Images

- **Standalone (`danpung2/logpilot:latest`)**: Only the LogPilot server. Best for custom integration.
- **Full-Stack (`danpung2/logpilot:fullstack`)**: LogPilot + Prometheus + Grafana in a single image. Best for testing and demos.

---

## 📥 Pulling Images

If you want to pull the images before running them:

```bash
# Pull Standalone image
docker pull danpung2/logpilot:latest

# Pull Full-Stack image
docker pull danpung2/logpilot:fullstack
```

## 🚀 Quick Start (Standalone)

Run the server with a single command:

```bash
docker run -d -p 8080:8080 -p 50051:50051 danpung2/logpilot:latest
```

## 🚀 Quick Start (Full-Stack)

Get everything (Server + Metrics + Dashboard) with one command:

```bash
docker run -d \
  -p 8080:8080 -p 50051:50051 \
  -p 9090:9090 -p 3000:3000 \
  danpung2/logpilot:fullstack
```

- **REST API**: http://localhost:8080
- **Prometheus**: http://localhost:9090
- **Grafana**: http://localhost:3000 (Dashboards pre-loaded)

---

## 🛠 Docker Compose

### 1. Standalone Deployment

```bash
docker-compose up -d
```

### 2. Deployment with Monitoring Stack

```bash
docker-compose -f docker-compose.monitoring.yml up -d
```

---

## ⚙️ Configuration

| Variable | Description | Default |
|----------|-------------|---------|
| `LOGPILOT_API_KEY` | API Key for secure ingestion | `your-api-key-here` |
| `SERVER_PORT` | REST API Port | `8080` |
| `GRPC_PORT` | gRPC Port | `50051` |
| `DATA_PATH` | Path for SQLite storage | `/app/data` |
