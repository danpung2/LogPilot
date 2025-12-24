# LogPilot Kubernetes Deployment Guide

This document provides a complete guide for deploying the **LogPilot Event Broker** to a Kubernetes cluster.

## Prerequisites

### Required Tools

- **Docker**: For building container images
- **minikube**: Local Kubernetes cluster
- **kubectl**: Kubernetes CLI
- **Java 17+**: For building the application (optional if using Docker)

### Installation Commands (macOS)

```bash
# Install via Homebrew
brew install docker minikube kubectl

# After starting Docker Desktop
minikube start --driver=docker
```

## Local Development Environment (minikube)

### 1. Setup minikube Cluster

```bash
# Start minikube (using Docker driver)
minikube start --driver=docker --disable-metrics=true --addons=ingress

# Check cluster status
minikube status
kubectl get nodes
```

### 2. Automated Deployment (Recommended)

The simplest way is to run the following command from the project root:

```bash
./k8s-deploy.sh
```

This script automatically performs the following tasks:
- Checks and starts minikube status
- Builds Docker image
- Loads image into minikube
- Deploys Kubernetes resources
- Waits for Pod status
- Prints service access information

### 3. Manual Deployment

If you prefer to deploy manually step-by-step:

```bash
# 1. Build Docker image
docker build -t logpilot:latest .

# 2. Load image into minikube
minikube image load logpilot:latest

# 3. Deploy Kubernetes resources
kubectl apply -f k8s/namespace.yaml
kubectl apply -f k8s/configmap.yaml
kubectl apply -f k8s/deployment-all.yaml
kubectl apply -f k8s/service.yaml

# 4. Check deployment status
kubectl get pods -n logpilot
kubectl get svc -n logpilot
```

## Kubernetes Manifest Structure

### Key Manifest Files

#### 1. Namespace (namespace.yaml)
```yaml
# Creates logpilot and logpilot-system namespaces
apiVersion: v1
kind: Namespace
metadata:
  name: logpilot
```

#### 2. ConfigMap (configmap.yaml)
Contains application configuration, providing:
- Spring Boot settings
- Log Storage settings (SQLite)
- gRPC/REST port settings
- Management endpoint settings

#### 3. Deployment (deployment-all.yaml)
- **Image**: `logpilot:latest`
- **Ports**: 8080 (HTTP), 50051 (gRPC), 8081 (Management)
- **Volumes**: Mounts ConfigMap and PersistentVolume
- **Health Checks**: Liveness, Readiness, Startup probes configured

#### 4. Service (service.yaml)
Provides various service types:
- **ClusterIP**: Internal cluster communication
- **NodePort**: External access for dev/testing
- **LoadBalancer**: For production environments

## Deployment Methods

### Deployment Selection by Environment

1. **Unified Server** (REST + gRPC): `deployment-all.yaml` (Default)
2. **REST Only**: `deployment-rest.yaml`
3. **gRPC Only**: `deployment-grpc.yaml`

### Deployment Commands

```bash
# Default unified deployment
kubectl apply -f k8s/deployment-all.yaml

# Or select specific deployment
kubectl apply -f k8s/deployment-rest.yaml
kubectl apply -f k8s/deployment-grpc.yaml
```

## Service Access

LogPilot offers multiple ways to access the service.

### Method 1: Port Forwarding

```bash
# Automated port forwarding script
./k8s-port-forward.sh

# Manual port forwarding
kubectl port-forward -n logpilot svc/logpilot-all 8080:8080 &
kubectl port-forward -n logpilot svc/logpilot-all 50051:50051 &
kubectl port-forward -n logpilot svc/logpilot-all 8081:8081 &
```

### Method 2: minikube tunnel (Using LoadBalancer)

```bash
# Run tunnel script
./k8s-tunnel.sh

# Or manually
minikube tunnel

# Check LoadBalancer External IP
kubectl get svc logpilot-loadbalancer -n logpilot
```

### Method 3: Ingress (Domain-based Access)

```bash
# Deploy Ingress
kubectl apply -f k8s/ingress.yaml

# Add domain to /etc/hosts (manual)
echo "127.0.0.1 logpilot.local" >> /etc/hosts

# Access
curl http://logpilot.local/api/logs
```

### Method 4: Deploy Consumer/Producer to Kubernetes

```bash
# Build Client Image (if needed)
docker build -f Dockerfile.client -t logpilot-client:latest .
minikube image load logpilot-client:latest

# Deploy Consumer/Producer
kubectl apply -f k8s/consumer-producer-deployment.yaml
```

### Method 5: Direct NodePort Access

```bash
# Check minikube IP
MINIKUBE_IP=$(minikube ip)

# Direct access via NodePort
curl -X POST http://$MINIKUBE_IP:30080/api/logs \
  -H 'Content-Type: application/json' \
  -d '{"channel":"test","level":"INFO","message":"via nodeport"}'

# gRPC Access (from consumer/producer)
java -jar logpilot-client.jar --grpc.server.address=$MINIKUBE_IP:30051
```

### Access URLs (When Port Forwarding is Active)

When running `./k8s-port-forward.sh` or `kubectl port-forward`, you can access via local addresses:

- **REST API**: `http://localhost:8080/api`
- **gRPC API**: `localhost:50051`
- **Health Check**: `http://localhost:8080/actuator/health` (Note: Management endpoints might be accessible on 8080 during port forwarding, or depending on logpilot-all service forwarding)
- **Metrics**: `http://localhost:8080/actuator/metrics`
- **Prometheus**: `http://localhost:8080/actuator/prometheus`

*Note: In minikube (Docker driver) environment, you cannot access NodePort or ClusterIP directly without port forwarding or tunneling.*

### API Usage Examples

Simple Health Check example to verify successful deployment. For detailed API usage and request field information, refer to [README.md](README.md) or [README.ko.md](README.ko.md).

```bash
# Health Check
curl http://localhost:8080/actuator/health
```

## Monitoring and Debugging

### 1. Check Pod Status

```bash
# List Pods
kubectl get pods -n logpilot

# Pod Details
kubectl describe pod <pod-name> -n logpilot

# Check Pod Logs
kubectl logs <pod-name> -n logpilot

# Stream Live Logs
kubectl logs -f <pod-name> -n logpilot
```

### 2. Check Service Status

```bash
# List Services
kubectl get svc -n logpilot

# Check Endpoints
kubectl get endpoints -n logpilot
```

### 3. Network Debugging

```bash
# Access Pod Shell
kubectl exec -it <pod-name> -n logpilot -- /bin/bash

# Test Network Connection
kubectl exec -it <pod-name> -n logpilot -- curl http://localhost:8080/actuator/health
```

### 4. Check Events

```bash
# List Namespace Events
kubectl get events -n logpilot --sort-by='.lastTimestamp'
```

## Advanced Features

### 1. Ingress Configuration (Optional)

```bash
# Apply Ingress
kubectl apply -f k8s/ingress.yaml

# Check Ingress Status
kubectl get ingress -n logpilot
```

### 2. Monitoring Configuration

```bash
# Deploy Monitoring Resources
kubectl apply -f k8s/monitoring.yaml
```

### 3. Scaling

```bash
# Adjust Replica Count
kubectl scale deployment/logpilot-all --replicas=3 -n logpilot

# Configure Autoscaling
kubectl autoscale deployment/logpilot-all --cpu-percent=50 --min=1 --max=10 -n logpilot
```

## Configuration Changes

### Modify ConfigMap

```bash
# Edit ConfigMap
kubectl edit configmap logpilot-config -n logpilot

# Or modify file and re-apply
kubectl apply -f k8s/configmap.yaml

# Restart Pod (Apply Settings)
kubectl rollout restart deployment/logpilot-all -n logpilot
```

### Add Environment Variables

You can add environment variables in deployment.yaml:

```yaml
env:
- name: SPRING_PROFILES_ACTIVE
  value: "all"
- name: LOGPILOT_CUSTOM_SETTING
  value: "custom-value"
```

## Cleanup

### 1. Delete Application

```bash
# Delete Deployed Resources
kubectl delete -f k8s/deployment-all.yaml
kubectl delete -f k8s/service.yaml
kubectl delete -f k8s/configmap.yaml

# Or delete entire namespace
kubectl delete namespace logpilot
```

### 2. minikube Cleanup

```bash
# Stop minikube cluster
minikube stop

# Delete minikube cluster
minikube delete
```

## Debugging

```bash
# Check Entire Cluster Status
kubectl get all -n logpilot

# Check Resource Usage
kubectl top pods -n logpilot

# Check Configuration
kubectl get configmap logpilot-config -n logpilot -o yaml
```
