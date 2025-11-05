#!/bin/bash

# LogPilot Kubernetes 배포 스크립트 (로컬 개발용)
# 이 스크립트는 minikube 환경에서 LogPilot을 배포하는데 사용됩니다.

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

# 명령어 존재 여부 확인 함수
check_command() {
    if ! command -v $1 &> /dev/null; then
        log_error "$1이 설치되지 않았습니다."
        exit 1
    fi
}

# minikube 상태 확인
check_minikube_status() {
    log_info "minikube 상태를 확인합니다..."
    if ! minikube status &> /dev/null; then
        log_warning "minikube가 실행되지 않았습니다. 시작합니다..."
        minikube start --driver=docker --disable-metrics=true --addons=ingress
        log_success "minikube를 시작했습니다."
    else
        log_success "minikube가 실행 중입니다."
    fi
}

# Docker 이미지 빌드 및 로드
build_and_load_image() {
    local IMAGE_NAME="logpilot:latest"

    log_info "Docker 이미지를 빌드합니다: $IMAGE_NAME"
    docker build -t $IMAGE_NAME .

    log_info "minikube에 이미지를 로드합니다..."
    minikube image load $IMAGE_NAME
    log_success "이미지를 성공적으로 로드했습니다."
}

# Kubernetes 리소스 배포
deploy_kubernetes_resources() {
    log_info "Kubernetes 리소스를 배포합니다..."

    # 네임스페이스 생성
    kubectl apply -f k8s/namespace.yaml
    log_success "네임스페이스를 생성했습니다."

    # ConfigMap 적용
    kubectl apply -f k8s/configmap.yaml
    log_success "ConfigMap을 적용했습니다."

    # Deployment 적용
    kubectl apply -f k8s/deployment-all.yaml
    log_success "Deployment를 적용했습니다."

    # Service 적용
    kubectl apply -f k8s/service.yaml
    log_success "Service를 적용했습니다."
}

# Pod 상태 확인 및 대기
wait_for_pods() {
    log_info "Pod가 준비될 때까지 기다립니다..."
    kubectl wait --for=condition=ready pod -l app=logpilot-all -n logpilot --timeout=30s
    log_success "모든 Pod가 실행 중입니다."
}

# 배포 상태 확인
check_deployment_status() {
    log_info "배포 상태를 확인합니다..."
    echo
    kubectl get pods -n logpilot
    echo
    kubectl get svc -n logpilot
    echo
}

# jq 설치 여부 확인 및 자동 설치
install_jq_if_missing() {
    if ! command -v jq &> /dev/null; then
        log_warning "jq가 설치되어 있지 않습니다. 자동으로 설치를 시도합니다..."

        if [[ "$OSTYPE" == "linux-gnu"* ]]; then
            if command -v apt-get &> /dev/null; then
                sudo apt-get update -y && sudo apt-get install -y jq
            elif command -v yum &> /dev/null; then
                sudo yum install -y jq
            else
                log_error "패키지 관리자를 찾을 수 없습니다. 수동으로 jq를 설치해주세요."
                exit 1
            fi
        elif [[ "$OSTYPE" == "darwin"* ]]; then
            if command -v brew &> /dev/null; then
                brew install jq
            else
                log_error "Homebrew가 설치되어 있지 않습니다. 먼저 Homebrew를 설치한 후 jq를 설치해주세요."
                log_info "설치 명령어: /bin/bash -c \"\$(curl -fsSL https://raw.githubusercontent.com/Homebrew/install/HEAD/install.sh)\""
                exit 1
            fi
        else
            log_error "지원되지 않는 OS입니다. jq를 수동으로 설치해주세요."
            exit 1
        fi

        log_success "jq가 성공적으로 설치되었습니다."
    else
        log_success "jq가 이미 설치되어 있습니다."
    fi
}

# 서비스 접속 정보 출력
show_access_info() {
    log_info "서비스 접속 정보:"
    echo

    # jq 확인 및 설치
    install_jq_if_missing

    # minikube IP 가져오기
    local MINIKUBE_IP=$(minikube ip)

    # minikube IP 가져오기
    local MINIKUBE_IP=$(minikube ip)

    # NodePort 서비스 포트 추출
    local HTTP_PORT=$(kubectl get svc logpilot-nodeport -n logpilot -o jsonpath='{.spec.ports[?(@.port==8080)].nodePort}')
    local GRPC_PORT=$(kubectl get svc logpilot-nodeport -n logpilot -o jsonpath='{.spec.ports[?(@.port==50051)].nodePort}')

    local HTTP_URL="http://${MINIKUBE_IP}:${HTTP_PORT}"
    local GRPC_URL="${MINIKUBE_IP}:${GRPC_PORT}"

    echo -e "${GREEN}HTTP REST API:${NC} $HTTP_URL"
    echo -e "${GREEN}gRPC API:${NC} $GRPC_URL"
    echo -e "${GREEN}Health Check:${NC} $HTTP_URL/actuator/health"
    echo -e "${GREEN}Metrics:${NC} $HTTP_URL/actuator/metrics"
    echo

    log_info "서비스를 브라우저에서 열려면 다음 명령어를 사용하세요:"
    echo "minikube service logpilot-nodeport -n logpilot"
}

# 메인 함수
main() {
    log_info "=== LogPilot Kubernetes 로컬 배포 시작 ==="

    # 필수 명령어 확인
    check_command "docker"
    check_command "minikube"
    check_command "kubectl"

    # minikube 상태 확인 및 시작
    check_minikube_status

    # Docker 이미지 빌드 및 로드
    build_and_load_image

    # Kubernetes 리소스 배포
    deploy_kubernetes_resources

    # Pod 상태 확인 및 대기
    wait_for_pods

    # 배포 상태 확인
    check_deployment_status

    # 서비스 접속 정보 출력
    show_access_info

    log_success "=== LogPilot 배포가 완료되었습니다! ==="
}

# 스크립트 실행
main "$@"