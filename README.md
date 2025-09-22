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
