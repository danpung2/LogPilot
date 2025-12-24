# LogPilot Demo Consumer: Event Analytics Service

This module demonstrates how to **consume and process event streams** stored in LogPilot using the gRPC client.

## Overview

This application acts as a **Log Consumer**. It periodically fetches logs from the LogPilot Server to perform near real-time analytics. It demonstrates the use of the `LogPilotClient` (specifically via gRPC) to retrieve logs with pagination and offset tracking.

## Features

- **Log Polling**: Polls for new logs every second using a scheduled task.
- **Offset Management**: Tracks the last processed log ID to ensure logs are processed exactly once (or at least once, depending on strategy).
- **In-Memory Analytics**: Aggregates real-time statistics by parsing metadata (`MDC`) from consumed logs.
    - **Mechanism**:
        1. Logs are fetched via the `gRPC` client.
        2. Logs with `ERROR` level increment the global error counter.
        3. The system inspects the `action` key in the log's `Meta` data:
            - `action="VIEW_JOB"`: Increments the view count for the specific `jobId`.
            - `action="APPLY_JOB"`: Increments the application count for the specific `jobId`.
    - **Metrics Provided**:
        - **Total Views**: Sum of view counts across all Job IDs.
        - **Total Applications**: Sum of application counts across all Job IDs.
        - **Conversion Rate**: `(Total Applications / Total Views) * 100` (%)
        - **Error Count**: Total number of error-level logs detected.
    - **Technical Implementation**: Utilizes `ConcurrentHashMap` and `AtomicLong` to ensure thread-safe aggregation.

## Usage

### 1. Run the Application
The application runs on port `8083`.

```bash
./gradlew :logpilot-demo-consume:bootRun
```

### 2. Check Analytics
Access the analytics dashboard data via REST API.

- **Get Current Stats**:
  ```bash
  curl http://localhost:8083/api/analytics/stats
  ```

Sample Response:
```json
{
  "totalViews": 150,
  "totalApplications": 12,
  "conversionRate": "8.00%",
  "errorCount": 0
}
```
