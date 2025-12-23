# LogPilot Demo Application

A Sample Spring Boot application to demonstrate LogPilot integration and traffic simulation.

## Features
- **Log Generation**: Automatically generates logs with various levels (INFO, WARN, ERROR) and patterns.
- **Integration Example**: Shows how to use `logpilot-spring-boot-starter` and `logpilot-logback`.
- **Traffic Simulator**: Simulates real-world traffic patterns to test LogPilot's performance.

## Usage

Run the demo application using Gradle:

```bash
./gradlew :logpilot-demo:bootRun
```

The application will start generating logs and sending them to the configured LogPilot Server (default: localhost:50051).

