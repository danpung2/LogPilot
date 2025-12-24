# LogPilot Monitoring Module

Optional monitoring stack for the LogPilot **Event Broker**, featuring Prometheus and Grafana.

## 📋 Overview

This module provides a complete observability stack for LogPilot:
- **Prometheus**: Metric collection and alerting.
- **Grafana**: Visual dashboards for monitoring performance and logs.

Unlike the core LogPilot server, this stack is **optional**. Deploy it only if you don't have an existing centralized monitoring system.

## 🚀 Quick Start

### 1. Automated Installation (Recommended)
You can deploy the entire stack (Prometheus + Grafana) using the provided script:

```bash
# Run from project root
./logpilot-monitoring/install-monitoring.sh
```

### 2. Manual Installation
For detailed manual steps and configuration options, see the documentation below:

- [**Prometheus Setup**](docs/PROMETHEUS_SETUP.md): Deployment and scraping configuration.
- [**Grafana Setup**](docs/GRAFANA_SETUP.md): Dashboard provisioning and data source configuration.
- [**Grafana Dashboards**](docs/GRAFANA_DASHBOARD.md): Details on included dashboards (Overview, Performance, etc.).
- [**Traffic Generation**](docs/TRAFFIC_GENERATION.md): Tools to generate load for testing metrics.

---

## 📊 Features

### Pre-configured Dashboards
The stack comes with ready-to-use Grafana dashboards:
- **LogPilot Overview**: High-level status, ingestion rates, and error counts.
- **System Performance**: CPU, Memory, GC, and Network usage.
- **Log Analytics**: Breakdown of logs by channel and level.

### Key Metrics
| Metric | Type | Description |
|--------|------|-------------|
| `logpilot_logs_received_total` | Counter | Total logs ingested (tagged by channel/level) |
| `http_requests_total` | Counter | REST API request counts |
| `grpc_server_requests_received` | Counter | gRPC request counts |
| `system_cpu_usage` | Gauge | JVM CPU usage |
| `jvm_memory_used_bytes` | Gauge | Heap/Non-heap memory usage |

---

## 🛠 Management via Scripts
The `scripts/` directory contains utility scripts:
- `create-dashboard-configmap.sh`: Updates dashboard ConfigMaps from JSON files.
- `wrk-load-test.sh`: Generates load using `wrk`.

## 📂 Directory Structure
```
logpilot-monitoring/
├── docs/               # Detailed documentation
├── k8s/                # Kubernetes manifests
│   ├── prometheus/     # Prometheus Deployment/Service/ConfigMap
│   ├── grafana/        # Grafana Deployment/Service/ConfigMap
│   └── alertmanager/   # AlertManager (Optional)
├── dashboards/         # Raw JSON Dashboard files
└── scripts/            # Helper scripts
```
