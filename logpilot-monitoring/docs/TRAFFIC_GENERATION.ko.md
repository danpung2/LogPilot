# 테스트 이벤트 트래픽 생성 가이드

## 📋 개요

Prometheus와 Grafana에서 유의미한 데이터를 확인하기 위해 LogPilot에 인위적인 **이벤트 트래픽**을 발생시키는 방법입니다.

---

## 🚀 방법 1: 간단한 HTTP 이벤트 생성

### curl을 사용한 반복 요청

```bash
# 기본 반복 요청 (100회)
for i in {1..100}; do
  curl -X POST http://localhost:8080/api/logs \
    -H "Content-Type: application/json" \
    -d '{
      "level": "INFO",
      "message": "테스트 이벤트 메시지 '$i'",
      "channel": "test-channel",
      "timestamp": "'$(date -u +%Y-%m-%dT%H:%M:%SZ)'"
    }'
  sleep 0.1
done
```

### 다양한 로그 레벨 생성

```bash
# INFO, WARN, ERROR 레벨을 랜덤하게 생성
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

## 🚀 방법 2: Apache Bench를 사용한 부하 테스트

### Apache Bench 설치

```bash
# macOS
brew install apache2

# Ubuntu/Debian
sudo apt-get install apache2-utils

# CentOS/RHEL
sudo yum install httpd-tools
```

### 간단한 GET 요청 부하

```bash
# 1000개 요청, 동시 접속 10
ab -n 1000 -c 10 http://localhost:8080/actuator/health

# 5000개 요청, 동시 접속 50, 결과 저장
ab -n 5000 -c 50 -g results.tsv http://localhost:8080/actuator/prometheus
```

### POST 요청 부하

```bash
# POST 데이터 파일 생성
cat > post_data.json <<EOF
{
  "level": "INFO",
  "message": "Load test message",
  "channel": "load-test",
  "timestamp": "$(date -u +%Y-%m-%dT%H:%M:%SZ)"
}
EOF

# POST 요청 부하 테스트
ab -n 2000 -c 20 -p post_data.json -T application/json \
  http://localhost:8080/api/logs
```

---

## 🚀 방법 3: wrk를 사용한 고성능 부하 테스트

### wrk 설치

```bash
# macOS
brew install wrk

# Ubuntu/Debian (빌드 필요)
git clone https://github.com/wg/wrk.git
cd wrk
make
sudo cp wrk /usr/local/bin/
```

### 기본 부하 테스트

```bash
# 30초 동안, 10개 스레드, 100개 연결
wrk -t10 -c100 -d30s http://localhost:8080/actuator/health

# 결과 예시:
# Running 30s test @ http://localhost:8080/actuator/health
#   10 threads and 100 connections
#   Thread Stats   Avg      Stdev     Max   +/- Stdev
#     Latency    10.23ms    5.45ms  89.12ms   78.23%
#     Req/Sec     1.02k   156.78    1.50k    68.00%
#   306789 requests in 30.10s, 45.67MB read
# Requests/sec:  10193.35
# Transfer/sec:      1.52MB
```

### Lua 스크립트를 사용한 POST 요청

```bash
# POST 스크립트 생성
cat > post.lua <<'EOF'
wrk.method = "POST"
wrk.body = '{"level":"INFO","message":"wrk test","channel":"wrk-test","timestamp":"2024-01-01T00:00:00Z"}'
wrk.headers["Content-Type"] = "application/json"
EOF

# POST 요청 부하 테스트
wrk -t10 -c100 -d60s -s post.lua http://localhost:8080/api/logs
```

### 다양한 데이터를 생성하는 Lua 스크립트

```bash
cat > random_logs.lua <<'EOF'
-- 요청 초기화
request = function()
  wrk.method = "POST"
  wrk.headers["Content-Type"] = "application/json"

  -- 랜덤 로그 레벨
  local levels = {"INFO", "INFO", "INFO", "WARN", "ERROR", "DEBUG"}
  local level = levels[math.random(#levels)]

  -- 랜덤 채널
  local channels = {"channel-1", "channel-2", "channel-3", "channel-4", "channel-5"}
  local channel = channels[math.random(#channels)]

  -- 랜덤 메시지
  local messages = {
    "User login successful",
    "Database query executed",
    "Cache hit",
    "API request processed",
    "File uploaded",
    "Payment processed"
  }
  local message = messages[math.random(#messages)]

  -- JSON 생성
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

# 실행
wrk -t10 -c100 -d120s -s random_logs.lua http://localhost:8080/api/logs
```

---

## 🚀 방법 4: 지속적인 백그라운드 트래픽 생성

### 무한 루프 스크립트

```bash
# 백그라운드 트래픽 생성 스크립트 작성
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
  # 랜덤 값 선택
  LEVEL=${LEVELS[$RANDOM % ${#LEVELS[@]}]}
  CHANNEL=${CHANNELS[$RANDOM % ${#CHANNELS[@]}]}
  MESSAGE=${MESSAGES[$RANDOM % ${#MESSAGES[@]}]}

  # 요청 전송
  curl -X POST http://localhost:8080/api/logs \
    -H "Content-Type: application/json" \
    -d "{
      \"level\": \"$LEVEL\",
      \"message\": \"$MESSAGE\",
      \"channel\": \"$CHANNEL\",
      \"timestamp\": \"$(date -u +%Y-%m-%dT%H:%M:%SZ)\"
    }" \
    -s -o /dev/null

  # 랜덤 대기 시간 (0.1초 ~ 1초)
  sleep 0.$((RANDOM % 10))
done
EOF

chmod +x generate_traffic.sh
```

### 백그라운드 실행

```bash
# 백그라운드로 실행
./generate_traffic.sh &

# PID 저장
echo $! > traffic_gen.pid

# 중지할 때
kill $(cat traffic_gen.pid)
rm traffic_gen.pid
```

---

## 🚀 방법 5: Kubernetes Pod에서 직접 트래픽 생성

### LogPilot Pod 내부에서 실행

```bash
# LogPilot Pod 이름 확인
POD_NAME=$(kubectl get pods -n logpilot -l app=logpilot -o jsonpath='{.items[0].metadata.name}')

# Pod 내부에서 트래픽 생성
kubectl exec -n logpilot $POD_NAME -- sh -c '
for i in $(seq 1 100); do
  curl -X POST http://localhost:8080/api/logs \
    -H "Content-Type: application/json" \
    -d "{\"level\":\"INFO\",\"message\":\"Internal test $i\",\"channel\":\"internal\"}"
  sleep 0.1
done
'
```

### 별도의 트래픽 생성 Pod 배포

```bash
# 트래픽 생성 Pod 생성
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

# 배포
kubectl apply -f traffic-generator.yaml

# 로그 확인
kubectl logs -f traffic-generator -n logpilot

# 삭제
kubectl delete pod traffic-generator -n logpilot
```

---

## 📊 트래픽 생성 시나리오

### 시나리오 1: 정상 운영 환경 시뮬레이션

```bash
# 백그라운드로 정상 트래픽 (INFO 70%, WARN 20%, ERROR 10%)
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

### 시나리오 2: 장애 상황 시뮬레이션

```bash
# 급격한 ERROR 증가
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

### 시나리오 3: 다양한 채널 활성화

```bash
# 여러 채널에서 동시 트래픽
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

## 🔍 메트릭 확인

### Prometheus에서 확인

트래픽 생성 후 Prometheus UI (http://localhost:9090)에서:

```promql
# HTTP 요청률 확인
rate(http_server_requests_seconds_count{namespace="logpilot"}[1m])

# 로그 처리율 확인
rate(logpilot_logs_processed_total{namespace="logpilot"}[1m])

# 레벨별 로그 수
sum by (level) (logpilot_logs_processed_total{namespace="logpilot"})
```

### Grafana에서 확인

Grafana UI (http://localhost:3000)에서:

1. **LogPilot Overview** 대시보드
   - Total Requests 증가 확인
   - Log Processing Rate 그래프 확인

2. **LogPilot Business Metrics** 대시보드
   - Logs by Level 분포 확인
   - Top Channels 확인
   - Log Timeline Heatmap 확인

3. **LogPilot Performance Metrics** 대시보드
   - HTTP Request Rate 증가 확인
   - Response Time 변화 확인

---

## 🎯 권장 테스트 순서

### 1단계: 기본 트래픽 생성 (5분)

```bash
# 간단한 curl 반복 (100회)
for i in {1..100}; do
  curl -X POST http://localhost:8080/api/logs \
    -H "Content-Type: application/json" \
    -d '{"level":"INFO","message":"Test '$i'","channel":"test"}'
  sleep 1
done
```

### 2단계: 다양한 로그 레벨 생성 (10분)

```bash
# 다양한 레벨의 로그 생성 스크립트 실행
for i in {1..300}; do
  LEVEL=$(shuf -n 1 -e INFO INFO INFO WARN ERROR)
  curl -X POST http://localhost:8080/api/logs \
    -H "Content-Type: application/json" \
    -d '{"level":"'$LEVEL'","message":"Test","channel":"test"}' \
    -s -o /dev/null
  sleep 2
done
```

### 3단계: 부하 테스트 (5분)

```bash
# wrk로 부하 생성
wrk -t10 -c50 -d300s -s random_logs.lua http://localhost:8080/api/logs
```

### 4단계: Grafana 대시보드 확인

- 모든 대시보드에 데이터가 표시되는지 확인
- 그래프가 정상적으로 업데이트되는지 확인
- 알림 임계값 테스트

---

## 📝 참고사항

### Port-forward 설정

LogPilot이 Kubernetes에 배포된 경우:

```bash
# LogPilot Service로 Port-forward
kubectl port-forward svc/logpilot 8080:8080 -n logpilot

# 그 다음 위의 스크립트들을 실행
```

### 트래픽 생성 중지

```bash
# 실행 중인 모든 백그라운드 curl 프로세스 중지
pkill -f "curl.*localhost:8080"

# wrk 중지
pkill wrk

# ab 중지
pkill ab
```

### 리소스 모니터링

트래픽 생성 중 시스템 리소스 확인:

```bash
# Pod 리소스 사용량
kubectl top pods -n logpilot

# 실시간 모니터링
watch kubectl top pods -n logpilot
```
