# LogPilot Kubernetes Deployment Guide

## 🤔 Why Deploy LogPilot on Kubernetes?

### 1. **Production-Ready Logging Service**
LogPilot은 로그 수집 시스템으로, 다음과 같은 특성을 가집니다:
- **고가용성 필요**: 로그 유실 방지를 위한 무중단 서비스
- **확장성 필요**: 트래픽 증가에 따른 자동 스케일링
- **내결함성 필요**: 장애 시 자동 복구 및 롤백
- **모니터링 필요**: 서비스 상태 및 성능 지표 추적

### 2. **Kubernetes가 제공하는 이점**

#### 🔄 **자동 스케일링**
```yaml
# HorizontalPodAutoscaler 예시
apiVersion: autoscaling/v2
kind: HorizontalPodAutoscaler
metadata:
  name: logpilot-hpa
spec:
  scaleTargetRef:
    apiVersion: apps/v1
    kind: Deployment
    name: logpilot-all
  minReplicas: 2
  maxReplicas: 10
  metrics:
  - type: Resource
    resource:
      name: cpu
      target:
        type: Utilization
        averageUtilization: 70
```

#### 🛡️ **고가용성 및 내결함성**
- **Pod 자동 재시작**: 컨테이너 실패 시 자동 복구
- **Rolling Update**: 무중단 배포
- **Health Check**: 서비스 상태 모니터링
- **Multi-AZ 배포**: 여러 가용 영역에 분산 배치

#### 🎯 **서비스 디스커버리**
- **DNS 기반 서비스 발견**: `logpilot-all.logpilot.svc.cluster.local`
- **로드 밸런싱**: 여러 Pod 간 자동 트래픽 분산
- **환경별 설정**: ConfigMap을 통한 설정 관리

#### 📊 **리소스 관리**
- **CPU/Memory 제한**: 리소스 사용량 제어
- **Storage 관리**: PersistentVolume을 통한 데이터 영속성
- **네트워크 정책**: 보안 강화

### 3. **LogPilot 특화 요구사항**

#### **다중 프로토콜 지원**
```
┌─────────────────┐    ┌─────────────────┐
│   REST Client   │───▶│  logpilot-rest  │
└─────────────────┘    └─────────────────┘
                              │
┌─────────────────┐           │
│  gRPC Client    │───────────┼─────────────┐
└─────────────────┘           │             │
                              ▼             ▼
                       ┌─────────────────────────┐
                       │    Shared Storage       │
                       │   (PersistentVolume)    │
                       └─────────────────────────┘
```

#### **성능 최적화**
- **REST vs gRPC**: 프로토콜별 최적화된 배포
- **분리 배포**: 각 서비스의 독립적인 스케일링
- **리소스 할당**: 프로토콜별 특성에 맞는 리소스 배정

---

## 📁 Kubernetes Manifests 상세 설명

### 🏠 `namespace.yaml`
**목적**: 리소스 격리 및 관리

```yaml
apiVersion: v1
kind: Namespace
metadata:
  name: logpilot                    # 애플리케이션 네임스페이스
  labels:
    name: logpilot
    app: logpilot
---
apiVersion: v1
kind: Namespace
metadata:
  name: logpilot-system            # 시스템/모니터링 네임스페이스
```

**특징**:
- **logpilot**: 애플리케이션 리소스
- **logpilot-system**: 모니터링, 백업 등 시스템 리소스
- **멀티테넌시**: 다른 애플리케이션과 격리

---

### ⚙️ `configmap.yaml`
**목적**: 애플리케이션 설정 관리

#### **logpilot-config** (All-in-One 모드)
```yaml
logpilot:
  server:
    protocol: all                  # REST + gRPC 동시 지원
  storage:
    type: sqlite
    directory: /data/logs
    sqlite:
      path: /data/logpilot.db
```

#### **logpilot-rest-config** (REST 전용)
```yaml
spring:
  profiles:
    active: rest                   # REST 프로파일 활성화
grpc:
  server:
    enabled: false                 # gRPC 비활성화
```

#### **logpilot-grpc-config** (gRPC 전용)
```yaml
server:
  port: -1                         # HTTP 서버 비활성화
grpc:
  server:
    port: 50051                    # gRPC만 활성화
```

**장점**:
- **환경별 분리**: 개발/스테이징/프로덕션 설정
- **핫 리로드**: ConfigMap 변경 시 자동 반영
- **보안**: 민감 정보는 Secret 분리

---

### 🚀 Deployment 파일들

#### `deployment-all.yaml` - 통합 배포
**용도**: 개발 환경, 소규모 배포

```yaml
spec:
  replicas: 2                      # 고가용성을 위한 최소 2개
  template:
    spec:
      containers:
      - name: logpilot
        image: logpilot:latest
        ports:
        - name: http
          containerPort: 8080      # REST API
        - name: grpc
          containerPort: 50051     # gRPC Service
        - name: management
          containerPort: 8081      # Actuator
        resources:
          requests:
            cpu: 100m              # 최소 CPU 요구량
            memory: 256Mi          # 최소 메모리 요구량
          limits:
            cpu: 500m              # 최대 CPU 제한
            memory: 512Mi          # 최대 메모리 제한
```

**Health Check 전략**:
```yaml
livenessProbe:                     # 컨테이너 생존 확인
  httpGet:
    path: /actuator/health
    port: management
  initialDelaySeconds: 30          # 첫 체크까지 대기
  periodSeconds: 10                # 체크 간격

readinessProbe:                    # 트래픽 수신 준비 확인
  httpGet:
    path: /actuator/health
    port: management
  initialDelaySeconds: 10
  periodSeconds: 5

startupProbe:                      # 시작 시간이 긴 애플리케이션용
  httpGet:
    path: /actuator/health
    port: management
  initialDelaySeconds: 10
  failureThreshold: 10             # 최대 10번 실패 허용
```

#### `deployment-rest.yaml` - REST 전용
**용도**: 웹 애플리케이션 클라이언트 대응

```yaml
spec:
  replicas: 3                      # 웹 트래픽을 위한 높은 복제본
  template:
    spec:
      containers:
      - name: logpilot-rest
        image: logpilot-rest:latest
        resources:
          requests:
            cpu: 50m               # 더 낮은 리소스 요구량
            memory: 128Mi
          limits:
            cpu: 300m
            memory: 256Mi
```

**특징**:
- **경량화**: gRPC 라이브러리 제외로 더 적은 리소스 사용
- **수평 확장**: 웹 트래픽 증가에 대응

#### `deployment-grpc.yaml` - gRPC 전용
**용도**: 고성능 클라이언트 대응

```yaml
livenessProbe:
  exec:
    command:
    - /bin/sh
    - -c
    - "grpcurl -plaintext localhost:50051 grpc.health.v1.Health/Check || exit 1"
```

**특징**:
- **성능 최적화**: HTTP 서버 오버헤드 제거
- **gRPC Health Check**: 프로토콜별 헬스체크

---

### 🌐 `service.yaml`
**목적**: 네트워크 접근 및 로드밸런싱

#### **ClusterIP Services**
```yaml
apiVersion: v1
kind: Service
metadata:
  name: logpilot-all
spec:
  type: ClusterIP                  # 클러스터 내부 접근만
  ports:
  - name: http
    port: 8080                     # 서비스 포트
    targetPort: http               # Pod 포트와 매핑
  - name: grpc
    port: 50051
    targetPort: grpc
```

#### **LoadBalancer Service**
```yaml
metadata:
  annotations:
    service.beta.kubernetes.io/aws-load-balancer-type: "nlb"
spec:
  type: LoadBalancer               # 외부 로드밸런서 생성
  ports:
  - name: http
    port: 80                       # 표준 HTTP 포트
    targetPort: http
```

#### **NodePort Service**
```yaml
spec:
  type: NodePort                   # 노드 포트로 직접 접근
  ports:
  - name: http
    port: 8080
    targetPort: http
    nodePort: 30080                # 고정 노드 포트
```

**용도별 구분**:
- **ClusterIP**: 클러스터 내부 서비스 간 통신
- **LoadBalancer**: 프로덕션 외부 접근
- **NodePort**: 개발/테스트 환경

---

### 🚪 `ingress.yaml`
**목적**: 외부 트래픽 라우팅 및 SSL 종료

#### **기본 Ingress**
```yaml
metadata:
  annotations:
    kubernetes.io/ingress.class: "nginx"
    nginx.ingress.kubernetes.io/enable-cors: "true"
    nginx.ingress.kubernetes.io/rate-limit: "100"
spec:
  rules:
  - host: logpilot.local           # 로컬 개발용 도메인
    http:
      paths:
      - path: /
        pathType: Prefix
        backend:
          service:
            name: logpilot-all
            port:
              number: 8080
```

#### **TLS Ingress**
```yaml
metadata:
  annotations:
    cert-manager.io/cluster-issuer: "letsencrypt-prod"
spec:
  tls:
  - hosts:
    - logpilot.example.com
    secretName: logpilot-tls-secret  # 자동 생성된 TLS 인증서
```

#### **Gateway API** (차세대 Ingress)
```yaml
apiVersion: gateway.networking.k8s.io/v1beta1
kind: Gateway
metadata:
  name: logpilot-gateway
spec:
  gatewayClassName: nginx
  listeners:
  - name: https
    port: 443
    protocol: HTTPS
    tls:
      mode: Terminate
      certificateRefs:
      - name: logpilot-tls-secret
```

**기능**:
- **도메인 라우팅**: 호스트명 기반 라우팅
- **SSL/TLS 종료**: HTTPS 자동 처리
- **Rate Limiting**: DDoS 방지
- **CORS 설정**: 브라우저 보안 정책

---

### 📊 `monitoring.yaml`
**목적**: 프로덕션 모니터링 및 알림

#### **ServiceMonitor** (Prometheus)
```yaml
apiVersion: monitoring.coreos.com/v1
kind: ServiceMonitor
metadata:
  name: logpilot-metrics
spec:
  selector:
    matchLabels:
      app: logpilot-all
  endpoints:
  - port: management
    path: /actuator/prometheus        # Spring Boot Metrics
    interval: 30s
```

#### **PrometheusRule** (알림 규칙)
```yaml
spec:
  groups:
  - name: logpilot.rules
    rules:
    - alert: LogPilotHighMemoryUsage
      expr: container_memory_usage_bytes{namespace="logpilot"} / container_spec_memory_limit_bytes > 0.8
      for: 5m
      labels:
        severity: warning
      annotations:
        summary: "LogPilot high memory usage"
```

#### **Grafana Dashboard**
```yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: logpilot-dashboard
  labels:
    grafana_dashboard: "1"           # Grafana 자동 임포트
data:
  logpilot-dashboard.json: |
    {
      "dashboard": {
        "title": "LogPilot Monitoring Dashboard",
        "panels": [
          {
            "title": "Request Rate",
            "targets": [
              {
                "expr": "rate(http_requests_total{namespace=\"logpilot\"}[5m])"
              }
            ]
          }
        ]
      }
    }
```

**모니터링 지표**:
- **애플리케이션 메트릭**: HTTP 요청률, 응답시간, 에러율
- **인프라 메트릭**: CPU, 메모리, 네트워크 사용량
- **비즈니스 메트릭**: 로그 처리량, 저장소 사용량
- **알림**: 임계값 초과 시 Slack/Email 알림

---

### 🎛️ `kustomization.yaml`
**목적**: 환경별 설정 관리

```yaml
apiVersion: kustomize.config.k8s.io/v1beta1
kind: Kustomization

# 공통 라벨
commonLabels:
  app.kubernetes.io/name: logpilot
  app.kubernetes.io/version: v1.0.0
  app.kubernetes.io/managed-by: kustomize

# 이미지 태그 관리
images:
  - name: logpilot
    newTag: latest                   # 환경별로 다른 태그 사용

# ConfigMap 생성
configMapGenerator:
  - name: logpilot-env-config
    literals:
      - LOGPILOT_STORAGE_TYPE=sqlite
      - SPRING_PROFILES_ACTIVE=all

# 환경별 패치
patches:
  - target:
      kind: Deployment
      name: logpilot-all
    patch: |-
      - op: add
        path: /spec/template/spec/containers/0/envFrom
        value:
          - configMapRef:
              name: logpilot-env-config
```

**Kustomize 구조 예시**:
```
overlays/
├── development/
│   ├── kustomization.yaml
│   └── resources.yaml
├── staging/
│   ├── kustomization.yaml
│   └── resources.yaml
└── production/
    ├── kustomization.yaml
    └── resources.yaml
```

---

## 🏗️ 배포 전략

### 1. **Blue-Green 배포**
```bash
# Blue 환경 배포
kubectl apply -k overlays/production-blue/

# 트래픽 검증 후 Green으로 전환
kubectl patch service logpilot-all -p '{"spec":{"selector":{"version":"green"}}}'
```

### 2. **Canary 배포**
```yaml
# 5% 트래픽을 새 버전으로
apiVersion: argoproj.io/v1alpha1
kind: Rollout
metadata:
  name: logpilot-canary
spec:
  strategy:
    canary:
      steps:
      - setWeight: 5
      - pause: {}
      - setWeight: 50
      - pause: {duration: 10m}
      - setWeight: 100
```

### 3. **Rolling Update**
```yaml
spec:
  strategy:
    type: RollingUpdate
    rollingUpdate:
      maxUnavailable: 25%          # 동시에 종료할 수 있는 Pod 비율
      maxSurge: 25%                # 동시에 생성할 수 있는 추가 Pod 비율
```

---

## 🔧 운영 가이드

### **로그 및 디버깅**
```bash
# 실시간 로그 확인
kubectl logs -f deployment/logpilot-all -n logpilot

# 이전 컨테이너 로그
kubectl logs -p <pod-name> -n logpilot

# Pod 상세 정보
kubectl describe pod <pod-name> -n logpilot

# 컨테이너 내부 접근
kubectl exec -it <pod-name> -n logpilot -- /bin/bash
```

### **성능 모니터링**
```bash
# 리소스 사용량 확인
kubectl top pods -n logpilot
kubectl top nodes

# 이벤트 확인
kubectl get events -n logpilot --sort-by='.lastTimestamp'
```

### **백업 및 복구**
```bash
# 설정 백업
kubectl get configmap -n logpilot -o yaml > backup-configmaps.yaml

# 데이터 백업 (PVC)
kubectl create job --from=cronjob/backup-job backup-manual -n logpilot
```

---

## 🎯 Best Practices

### 1. **리소스 관리**
- **Requests vs Limits**: 적절한 리소스 요청/제한 설정
- **QoS Classes**: Guaranteed, Burstable, BestEffort 클래스 이해
- **LimitRange**: 네임스페이스 레벨 리소스 제한

### 2. **보안**
- **ServiceAccount**: 최소 권한 원칙
- **NetworkPolicy**: 네트워크 트래픽 제한
- **PodSecurityPolicy**: Pod 보안 정책
- **Secrets 관리**: 민감 정보 암호화

### 3. **고가용성**
- **PodDisruptionBudget**: 자발적 중단 최소화
- **Anti-Affinity**: Pod 분산 배치
- **Multi-AZ**: 여러 가용 영역 활용

### 4. **모니터링**
- **SLI/SLO**: 서비스 수준 지표/목표 설정
- **알림 최적화**: 알림 피로도 방지
- **대시보드**: 운영팀을 위한 직관적 시각화
