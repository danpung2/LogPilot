# Test Event Traffic Generation Guide

## 📋 Overview

Methods to generate artificial event traffic for LogPilot to verify **event ingestion** and metrics in Prometheus and Grafana.

---

## 🚀 Method 1: Simple HTTP Event Generation

### Repeat Requests using curl

```bash
# Basic repeat requests (100 times)
for i in {1..100}; do
  curl -X POST http://localhost:8080/api/logs \
    -H "Content-Type: application/json" \
    -d '{
      "level": "INFO",
      "message": "Test event message '$i'",
      "channel": "test-channel",
      "timestamp": "'$(date -u +%Y-%m-%dT%H:%M:%SZ)'"
    }'
  sleep 0.1
done
```

### Mixed Log Levels

```bash
# Randomly generate INFO, WARN, ERROR levels
for i in {1..200}; do
  LEVEL=$(shuf -n 1 -e INFO INFO INFO WARN ERROR)
  curl -X POST http://localhost:8080/api/logs \
    -H "Content-Type: application/json" \
    -d '{
      "level": "'$LEVEL'",
      "message": "Test log message '$i'",
      "channel": "channel-'$((RANDOM % 5))'",
      "timestamp": "'$(date -u +%Y-%m-%dT%H:%M:%SZ)'"
    }'
  sleep 0.05
done
```

---

## 🚀 Method 2: Load Testing with Apache Bench

### Install Apache Bench

```bash
# macOS
brew install apache2

# Ubuntu/Debian
sudo apt-get install apache2-utils

# CentOS/RHEL
sudo yum install httpd-tools
```

### Simple GET Load

```bash
# 1000 requests, 10 concurrent
ab -n 1000 -c 10 http://localhost:8080/actuator/health

# 5000 requests, 50 concurrent, save results
ab -n 5000 -c 50 -g results.tsv http://localhost:8080/actuator/prometheus
```

### POST Load

```bash
# Create POST data file
cat > post_data.json <<EOF
{
  "level": "INFO",
  "message": "Load test message",
  "channel": "load-test",
  "timestamp": "$(date -u +%Y-%m-%dT%H:%M:%SZ)"
}
EOF

# Run load test
ab -n 2000 -c 20 -p post_data.json -T application/json \
  http://localhost:8080/api/logs
```

---

## 🚀 Method 3: High-Performance Load Testing with wrk

### Install wrk

```bash
# macOS
brew install wrk

# Ubuntu/Debian (build required)
git clone https://github.com/wg/wrk.git
cd wrk
make
sudo cp wrk /usr/local/bin/
```

### Basic Load Test

```bash
# 30 seconds, 10 threads, 100 connections
wrk -t10 -c100 -d30s http://localhost:8080/actuator/health
```

### POST Requests using Lua Script

```bash
# Create POST script
cat > post.lua <<'EOF'
wrk.method = "POST"
wrk.body = '{"level":"INFO","message":"wrk test","channel":"wrk-test","timestamp":"2024-01-01T00:00:00Z"}'
wrk.headers["Content-Type"] = "application/json"
EOF

# Run load test
wrk -t10 -c100 -d60s -s post.lua http://localhost:8080/api/logs
```

### Lua Script for Random Data

```bash
cat > random_logs.lua <<'EOF'
-- Initialize request
request = function()
  wrk.method = "POST"
  wrk.headers["Content-Type"] = "application/json"

  -- Random log level
  local levels = {"INFO", "INFO", "INFO", "WARN", "ERROR", "DEBUG"}
  local level = levels[math.random(#levels)]

  -- Random channel
  local channels = {"channel-1", "channel-2", "channel-3", "channel-4", "channel-5"}
  local channel = channels[math.random(#channels)]

  -- Random message
  local messages = {
    "User login successful",
    "Database query executed",
    "Cache hit",
    "API request processed",
    "File uploaded",
    "Payment processed"
  }
  local message = messages[math.random(#messages)]

  -- Create JSON
  wrk.body = string.format([[
    {
      "level": "%s",
      "message": "%s",
      "channel": "%s",
      "timestamp": "%s"
    }
  ]], level, message, channel, os.date("!%Y-%m-%dT%H:%M:%SZ"))

  return wrk.format()
end
EOF

# Run
wrk -t10 -c100 -d120s -s random_logs.lua http://localhost:8080/api/logs
```

---

## 🚀 Method 4: Continuous Background Traffic Generation

### Infinite Loop Script

```bash
# Create script
cat > generate_traffic.sh <<'EOF'
#!/bin/bash

echo "Starting traffic generation..."
echo "Press Ctrl+C to stop"

LEVELS=("INFO" "INFO" "INFO" "WARN" "ERROR" "DEBUG")
CHANNELS=("user-service" "payment-service" "auth-service" "notification-service" "analytics-service")
MESSAGES=(
  "Request processed successfully"
  "Database operation completed"
  "Cache updated"
  "External API called"
  "Message queued"
  "Task completed"
)

while true; do
  # Select random values
  LEVEL=${LEVELS[$RANDOM % ${#LEVELS[@]}]}
  CHANNEL=${CHANNELS[$RANDOM % ${#CHANNELS[@]}]}
  MESSAGE=${MESSAGES[$RANDOM % ${#MESSAGES[@]}]}

  # Send request
  curl -X POST http://localhost:8080/api/logs \
    -H "Content-Type: application/json" \
    -d "{
      \"level\": \"$LEVEL\",
      \"message\": \"$MESSAGE\",
      \"channel\": \"$CHANNEL\",
      \"timestamp\": \"$(date -u +%Y-%m-%dT%H:%M:%SZ)\"
    }" \
    -s -o /dev/null

  # Random sleep (0.1s ~ 1s)
  sleep 0.$((RANDOM % 10))
done
EOF

chmod +x generate_traffic.sh
```

### Run in Background

```bash
# Run in background
./generate_traffic.sh &

# Save PID
echo $! > traffic_gen.pid

# Stop
kill $(cat traffic_gen.pid)
rm traffic_gen.pid
```

---

## 🚀 Method 5: Generate Traffic from Kubernetes Pod

### Execute inside LogPilot Pod

```bash
# Get Pod name
POD_NAME=$(kubectl get pods -n logpilot -l app=logpilot -o jsonpath='{.items[0].metadata.name}')

# Execute inside Pod
kubectl exec -n logpilot $POD_NAME -- sh -c '
for i in $(seq 1 100); do
  curl -X POST http://localhost:8080/api/logs \
    -H "Content-Type: application/json" \
    -d "{\"level\":\"INFO\",\"message\":\"Internal test $i\",\"channel\":\"internal\"}"
  sleep 0.1
done
'
```

### Deploy Separate Traffic Generator Pod

```bash
# Create Pod manifest
cat > traffic-generator.yaml <<'EOF'
apiVersion: v1
kind: Pod
metadata:
  name: traffic-generator
  namespace: logpilot
  labels:
    app: traffic-generator
spec:
  containers:
    - name: curl
      image: curlimages/curl:latest
      command:
        - sh
        - -c
        - |
          while true; do
            curl -X POST http://logpilot:8080/api/logs \
              -H "Content-Type: application/json" \
              -d '{"level":"INFO","message":"Traffic generator","channel":"k8s-test"}'
            sleep 1
          done
  restartPolicy: Never
EOF

# Deploy
kubectl apply -f traffic-generator.yaml

# Check logs
kubectl logs -f traffic-generator -n logpilot

# Delete
kubectl delete pod traffic-generator -n logpilot
```

---

## 📊 Traffic Scenarios

### Scenario 1: Normal Operation Simulation

```bash
cat > normal_traffic.sh <<'EOF'
#!/bin/bash
while true; do
  RAND=$((RANDOM % 10))
  if [ $RAND -lt 7 ]; then
    LEVEL="INFO"
  elif [ $RAND -lt 9 ]; then
    LEVEL="WARN"
  else
    LEVEL="ERROR"
  fi

  curl -X POST http://localhost:8080/api/logs \
    -H "Content-Type: application/json" \
    -d "{\"level\":\"$LEVEL\",\"message\":\"Normal operation\",\"channel\":\"prod\"}" \
    -s -o /dev/null

  sleep 0.5
done
EOF

chmod +x normal_traffic.sh
./normal_traffic.sh &
```

### Scenario 2: Error Spike Simulation

```bash
cat > spike_errors.sh <<'EOF'
#!/bin/bash
echo "Generating error spike..."
for i in {1..100}; do
  curl -X POST http://localhost:8080/api/logs \
    -H "Content-Type: application/json" \
    -d '{"level":"ERROR","message":"Service unavailable","channel":"payment"}' \
    -s -o /dev/null
  sleep 0.05
done
echo "Error spike completed"
EOF

chmod +x spike_errors.sh
./spike_errors.sh
```

### Scenario 3: Multi-Channel Activity

```bash
cat > multi_channel.sh <<'EOF'
#!/bin/bash
for channel in user auth payment notification analytics; do
  (
    for i in {1..50}; do
      curl -X POST http://localhost:8080/api/logs \
        -H "Content-Type: application/json" \
        -d "{\"level\":\"INFO\",\"message\":\"Activity on $channel\",\"channel\":\"$channel\"}" \
        -s -o /dev/null
      sleep 0.2
    done
  ) &
done
wait
echo "Multi-channel traffic completed"
EOF

chmod +x multi_channel.sh
./multi_channel.sh
```

---

## 🔍 Verifying Metrics

### Check in Prometheus

UI: http://localhost:9090

```promql
# HTTP Request Rate
rate(http_server_requests_seconds_count{namespace="logpilot"}[1m])

# Log Processing Rate
rate(logpilot_logs_processed_total{namespace="logpilot"}[1m])

# Log Count by Level
sum by (level) (logpilot_logs_processed_total{namespace="logpilot"})
```

### Check in Grafana

UI: http://localhost:3000

1. **LogPilot Overview**: Check Total Requests & Log Processing Rate.
2. **LogPilot Business Metrics**: Check Logs by Level & Top Channels.
3. **LogPilot Performance Metrics**: Check HTTP Request Rate & Response Times.

---

## 🎯 Recommended Test Sequence

1. **Basic Traffic (5m)**: Simple curl loop to verify connectivity.
2. **Mixed Levels (10m)**: Use script to generate various log levels.
3. **Load Test (5m)**: Use `wrk` to stress test.
4. **Dashboard Verification**: Ensure all panels display data correctly.

---

## 📝 Notes

### Port-forwarding

If LogPilot is in K8s:

```bash
kubectl port-forward svc/logpilot 8080:8080 -n logpilot
```

### Stopping Traffic

```bash
pkill -f "curl.*localhost:8080"
pkill wrk
pkill ab
```

### Resource Monitoring

```bash
watch kubectl top pods -n logpilot
```

---

## 🎯 Target Metrics

For meaningful dashboard visualization:

- **HTTP Request Rate**: 10+ req/sec
- **Log Processing Rate**: 50+ logs/sec
- **Error Rate**: 5-10%
- **Active Channels**: 5+
- **Log Level Distribution**: INFO 60%, WARN 25%, ERROR 10%, DEBUG 5%
