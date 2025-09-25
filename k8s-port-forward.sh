#!/bin/bash

# LogPilot Kubernetes 포트 포워딩 스크립트
# 이 스크립트는 minikube에서 실행 중인 LogPilot 서비스를 로컬에서 접근할 수 있게 합니다.

set -e

# 색상 코드
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# 로그 함수들
log_info() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

log_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

log_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# 기존 포트 포워딩 프로세스 종료
cleanup_port_forwards() {
    log_info "기존 포트 포워딩 프로세스를 정리합니다..."
    pkill -f "kubectl port-forward.*logpilot" 2>/dev/null || true
    sleep 2
}

# 포트 포워딩 시작
start_port_forwards() {
    log_info "포트 포워딩을 시작합니다..."

    # HTTP REST API 포트 (8080)
    log_info "HTTP REST API 포트 포워딩 시작: 8080"
    kubectl port-forward -n logpilot svc/logpilot-all 8080:8080 > /dev/null 2>&1 &
    HTTP_PID=$!

    # gRPC API 포트 (50051)
    log_info "gRPC API 포트 포워딩 시작: 50051"
    kubectl port-forward -n logpilot svc/logpilot-all 50051:50051 > /dev/null 2>&1 &
    GRPC_PID=$!

    # Management 포트 (8081)
    log_info "Management 포트 포워딩 시작: 8081"
    kubectl port-forward -n logpilot svc/logpilot-all 8081:8081 > /dev/null 2>&1 &
    MGMT_PID=$!

    # 잠시 대기하여 포트 포워딩이 시작되도록 함
    sleep 3
}

# 연결 테스트
test_connections() {
    log_info "연결을 테스트합니다..."

    # Management 포트 테스트
    if curl -s -f http://localhost:8081/actuator/health > /dev/null; then
        log_success "Management 포트 (8081) 연결 성공"
    else
        log_error "Management 포트 (8081) 연결 실패"
    fi

    # HTTP REST API 테스트 (간단한 연결 테스트)
    if nc -z localhost 8080 2>/dev/null; then
        log_success "HTTP REST API 포트 (8080) 연결 성공"
    else
        log_error "HTTP REST API 포트 (8080) 연결 실패"
    fi

    # gRPC API 테스트 (간단한 연결 테스트)
    if nc -z localhost 50051 2>/dev/null; then
        log_success "gRPC API 포트 (50051) 연결 성공"
    else
        log_error "gRPC API 포트 (50051) 연결 실패"
    fi
}

# 사용법 출력
show_usage_info() {
    echo
    log_info "=== LogPilot 로컬 접근 정보 ==="
    echo
    echo -e "${GREEN}HTTP REST API:${NC} http://localhost:8080"
    echo -e "${GREEN}gRPC API:${NC} localhost:50051"
    echo -e "${GREEN}Management:${NC} http://localhost:8081"
    echo -e "${GREEN}Health Check:${NC} http://localhost:8081/actuator/health"
    echo -e "${GREEN}Metrics:${NC} http://localhost:8081/actuator/metrics"
    echo

    log_info "=== Consumer/Producer 실행 예시 ==="
    echo
    echo "# gRPC Consumer/Producer"
    echo "java -jar logpilot-client/build/libs/logpilot-client.jar --grpc.server.address=localhost:50051"
    echo
    echo "# REST API 로그 저장"
    echo "curl -X POST http://localhost:8080/api/logs \\"
    echo "  -H 'Content-Type: application/json' \\"
    echo "  -d '{\"channel\":\"test\",\"level\":\"INFO\",\"message\":\"test log\"}'"
    echo
    echo "# REST API 로그 조회"
    echo "curl http://localhost:8080/api/logs"
    echo

    log_warning "이 터미널을 닫으면 포트 포워딩이 중단됩니다."
    log_info "중단하려면 Ctrl+C를 누르세요."
}

# 신호 핸들러 - 스크립트 종료 시 포트 포워딩도 함께 종료
cleanup_on_exit() {
    echo
    log_info "포트 포워딩을 종료합니다..."
    cleanup_port_forwards
    exit 0
}

# 메인 함수
main() {
    log_info "=== LogPilot Kubernetes 포트 포워딩 시작 ==="

    # kubectl 명령어 확인
    if ! command -v kubectl &> /dev/null; then
        log_error "kubectl이 설치되지 않았습니다."
        exit 1
    fi

    # Kubernetes 클러스터 연결 확인
    if ! kubectl get nodes &> /dev/null; then
        log_error "Kubernetes 클러스터에 연결할 수 없습니다."
        log_info "minikube가 실행 중인지 확인하세요: minikube status"
        exit 1
    fi

    # LogPilot 서비스 확인
    if ! kubectl get svc logpilot-all -n logpilot &> /dev/null; then
        log_error "LogPilot 서비스를 찾을 수 없습니다."
        log_info "먼저 LogPilot을 배포하세요: ./k8s-deploy.sh"
        exit 1
    fi

    # 시그널 핸들러 등록 (Ctrl+C 처리)
    trap cleanup_on_exit SIGINT SIGTERM

    # 기존 포트 포워딩 정리
    cleanup_port_forwards

    # 포트 포워딩 시작
    start_port_forwards

    # 연결 테스트
    test_connections

    # 사용법 정보 출력
    show_usage_info

    # 백그라운드에서 실행 중인 포트 포워딩을 기다림
    wait
}

# 스크립트 실행
main "$@"