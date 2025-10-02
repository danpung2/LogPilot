#!/bin/bash

##############################################################################
# wrk 부하 테스트 스크립트
#
# 사용법:
#   ./wrk-load-test.sh [옵션]
#
# 옵션:
#   --threads, -t    스레드 수 (기본값: 10)
#   --connections, -c    동시 연결 수 (기본값: 100)
#   --duration, -d    테스트 시간 (기본값: 60s)
#   --host    LogPilot 호스트 (기본값: localhost:8080)
#   --scenario    시나리오 선택 (basic|random|spike) (기본값: random)
#   --help, -h    도움말 출력
##############################################################################

# set -e는 사용하지 않음 (kubectl 등 외부 명령어 실패 시 조기 종료 방지)

# 색상 정의
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# 기본값
THREADS=10
CONNECTIONS=100
DURATION="60s"
HOST="localhost:8080"
SCENARIO="random"
USE_K8S="auto"
NAMESPACE="logpilot"
SERVICE_NAME="logpilot-all"
PORT_FORWARD_PID=""

# 스크립트 디렉토리
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
TEMP_DIR="$SCRIPT_DIR/.wrk_temp"

##############################################################################
# 함수: 도움말 출력
##############################################################################
show_help() {
    cat << EOF
wrk 부하 테스트 스크립트

사용법:
    $0 [옵션]

옵션:
    -t, --threads NUM        스레드 수 (기본값: 10)
    -c, --connections NUM    동시 연결 수 (기본값: 100)
    -d, --duration TIME      테스트 시간 (예: 30s, 1m, 2h) (기본값: 60s)
    --host HOST:PORT         LogPilot 호스트 (기본값: localhost:8080)
    --scenario TYPE          시나리오 (basic|random|spike) (기본값: random)
    --k8s yes|no|auto        Kubernetes 모드 (기본값: auto - 자동 감지)
    --namespace NAME         Kubernetes 네임스페이스 (기본값: logpilot)
    --service NAME           Kubernetes 서비스 이름 (기본값: logpilot-all)
    -h, --help               도움말 출력

시나리오 설명:
    basic    - 단순 반복 요청 (동일한 데이터)
    random   - 랜덤 로그 레벨, 채널, 메시지 생성
    spike    - ERROR 로그 급증 시뮬레이션

Kubernetes 모드:
    auto     - kubectl 설치 여부 및 서비스 존재 여부로 자동 판단
    yes      - 강제로 port-forward 사용
    no       - port-forward 사용 안 함 (로컬 서버 사용)

예제:
    # 기본 설정으로 실행 (자동으로 k8s 감지)
    $0

    # 커스텀 설정
    $0 -t 20 -c 200 -d 120s --scenario random

    # Kubernetes 강제 사용
    $0 --k8s yes --namespace logpilot --service logpilot-all

    # 로컬 서버 사용
    $0 --k8s no --host localhost:8080

EOF
}

##############################################################################
# 함수: 파라미터 파싱
##############################################################################
parse_args() {
    while [[ $# -gt 0 ]]; do
        case $1 in
            -t|--threads)
                THREADS="$2"
                shift 2
                ;;
            -c|--connections)
                CONNECTIONS="$2"
                shift 2
                ;;
            -d|--duration)
                DURATION="$2"
                shift 2
                ;;
            --host)
                HOST="$2"
                shift 2
                ;;
            --scenario)
                SCENARIO="$2"
                shift 2
                ;;
            --k8s)
                USE_K8S="$2"
                shift 2
                ;;
            --namespace)
                NAMESPACE="$2"
                shift 2
                ;;
            --service)
                SERVICE_NAME="$2"
                shift 2
                ;;
            -h|--help)
                show_help
                exit 0
                ;;
            *)
                echo -e "${RED}Error: Unknown option $1${NC}"
                show_help
                exit 1
                ;;
        esac
    done
}

##############################################################################
# 함수: wrk 설치 확인
##############################################################################
check_wrk() {
    echo -e "${BLUE}==> wrk 설치 확인...${NC}"

    if ! command -v wrk &> /dev/null; then
        echo -e "${RED}Error: wrk가 설치되어 있지 않습니다${NC}"
        echo ""
        echo "설치 방법:"
        echo "  macOS:   brew install wrk"
        echo "  Ubuntu:  git clone https://github.com/wg/wrk.git && cd wrk && make && sudo cp wrk /usr/local/bin/"
        echo ""
        exit 1
    fi

    echo -e "${GREEN}✓ wrk 설치됨${NC}"
    wrk --version
    echo ""
}

##############################################################################
# 함수: Kubernetes 환경 확인
##############################################################################
check_kubernetes() {
    if [[ "$USE_K8S" == "auto" ]]; then
        echo -e "${BLUE}==> Kubernetes 환경 자동 감지...${NC}"

        # kubectl 설치 확인
        if ! command -v kubectl &> /dev/null; then
            echo -e "${YELLOW}⚠ kubectl이 설치되어 있지 않습니다. 로컬 모드로 실행합니다.${NC}"
            echo ""
            USE_K8S="no"
            return 0
        fi

        # kubectl 클러스터 연결 확인
        if ! kubectl cluster-info &> /dev/null; then
            echo -e "${YELLOW}⚠ Kubernetes 클러스터에 연결할 수 없습니다. 로컬 모드로 실행합니다.${NC}"
            echo ""
            USE_K8S="no"
            return 0
        fi

        # k8s 서비스 존재 확인
        if kubectl get svc "$SERVICE_NAME" -n "$NAMESPACE" &> /dev/null; then
            echo -e "${GREEN}✓ Kubernetes 서비스 발견: $SERVICE_NAME (namespace: $NAMESPACE)${NC}"
            echo ""
            USE_K8S="yes"
            return 0
        else
            echo -e "${YELLOW}⚠ Kubernetes 서비스를 찾을 수 없습니다. 로컬 모드로 실행합니다.${NC}"
            echo "   (namespace: $NAMESPACE, service: $SERVICE_NAME)"
            echo ""
            USE_K8S="no"
            return 0
        fi
    fi

    echo ""
    return 0
}

##############################################################################
# 함수: Port-forward 시작
##############################################################################
start_port_forward() {
    if [[ "$USE_K8S" != "yes" ]]; then
        return 0
    fi

    echo -e "${BLUE}==> Port-forward 시작...${NC}"

    # 기존 port-forward 프로세스 정리
    pkill -f "kubectl port-forward.*8080:8080" 2>/dev/null || true
    sleep 1

    # Port-forward 로그 파일
    local PF_LOG="$TEMP_DIR/port-forward.log"
    mkdir -p "$TEMP_DIR"

    # Port-forward 시작 (백그라운드)
    kubectl port-forward svc/"$SERVICE_NAME" 8080:8080 -n "$NAMESPACE" > "$PF_LOG" 2>&1 &
    PORT_FORWARD_PID=$!

    # Port-forward 준비 대기
    echo -e "${YELLOW}   Port-forward 준비 중 (PID: $PORT_FORWARD_PID)...${NC}"

    # 최대 10초 동안 대기하면서 "Forwarding from" 메시지 확인
    local wait_time=0
    local max_wait=10
    while [ $wait_time -lt $max_wait ]; do
        if grep -q "Forwarding from" "$PF_LOG" 2>/dev/null; then
            echo -e "${GREEN}✓ Port-forward 시작됨${NC}"
            echo ""
            return 0
        fi

        if ! ps -p $PORT_FORWARD_PID > /dev/null 2>&1; then
            echo -e "${RED}Error: Port-forward 프로세스가 종료되었습니다${NC}"
            echo "로그:"
            cat "$PF_LOG"
            echo ""
            return 1
        fi

        sleep 1
        ((wait_time++))
    done

    # 타임아웃
    echo -e "${YELLOW}⚠ Port-forward 준비 확인 타임아웃 (계속 진행)${NC}"
    echo ""
    return 0
}

##############################################################################
# 함수: Port-forward 정지
##############################################################################
stop_port_forward() {
    if [[ -n "$PORT_FORWARD_PID" ]] && ps -p $PORT_FORWARD_PID > /dev/null 2>&1; then
        echo -e "${BLUE}==> Port-forward 정지...${NC}"
        kill $PORT_FORWARD_PID 2>/dev/null || true
        echo -e "${GREEN}✓ Port-forward 정지됨${NC}"
    fi
}

##############################################################################
# 함수: 호스트 연결 확인
##############################################################################
check_host() {
    echo -e "${BLUE}==> 호스트 연결 확인: $HOST${NC}"

    # 연결 시도 (최대 5회)
    local max_attempts=5
    local attempt=1

    while [ $attempt -le $max_attempts ]; do
        # 간단한 TCP 연결 확인 (nc 사용)
        if nc -z -w 2 ${HOST%:*} ${HOST#*:} 2>/dev/null; then
            echo -e "${GREEN}✓ 호스트 연결 성공 (포트 열림)${NC}"
            echo ""
            return 0
        fi

        # nc가 없으면 curl로 시도
        if ! command -v nc &> /dev/null; then
            if timeout 2 bash -c "cat < /dev/null > /dev/tcp/${HOST%:*}/${HOST#*:}" 2>/dev/null; then
                echo -e "${GREEN}✓ 호스트 연결 성공${NC}"
                echo ""
                return 0
            fi
        fi

        if [ $attempt -lt $max_attempts ]; then
            echo -e "${YELLOW}   연결 시도 $attempt/$max_attempts 실패, 재시도 중...${NC}"
            sleep 2
            ((attempt++))
        else
            echo -e "${RED}Error: $HOST 에 연결할 수 없습니다${NC}"
            echo ""
            echo "다음을 확인하세요:"
            echo "  1. LogPilot이 실행 중인지 확인"
            if [[ "$USE_K8S" == "yes" ]]; then
                echo "  2. Kubernetes Pod가 Running 상태인지 확인:"
                echo "     kubectl get pods -n $NAMESPACE"
                echo "  3. Port-forward가 정상 작동하는지 확인:"
                echo "     ps aux | grep port-forward"
            else
                echo "  2. 로컬 서버가 $HOST 에서 실행 중인지 확인"
            fi
            echo ""
            exit 1
        fi
    done
}

##############################################################################
# 함수: 임시 디렉토리 생성
##############################################################################
create_temp_dir() {
    mkdir -p "$TEMP_DIR"
}

##############################################################################
# 함수: 임시 디렉토리 삭제
##############################################################################
cleanup() {
    echo ""
    echo -e "${BLUE}==> 정리 중...${NC}"

    # Port-forward 정지
    stop_port_forward

    # 임시 파일 삭제
    rm -rf "$TEMP_DIR"

    echo -e "${GREEN}✓ 정리 완료${NC}"
}

##############################################################################
# 시나리오 1: Basic - 단순 반복 요청
##############################################################################
scenario_basic() {
    echo -e "${YELLOW}==> 시나리오: Basic (단순 반복 요청)${NC}"

    local LUA_SCRIPT="$TEMP_DIR/basic.lua"

    cat > "$LUA_SCRIPT" <<'EOF'
wrk.method = "POST"
wrk.headers["Content-Type"] = "application/json"

request = function()
    wrk.body = string.format([[
{
  "level": "INFO",
  "message": "wrk load test - basic scenario",
  "channel": "load-test",
  "timestamp": "%s"
}
]], os.date("!%Y-%m-%dT%H:%M:%SZ"))

    return wrk.format()
end
EOF

    echo -e "${GREEN}✓ Lua 스크립트 생성: $LUA_SCRIPT${NC}"
    echo ""

    run_wrk "$LUA_SCRIPT"
}

##############################################################################
# 시나리오 2: Random - 랜덤 데이터 생성
##############################################################################
scenario_random() {
    echo -e "${YELLOW}==> 시나리오: Random (랜덤 로그 생성)${NC}"

    local LUA_SCRIPT="$TEMP_DIR/random.lua"

    cat > "$LUA_SCRIPT" <<'EOF'
-- 초기화
math.randomseed(os.time())

wrk.method = "POST"
wrk.headers["Content-Type"] = "application/json"

-- 로그 레벨 (INFO 60%, WARN 25%, ERROR 10%, DEBUG 5%)
local levels = {
    "INFO", "INFO", "INFO", "INFO", "INFO", "INFO",
    "WARN", "WARN", "WARN",
    "ERROR",
    "DEBUG"
}

-- 채널
local channels = {
    "user-service",
    "payment-service",
    "auth-service",
    "notification-service",
    "analytics-service",
    "storage-service",
    "api-gateway",
    "data-pipeline"
}

-- 메시지
local messages = {
    "Request processed successfully",
    "Database operation completed",
    "Cache updated",
    "External API called",
    "Message queued",
    "Task completed",
    "User authenticated",
    "Payment transaction processed",
    "File uploaded",
    "Data synchronized",
    "Notification sent",
    "Report generated"
}

-- 요청 생성
request = function()
    local level = levels[math.random(#levels)]
    local channel = channels[math.random(#channels)]
    local message = messages[math.random(#messages)]

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

    echo -e "${GREEN}✓ Lua 스크립트 생성: $LUA_SCRIPT${NC}"
    echo ""

    run_wrk "$LUA_SCRIPT"
}

##############################################################################
# 시나리오 3: Spike - ERROR 급증
##############################################################################
scenario_spike() {
    echo -e "${YELLOW}==> 시나리오: Spike (ERROR 로그 급증)${NC}"

    local LUA_SCRIPT="$TEMP_DIR/spike.lua"

    cat > "$LUA_SCRIPT" <<'EOF'
-- 초기화
math.randomseed(os.time())

wrk.method = "POST"
wrk.headers["Content-Type"] = "application/json"

-- 대부분 ERROR 로그 (ERROR 80%, WARN 15%, INFO 5%)
local levels = {
    "ERROR", "ERROR", "ERROR", "ERROR", "ERROR", "ERROR", "ERROR", "ERROR",
    "WARN", "WARN",
    "INFO"
}

-- 에러 메시지
local error_messages = {
    "Service unavailable",
    "Database connection timeout",
    "External API error",
    "Out of memory",
    "Authentication failed",
    "Payment gateway error",
    "Internal server error",
    "Resource not found",
    "Permission denied",
    "Request timeout"
}

-- 채널
local channels = {
    "payment-service",
    "database-service",
    "api-gateway",
    "auth-service"
}

-- 요청 생성
request = function()
    local level = levels[math.random(#levels)]
    local message = error_messages[math.random(#error_messages)]
    local channel = channels[math.random(#channels)]

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

    echo -e "${GREEN}✓ Lua 스크립트 생성: $LUA_SCRIPT${NC}"
    echo ""

    run_wrk "$LUA_SCRIPT"
}

##############################################################################
# 함수: wrk 실행
##############################################################################
run_wrk() {
    local LUA_SCRIPT="$1"

    echo -e "${BLUE}==> wrk 부하 테스트 시작${NC}"
    echo -e "${BLUE}    스레드: $THREADS${NC}"
    echo -e "${BLUE}    연결: $CONNECTIONS${NC}"
    echo -e "${BLUE}    시간: $DURATION${NC}"
    echo -e "${BLUE}    호스트: http://$HOST/api/logs${NC}"
    echo ""

    # wrk 실행
    wrk -t"$THREADS" -c"$CONNECTIONS" -d"$DURATION" \
        -s "$LUA_SCRIPT" \
        "http://$HOST/api/logs"

    echo ""
    echo -e "${GREEN}==> 부하 테스트 완료${NC}"
}

##############################################################################
# 함수: 결과 요약
##############################################################################
show_summary() {
    echo ""
    echo -e "${YELLOW}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
    echo -e "${YELLOW}  다음 단계${NC}"
    echo -e "${YELLOW}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
    echo ""
    echo "1. Prometheus에서 메트릭 확인:"
    echo "   http://localhost:9090"
    echo ""
    echo "   쿼리 예제:"
    echo "   - rate(http_server_requests_seconds_count{namespace=\"logpilot\"}[1m])"
    echo "   - rate(logpilot_logs_processed_total{namespace=\"logpilot\"}[1m])"
    echo ""
    echo "2. Grafana 대시보드 확인:"
    echo "   http://localhost:3000"
    echo ""
    echo "   대시보드:"
    echo "   - LogPilot Overview"
    echo "   - LogPilot Performance Metrics"
    echo "   - LogPilot Business Metrics"
    echo ""
}

##############################################################################
# Main
##############################################################################
main() {
    # 파라미터 파싱
    parse_args "$@"

    # 배너
    echo -e "${GREEN}"
    echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
    echo "  wrk 부하 테스트 스크립트"
    echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
    echo -e "${NC}"

    # trap으로 종료 시 정리
    trap cleanup EXIT INT TERM

    # 사전 체크
    check_wrk
    check_kubernetes

    # Port-forward 시작 (필요한 경우)
    start_port_forward

    # 호스트 연결 확인
    check_host

    # 임시 디렉토리 생성
    create_temp_dir

    # 시나리오 실행
    case "$SCENARIO" in
        basic)
            scenario_basic
            ;;
        random)
            scenario_random
            ;;
        spike)
            scenario_spike
            ;;
        *)
            echo -e "${RED}Error: Unknown scenario '$SCENARIO'${NC}"
            echo "Available scenarios: basic, random, spike"
            exit 1
            ;;
    esac

    # 결과 요약
    show_summary
}

# 스크립트 실행
main "$@"
