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

# 서비스 접속 정보 출력
show_access_info() {
    log_info "서비스 접속 정보:"
    echo

    # NodePort URL 가져오기
    local HTTP_URL=$(minikube service logpilot-nodeport -n logpilot --url | head -1)
    local GRPC_URL=$(minikube service logpilot-nodeport -n logpilot --url | tail -1)

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