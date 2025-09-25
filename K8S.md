# LogPilot Kubernetes 배포 가이드

이 문서는 LogPilot 애플리케이션을 Kubernetes 클러스터에 배포하는 방법에 대한 완전한 가이드입니다.

## 사전 요구사항

### 필수 도구

- **Docker**: 컨테이너 이미지 빌드용
- **minikube**: 로컬 Kubernetes 클러스터
- **kubectl**: Kubernetes CLI
- **Java 17+**: 애플리케이션 빌드용 (Docker 사용 시 선택사항)

### 설치 명령어 (macOS)

```bash
# Homebrew를 통한 설치
brew install docker minikube kubectl

# Docker Desktop 시작 후
minikube start --driver=docker
```

## 로컬 개발 환경 (minikube)

### 1. minikube 클러스터 설정

```bash
# minikube 시작 (Docker 드라이버 사용)
minikube start --driver=docker --disable-metrics=true --addons=ingress

# 클러스터 상태 확인
minikube status
kubectl get nodes
```

### 2. 자동 배포 (권장)

가장 간단한 방법으로, 프로젝트 루트에서 다음 명령어를 실행하세요:

```bash
./k8s-deploy.sh
```

이 스크립트는 다음 작업을 자동으로 수행합니다:
- minikube 상태 확인 및 시작
- Docker 이미지 빌드
- minikube에 이미지 로드
- Kubernetes 리소스 배포
- Pod 상태 대기
- 서비스 접근 정보 출력

### 3. 수동 배포

단계별로 수동 배포를 원하는 경우:

```bash
# 1. Docker 이미지 빌드
docker build -t logpilot:latest .

# 2. minikube에 이미지 로드
minikube image load logpilot:latest

# 3. Kubernetes 리소스 배포
kubectl apply -f k8s/namespace.yaml
kubectl apply -f k8s/configmap.yaml
kubectl apply -f k8s/deployment-all.yaml
kubectl apply -f k8s/service.yaml

# 4. 배포 상태 확인
kubectl get pods -n logpilot
kubectl get svc -n logpilot
```

## Kubernetes 매니페스트 파일 구조

### 주요 매니페스트 파일 설명

#### 1. Namespace (namespace.yaml)
```yaml
# logpilot과 logpilot-system 네임스페이스 생성
apiVersion: v1
kind: Namespace
metadata:
  name: logpilot
```

#### 2. ConfigMap (configmap.yaml)
애플리케이션 설정을 포함하며, 다음 설정을 제공합니다:
- Spring Boot 설정
- 로그 저장소 설정 (SQLite)
- gRPC/REST 포트 설정
- 관리 엔드포인트 설정

#### 3. Deployment (deployment-all.yaml)
- **이미지**: `logpilot:latest`
- **포트**: 8080 (HTTP), 50051 (gRPC), 8081 (Management)
- **볼륨**: ConfigMap과 PersistentVolume 마운트
- **헬스 체크**: Liveness, Readiness, Startup 프로브 설정

#### 4. Service (service.yaml)
다양한 서비스 타입을 제공합니다:
- **ClusterIP**: 클러스터 내부 통신
- **NodePort**: 개발/테스트용 외부 접근
- **LoadBalancer**: 프로덕션 환경용

## 배포 방법

### 환경별 배포 선택

1. **통합 서버** (REST + gRPC): `deployment-all.yaml` (기본)
2. **REST 전용**: `deployment-rest.yaml`
3. **gRPC 전용**: `deployment-grpc.yaml`

### 배포 명령어

```bash
# 기본 통합 배포
kubectl apply -f k8s/deployment-all.yaml

# 또는 특정 배포 선택
kubectl apply -f k8s/deployment-rest.yaml
kubectl apply -f k8s/deployment-grpc.yaml
```

## 서비스 접근

LogPilot에 접근하는 여러 방법을 제공합니다.

### 방법 1: 포트 포워딩

```bash
# 자동 포트 포워딩 스크립트
./k8s-port-forward.sh

# 수동 포트 포워딩
kubectl port-forward -n logpilot svc/logpilot-all 8080:8080 &
kubectl port-forward -n logpilot svc/logpilot-all 50051:50051 &
kubectl port-forward -n logpilot svc/logpilot-all 8081:8081 &
```

### 방법 2: minikube tunnel (LoadBalancer 사용)

```bash
# tunnel 스크립트 실행
./k8s-tunnel.sh

# 또는 수동으로
minikube tunnel

# LoadBalancer External IP 확인
kubectl get svc logpilot-loadbalancer -n logpilot
```

### 방법 3: Ingress (도메인 기반 접근)

```bash
# Ingress 배포
kubectl apply -f k8s/ingress.yaml

# /etc/hosts에 도메인 추가 (수동)
echo "127.0.0.1 logpilot.local" >> /etc/hosts

# 접근
curl http://logpilot.local/api/logs
```

### 방법 4: Consumer/Producer를 Kubernetes에 배포

```bash
# Client 이미지 빌드 (필요시)
docker build -f Dockerfile.client -t logpilot-client:latest .
minikube image load logpilot-client:latest

# Consumer/Producer 배포
kubectl apply -f k8s/consumer-producer-deployment.yaml

# 로그 확인
kubectl logs -f deployment/logpilot-consumer -n logpilot
kubectl logs -f deployment/logpilot-producer -n logpilot
```

### 방법 5: NodePort 직접 접근

```bash
# minikube IP 확인
MINIKUBE_IP=$(minikube ip)

# NodePort를 통한 직접 접근
curl -X POST http://$MINIKUBE_IP:30080/api/logs \
  -H 'Content-Type: application/json' \
  -d '{"channel":"test","level":"INFO","message":"via nodeport"}'

# gRPC 접근 (consumer/producer에서)
java -jar logpilot-client.jar --grpc.server.address=$MINIKUBE_IP:30051
```

### 접근 URL

서비스가 실행된 후 다음 엔드포인트에 접근할 수 있습니다:

- **REST API**: `http://localhost:8080/api`
- **gRPC API**: `localhost:50051`
- **Health Check**: `http://localhost:8081/actuator/health`
- **Metrics**: `http://localhost:8081/actuator/metrics`
- **Prometheus**: `http://localhost:8081/actuator/prometheus`

### API 사용 예시

#### REST API 사용법

```bash
# 로그 저장 (단일)
curl -X POST http://localhost:8080/api/logs \
  -H 'Content-Type: application/json' \
  -d '{
    "channel": "my-app",
    "level": "INFO",
    "message": "Application started successfully",
    "timestamp": "2025-09-25T05:00:00"
  }'

# 로그 저장 (배치)
curl -X POST http://localhost:8080/api/logs/batch \
  -H 'Content-Type: application/json' \
  -d '[
    {
      "channel": "my-app",
      "level": "INFO",
      "message": "First log entry"
    },
    {
      "channel": "my-app",
      "level": "ERROR",
      "message": "Second log entry"
    }
  ]'

# 모든 로그 조회
curl http://localhost:8080/api/logs

# 특정 채널의 로그 조회
curl http://localhost:8080/api/logs/my-app

# 제한된 개수의 로그 조회
curl "http://localhost:8080/api/logs?limit=50"

# Consumer ID를 사용한 로그 조회
curl "http://localhost:8080/api/logs/my-app?consumerId=consumer-1&limit=10"
```

#### LogEntry 필수 필드
- `channel` (String): 로그 채널명 - **필수**
- `level` (String): 로그 레벨 (DEBUG, INFO, WARN, ERROR) - **필수**
- `message` (String): 로그 메시지 - **필수**
- `timestamp` (String): ISO 형식 타임스탬프 (선택사항, 생략시 현재시간)
- `meta` (Object): 추가 메타데이터 (선택사항)

## 모니터링 및 디버깅

### 1. Pod 상태 확인

```bash
# Pod 목록 조회
kubectl get pods -n logpilot

# Pod 상세 정보
kubectl describe pod <pod-name> -n logpilot

# Pod 로그 확인
kubectl logs <pod-name> -n logpilot

# 실시간 로그 스트리밍
kubectl logs -f <pod-name> -n logpilot
```

### 2. 서비스 상태 확인

```bash
# 서비스 목록
kubectl get svc -n logpilot

# 엔드포인트 확인
kubectl get endpoints -n logpilot
```

### 3. 네트워크 디버깅

```bash
# Pod 내부 접근
kubectl exec -it <pod-name> -n logpilot -- /bin/bash

# 네트워크 연결 테스트
kubectl exec -it <pod-name> -n logpilot -- curl http://localhost:8080/actuator/health
```

### 4. 이벤트 확인

```bash
# 네임스페이스 이벤트 조회
kubectl get events -n logpilot --sort-by='.lastTimestamp'
```

## 고급 기능

### 1. Ingress 설정 (선택사항)

```bash
# Ingress 적용
kubectl apply -f k8s/ingress.yaml

# Ingress 상태 확인
kubectl get ingress -n logpilot
```

### 2. 모니터링 설정

```bash
# 모니터링 리소스 배포
kubectl apply -f k8s/monitoring.yaml
```

### 3. 스케일링

```bash
# 레플리카 수 조정
kubectl scale deployment/logpilot-all --replicas=3 -n logpilot

# 오토스케일링 설정
kubectl autoscale deployment/logpilot-all --cpu-percent=50 --min=1 --max=10 -n logpilot
```

## 설정 변경

### ConfigMap 수정

```bash
# ConfigMap 편집
kubectl edit configmap logpilot-config -n logpilot

# 또는 파일 수정 후 재적용
kubectl apply -f k8s/configmap.yaml

# Pod 재시작 (설정 반영)
kubectl rollout restart deployment/logpilot-all -n logpilot
```

### 환경 변수 추가

deployment.yaml에서 환경 변수를 추가할 수 있습니다:

```yaml
env:
- name: SPRING_PROFILES_ACTIVE
  value: "all"
- name: LOGPILOT_CUSTOM_SETTING
  value: "custom-value"
```

## 정리 및 삭제

### 1. 애플리케이션 삭제

```bash
# 배포된 리소스 삭제
kubectl delete -f k8s/deployment-all.yaml
kubectl delete -f k8s/service.yaml
kubectl delete -f k8s/configmap.yaml

# 또는 네임스페이스 전체 삭제
kubectl delete namespace logpilot
```

### 2. minikube 정리

```bash
# minikube 클러스터 중지
minikube stop

# minikube 클러스터 삭제
minikube delete
```

## 디버깅

```bash
# 클러스터 전체 상태 확인
kubectl get all -n logpilot

# 리소스 사용량 확인
kubectl top pods -n logpilot

# 설정 확인
kubectl get configmap logpilot-config -n logpilot -o yaml
```