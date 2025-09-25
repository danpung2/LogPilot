#!/bin/bash

# LogPilot minikube tunnel 스크립트
# LoadBalancer 서비스를 로컬에서 접근 가능하게 만듭니다.

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

# 신호 핸들러 - 스크립트 종료 시 터널도 함께 종료
cleanup_on_exit() {
    echo
    log_info "minikube tunnel을 종료합니다..."
    pkill -f "minikube tunnel" 2>/dev/null || true
    exit 0
}

main() {
    log_info "=== LogPilot minikube tunnel 시작 ==="

    # minikube 상태 확인
    if ! minikube status &> /dev/null; then
        log_error "minikube가 실행되지 않았습니다."
        log_info "먼저 minikube를 시작하세요: minikube start"
        exit 1
    fi

    # LoadBalancer 서비스 확인
    if ! kubectl get svc logpilot-loadbalancer -n logpilot &> /dev/null; then
        log_error "LoadBalancer 서비스를 찾을 수 없습니다."
        log_info "먼저 LogPilot을 배포하세요: ./k8s-deploy.sh"
        exit 1
    fi

    # 시그널 핸들러 등록
    trap cleanup_on_exit SIGINT SIGTERM

    log_warning "이 명령어는 관리자 권한이 필요할 수 있습니다."
    log_info "비밀번호를 입력하라고 나오면 입력해주세요."
    echo

    # minikube tunnel 시작
    log_info "minikube tunnel을 시작합니다..."
    minikube tunnel &

    # 잠시 대기
    sleep 5

    # LoadBalancer 서비스 상태 확인
    log_info "LoadBalancer 서비스 상태:"
    kubectl get svc logpilot-loadbalancer -n logpilot

    echo
    log_success "tunnel이 시작되었습니다!"
    log_info "이제 다음 주소로 접근할 수 있습니다:"

    # External IP 확인
    EXTERNAL_IP=$(kubectl get svc logpilot-loadbalancer -n logpilot -o jsonpath='{.status.loadBalancer.ingress[0].ip}')

    if [ -n "$EXTERNAL_IP" ] && [ "$EXTERNAL_IP" != "null" ]; then
        echo -e "${GREEN}HTTP REST API:${NC} http://$EXTERNAL_IP/api"
        echo -e "${GREEN}gRPC API:${NC} $EXTERNAL_IP:50051"
        echo
        log_info "테스트 명령어:"
        echo "curl -X POST http://$EXTERNAL_IP/api/logs -H 'Content-Type: application/json' -d '{\"channel\":\"test\",\"level\":\"INFO\",\"message\":\"via tunnel\"}'"
    else
        log_warning "External IP가 아직 할당되지 않았습니다. 잠시 후 다시 확인해주세요."
    fi

    echo
    log_warning "이 터미널을 닫으면 tunnel이 중단됩니다."
    log_info "중단하려면 Ctrl+C를 누르세요."

    # 터널 프로세스를 기다림
    wait
}

# 스크립트 실행
main "$@"