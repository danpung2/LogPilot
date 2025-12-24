# LogPilot Demo Producer: Recruitment Event Simulation

This module simulates a high-traffic "Recruitment Site" to demonstrate LogPilot's **Event Publishing** capabilities in a production-like environment.

## Overview

This application acts as a **Log Producer**. It simulates user scenarios such as viewing job postings and applying for jobs, generating various logs (INFO, WARN, ERROR) with rich metadata (MDC). These logs are sent to the LogPilot Server via the `logpilot-logback` appender.

## Simulation Scenarios

The app simulates user behaviors (viewing jobs, applying for jobs) with two traffic modes:

1.  **STEADY Mode**:
    - Simulates normal day-time traffic.
    - Users browse jobs (View) and occasionally apply.
    - Stable request rate.
2.  **PEAK Mode**:
    - Simulates "Application Deadline" traffic.
    - High concurrency and rapid request rate.
    - Higher write traffic (Applications) and occasional simulated system errors.

## Usage

### 1. Run the Application
The application runs on port `8082`.

```bash
./gradlew :logpilot-demo-produce:bootRun
```

### 2. Control Simulation
Use the exposed REST API to control the traffic generator.

- **Start Steady Traffic**:
  ```bash
  curl -X POST "http://localhost:8082/simulation/start?mode=STEADY"
  ```
  > **Scenario Details:**
  > - **Concurrency:** 5 concurrent worker threads.
  > - **Throughput:** ~5 requests/sec (0.5s - 1.5s delay per thread).
  > - **Behavior:** 90% Job Views, 10% Applications.
  > - **Goal:** Simulates daily operational traffic, generating mostly INFO logs.

- **Start Peak Traffic**:
  ```bash
  curl -X POST "http://localhost:8082/simulation/start?mode=PEAK"
  ```
  > **Scenario Details:**
  > - **Concurrency:** 5 concurrent worker threads running at high speed.
  > - **Throughput:** ~50 requests/sec (0.05s - 0.15s delay per thread).
  > - **Behavior:** 50% Job Views, 50% Applications.
  > - **Goal:** Simulates "Deadline Rush" traffic. Increases write operations and generates frequent WARN/ERROR logs due to business logic race conditions (e.g., duplicate applications).

- **Stop Simulation**:
  ```bash
  curl -X POST http://localhost:8082/simulation/stop
  ```
- **Check Status**:
  ```bash
  curl http://localhost:8082/simulation/status
  ```
