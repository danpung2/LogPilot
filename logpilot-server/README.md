# LogPilot Server

The central message broker component of LogPilot that receives, stores, and manages event streams.

## Features
- **Multi-Protocol**: Supports both gRPC (High performance) and REST API (Ease of use) for event ingestion.
- **Storage**: Currently supports SQLite for lightweight, zero-conf storage.
- **High Performance**: Optimized for write-heavy workloads with bulk insert and WAL mode.
- **Rate Limiting**: Built-in protection against client log flooding.

## Usage

Build and run the server:

```bash
./gradlew :logpilot-server:bootRun
```

Configuration can be customized via `application.yml` (e.g., changing ports, storage paths).

