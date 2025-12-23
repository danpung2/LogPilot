# LogPilot Server

The central server component of LogPilot that receives, stores, and manages logs.

## Features
- **Multi-Protocol**: Supports both gRPC (High performance) and REST API (Ease of use) for log ingestion.
- **Storage**: Currently supports SQLite for lightweight, zero-conf storage.
- **High Performance**: Optimized for write-heavy workloads with bulk insert and WAL mode.
- **Rate Limiting**: Built-in protection against client log flooding.

## Usage

Build and run the server:

```bash
./gradlew :logpilot-server:bootRun
```

Configuration can be customized via `application.yml` (e.g., changing ports, storage paths).

