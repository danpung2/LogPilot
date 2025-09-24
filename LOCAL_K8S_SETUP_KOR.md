# 로컬 Kubernetes 환경 설정 가이드

### 1. 🐳 **Docker Desktop **
Docker Desktop에 내장된 Kubernetes 사용

#### 설치 및 설정:
```bash
# 1. Docker Desktop 설치
# https://www.docker.com/products/docker-desktop

# 2. Docker Desktop에서 Kubernetes 활성화
# Docker Desktop > Settings > Kubernetes > Enable Kubernetes 체크

# 3. 설정 확인
kubectl cluster-info
kubectl get nodes
```

#### 장점:
- ✅ 설치가 매우 간단
- ✅ Docker와 통합 환경
- ✅ macOS에서 안정적

#### 단점:
- ❌ 리소스 사용량이 높음
- ❌ 단일 노드만 지원

---

### 2. 🎯 **Kind (Kubernetes in Docker)**
Docker 컨테이너로 Kubernetes 클러스터 구성

#### 설치:
```bash
# Kind 설치 (macOS)
brew install kind

# 또는 직접 다운로드
curl -Lo ./kind https://kind.sigs.k8s.io/dl/v0.20.0/kind-darwin-amd64
chmod +x ./kind
sudo mv ./kind /usr/local/bin/kind
```

#### 클러스터 생성:
```bash
# 기본 클러스터 생성
kind create cluster --name logpilot

# 멀티노드 클러스터 생성 (선택사항)
cat <<EOF > kind-config.yaml
kind: Cluster
apiVersion: kind.x-k8s.io/v1alpha4
nodes:
- role: control-plane
  kubeadmConfigPatches:
  - |
    kind: InitConfiguration
    nodeRegistration:
      kubeletExtraArgs:
        node-labels: "ingress-ready=true"
  extraPortMappings:
  - containerPort: 80
    hostPort: 80
    protocol: TCP
  - containerPort: 443
    hostPort: 443
    protocol: TCP
- role: worker
- role: worker
EOF

kind create cluster --name logpilot --config kind-config.yaml
```

#### 클러스터 확인:
```bash
kubectl cluster-info --context kind-logpilot
kubectl get nodes
```

#### 장점:
- ✅ 가벼움 (Docker Desktop보다)
- ✅ 멀티노드 클러스터 지원
- ✅ CI/CD에 적합

#### 단점:
- ❌ 초기 설정이 필요
- ❌ LoadBalancer 타입 서비스 제한

---

### 3. 🚀 **Minikube**
가상머신 기반 Kubernetes 클러스터

#### 설치:
```bash
# Minikube 설치 (macOS)
brew install minikube

# 또는 직접 다운로드
curl -LO https://storage.googleapis.com/minikube/releases/latest/minikube-darwin-amd64
sudo install minikube-darwin-amd64 /usr/local/bin/minikube
```

#### 클러스터 시작:
```bash
# Docker 드라이버로 시작
minikube start --driver=docker

# 리소스 설정
minikube start --driver=docker --memory=4096 --cpus=2

# Ingress 애드온 활성화
minikube addons enable ingress
```

#### 클러스터 관리:
```bash
# 상태 확인
minikube status

# 대시보드 열기
minikube dashboard

# 서비스 접근 (터널링)
minikube tunnel

# 정지/삭제
minikube stop
minikube delete
```

#### 장점:
- ✅ 풍부한 애드온
- ✅ LoadBalancer 지원 (터널 모드)
- ✅ 대시보드 내장

#### 단점:
- ❌ 리소스 사용량 높음
- ❌ 설정이 복잡할 수 있음

---

### 4. ☁️ **클라우드 Kubernetes **
실제 클라우드 환경에서 테스트

#### AWS EKS:
```bash
# eksctl 설치
brew tap weaveworks/tap
brew install weaveworks/tap/eksctl

# 클러스터 생성
eksctl create cluster --name logpilot --region us-west-2 --nodegroup-name standard-workers --node-type t3.medium --nodes 2
```

#### Google GKE:
```bash
# gcloud CLI 설치 및 설정
brew install google-cloud-sdk
gcloud auth login
gcloud config set project YOUR_PROJECT_ID

# 클러스터 생성
gcloud container clusters create logpilot --zone us-central1-a --num-nodes 2
```

---

## 🔧 **설정 확인 명령어**

### kubectl 설치 확인:
```bash
kubectl version --client
```

### 클러스터 연결 확인:
```bash
kubectl cluster-info
kubectl get nodes
kubectl get namespaces
```

### 컨텍스트 관리:
```bash
# 현재 컨텍스트 확인
kubectl config current-context

# 사용 가능한 컨텍스트 목록
kubectl config get-contexts

# 컨텍스트 전환
kubectl config use-context docker-desktop
kubectl config use-context kind-logpilot
```
---

## 🛠️ **문제 해결**

### "kubectl: command not found"
```bash
# kubectl 설치
brew install kubectl

# 또는 Docker Desktop을 통해 자동 설치됨
```

### "The connection to the server localhost:8080 was refused"
```bash
# 클러스터가 실행 중인지 확인
docker ps | grep k8s

# Docker Desktop에서 Kubernetes 재시작
# Settings → Kubernetes → "Reset Kubernetes Cluster"
```

### "context deadline exceeded"
```bash
# 네트워크 확인
kubectl config view
kubectl cluster-info dump
```

### 포트 충돌
```bash
# 사용 중인 포트 확인
lsof -i :8080
lsof -i :50051

# 프로세스 종료
kill -9 <PID>
```