#!/bin/bash
set -e

echo "📋 Step 1: Minikube 상태 확인 및 시작"
if ! minikube status | grep -q "apiserver: Running"; then
  echo "⚠️  Minikube가 실행 중이지 않습니다. 시작합니다..."
  minikube start
else
  echo "✅ Minikube가 실행 중입니다."
fi

echo ""
echo "📋 Step 2: kubectl context 업데이트"
minikube update-context

echo ""
echo "📋 Step 3: 클러스터 연결 확인"
kubectl cluster-info

echo ""
echo "📋 Step 4: LogPilot 네임스페이스 확인/생성"
if ! kubectl get namespace logpilot &>/dev/null; then
  echo "⚠️  logpilot 네임스페이스가 없습니다. 생성합니다..."
  kubectl create namespace logpilot
else
  echo "✅ logpilot 네임스페이스가 존재합니다."
fi

echo ""
echo "📋 Step 5: Prometheus 배포"
kubectl apply -f logpilot-monitoring/k8s/prometheus/

echo ""
echo "📋 Step 6: 배포 대기 (최대 5분)"
kubectl wait --for=condition=ready pod -l app=prometheus -n logpilot --timeout=300s

echo ""
echo "✅ Prometheus 배포 완료!"
echo ""
echo "📊 Prometheus UI 접근:"
echo "kubectl port-forward svc/prometheus 9090:9090 -n logpilot"
echo "http://localhost:9090"