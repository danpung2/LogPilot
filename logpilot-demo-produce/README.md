# LogPilot Demo Application: Recruitment Site Simulation

This application simulates a high-traffic "Recruitment Site" (e.g., Job Portal) to demonstrate LogPilot's capabilities in a production-like environment.

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
```bash
./gradlew :logpilot-demo:bootRun
```

### 2. Control Simulation
Use the exposed REST API to control the traffic generator.

- **Start Steady Traffic**:
  ```bash
  curl -X POST "http://localhost:8080/simulation/start?mode=STEADY"
  ```
- **Start Peak Traffic**:
  ```bash
  curl -X POST "http://localhost:8080/simulation/start?mode=PEAK"
  ```
- **Stop Simulation**:
  ```bash
  curl -X POST http://localhost:8080/simulation/stop
  ```
- **Check Status**:
  ```bash
  curl http://localhost:8080/simulation/status
  ```

