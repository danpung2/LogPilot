#!/bin/bash

# LogPilot Kubernetes Deployment Script
# This script helps deploy LogPilot to Kubernetes cluster

set -e  # Exit on any error

# Configuration
NAMESPACE="logpilot"
DEPLOYMENT_MODE=""
IMAGE_TAG="latest"
CONTEXT=""

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Function to print colored output
print_info() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

print_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

print_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# Function to check if kubectl is available
check_kubectl() {
    if ! command -v kubectl &> /dev/null; then
        print_error "kubectl is not installed or not in PATH"
        exit 1
    fi
}

# Function to check if cluster is accessible
check_cluster() {
    if ! kubectl cluster-info &> /dev/null; then
        print_error "Cannot connect to Kubernetes cluster"
        print_info "Make sure you have a valid kubeconfig and cluster is running"
        exit 1
    fi

    print_success "Connected to Kubernetes cluster"
    kubectl cluster-info
}

# Function to build Docker images
build_images() {
    print_info "Building Docker images..."

    case $DEPLOYMENT_MODE in
        "all")
            print_info "Building all-in-one image..."
            docker build -t logpilot:$IMAGE_TAG .
            ;;
        "rest")
            print_info "Building REST-only image..."
            docker build -f Dockerfile.rest -t logpilot-rest:$IMAGE_TAG .
            ;;
        "grpc")
            print_info "Building gRPC-only image..."
            docker build -f Dockerfile.grpc -t logpilot-grpc:$IMAGE_TAG .
            ;;
        "split")
            print_info "Building both REST and gRPC images..."
            docker build -f Dockerfile.rest -t logpilot-rest:$IMAGE_TAG .
            docker build -f Dockerfile.grpc -t logpilot-grpc:$IMAGE_TAG .
            ;;
        *)
            print_error "Unknown deployment mode: $DEPLOYMENT_MODE"
            exit 1
            ;;
    esac

    print_success "Docker images built successfully"
}

# Function to load images to kind cluster (if using kind)
load_images_to_kind() {
    if command -v kind &> /dev/null && kind get clusters | grep -q "kind"; then
        print_info "Detected kind cluster, loading images..."

        case $DEPLOYMENT_MODE in
            "all")
                kind load docker-image logpilot:$IMAGE_TAG
                ;;
            "rest")
                kind load docker-image logpilot-rest:$IMAGE_TAG
                ;;
            "grpc")
                kind load docker-image logpilot-grpc:$IMAGE_TAG
                ;;
            "split")
                kind load docker-image logpilot-rest:$IMAGE_TAG
                kind load docker-image logpilot-grpc:$IMAGE_TAG
                ;;
        esac

        print_success "Images loaded to kind cluster"
    fi
}

# Function to create namespace
create_namespace() {
    print_info "Creating namespace: $NAMESPACE"

    if kubectl get namespace $NAMESPACE &> /dev/null; then
        print_warning "Namespace $NAMESPACE already exists"
    else
        kubectl apply -f k8s/namespace.yaml
        print_success "Namespace created"
    fi
}

# Function to deploy ConfigMaps
deploy_configmaps() {
    print_info "Deploying ConfigMaps..."
    kubectl apply -f k8s/configmap.yaml
    print_success "ConfigMaps deployed"
}

# Function to deploy services
deploy_services() {
    print_info "Deploying Services..."
    kubectl apply -f k8s/service.yaml
    print_success "Services deployed"
}

# Function to deploy applications
deploy_applications() {
    print_info "Deploying applications in mode: $DEPLOYMENT_MODE"

    case $DEPLOYMENT_MODE in
        "all")
            kubectl apply -f k8s/deployment-all.yaml
            ;;
        "rest")
            kubectl apply -f k8s/deployment-rest.yaml
            ;;
        "grpc")
            kubectl apply -f k8s/deployment-grpc.yaml
            ;;
        "split")
            kubectl apply -f k8s/deployment-rest.yaml
            kubectl apply -f k8s/deployment-grpc.yaml
            ;;
    esac

    print_success "Applications deployed"
}

# Function to deploy ingress
deploy_ingress() {
    print_info "Deploying Ingress..."

    # Check if ingress controller is available
    if kubectl get ingressclass &> /dev/null; then
        kubectl apply -f k8s/ingress.yaml
        print_success "Standard Ingress deployed"

        # Optionally deploy Gateway API resources
        if kubectl get crd gateways.gateway.networking.k8s.io &> /dev/null; then
            print_info "Gateway API CRDs found, deploying Gateway resources..."
            kubectl apply -f k8s/gateway-api.yaml
            print_success "Gateway API resources deployed"
        else
            print_warning "Gateway API CRDs not found, skipping Gateway deployment"
            print_info "To use Gateway API, install CRDs first:"
            print_info "  kubectl apply -f https://github.com/kubernetes-sigs/gateway-api/releases/download/v0.8.1/standard-install.yaml"
            print_info "  Then deploy Gateway resources: kubectl apply -f k8s/gateway-api.yaml"
        fi
    else
        print_warning "No Ingress controller found, skipping Ingress deployment"
        print_info "You can install NGINX Ingress controller with:"
        print_info "  kubectl apply -f https://raw.githubusercontent.com/kubernetes/ingress-nginx/controller-v1.8.2/deploy/static/provider/cloud/deploy.yaml"
    fi
}

# Function to wait for deployments
wait_for_deployments() {
    print_info "Waiting for deployments to be ready..."

    case $DEPLOYMENT_MODE in
        "all")
            kubectl rollout status deployment/logpilot-all -n $NAMESPACE --timeout=300s
            ;;
        "rest")
            kubectl rollout status deployment/logpilot-rest -n $NAMESPACE --timeout=300s
            ;;
        "grpc")
            kubectl rollout status deployment/logpilot-grpc -n $NAMESPACE --timeout=300s
            ;;
        "split")
            kubectl rollout status deployment/logpilot-rest -n $NAMESPACE --timeout=300s
            kubectl rollout status deployment/logpilot-grpc -n $NAMESPACE --timeout=300s
            ;;
    esac

    print_success "All deployments are ready"
}

# Function to show deployment status
show_status() {
    print_info "Deployment Status:"
    echo ""

    print_info "=== Namespaces ==="
    kubectl get namespaces | grep logpilot

    print_info "=== Pods ==="
    kubectl get pods -n $NAMESPACE -o wide

    print_info "=== Services ==="
    kubectl get services -n $NAMESPACE

    print_info "=== Ingress ==="
    kubectl get ingress -n $NAMESPACE 2>/dev/null || print_warning "No Ingress found"

    print_info "=== PersistentVolumeClaims ==="
    kubectl get pvc -n $NAMESPACE

    echo ""
    print_info "=== Access Information ==="

    case $DEPLOYMENT_MODE in
        "all")
            print_info "REST API: http://logpilot.local (via Ingress) or kubectl port-forward svc/logpilot-all 8080:8080 -n $NAMESPACE"
            print_info "gRPC Service: logpilot.local:50051 (via Ingress) or kubectl port-forward svc/logpilot-all 50051:50051 -n $NAMESPACE"
            ;;
        "rest")
            print_info "REST API: http://logpilot-rest.local (via Ingress) or kubectl port-forward svc/logpilot-rest 8080:8080 -n $NAMESPACE"
            ;;
        "grpc")
            print_info "gRPC Service: kubectl port-forward svc/logpilot-grpc 50051:50051 -n $NAMESPACE"
            ;;
        "split")
            print_info "REST API: http://logpilot-rest.local (via Ingress) or kubectl port-forward svc/logpilot-rest 8080:8080 -n $NAMESPACE"
            print_info "gRPC Service: kubectl port-forward svc/logpilot-grpc 50051:50051 -n $NAMESPACE"
            ;;
    esac

    echo ""
    print_info "Health Check: kubectl port-forward svc/logpilot-* 8081:8081 -n $NAMESPACE"
    print_info "Logs: kubectl logs -f deployment/logpilot-* -n $NAMESPACE"
}

# Function to cleanup deployment
cleanup() {
    print_info "Cleaning up LogPilot deployment..."

    print_warning "This will delete all LogPilot resources from the cluster"
    read -p "Are you sure? (y/N): " -n 1 -r
    echo

    if [[ $REPLY =~ ^[Yy]$ ]]; then
        kubectl delete namespace $NAMESPACE --ignore-not-found=true
        print_success "LogPilot deployment cleaned up"
    else
        print_info "Cleanup cancelled"
    fi
}

# Function to show help
show_help() {
    echo "LogPilot Kubernetes Deployment Script"
    echo ""
    echo "Usage: $0 [OPTIONS] COMMAND"
    echo ""
    echo "Commands:"
    echo "  deploy       Deploy LogPilot to Kubernetes"
    echo "  build        Build Docker images only"
    echo "  status       Show deployment status"
    echo "  cleanup      Remove LogPilot from cluster"
    echo "  help         Show this help message"
    echo ""
    echo "Options:"
    echo "  -m, --mode MODE       Deployment mode: all, rest, grpc, split (default: all)"
    echo "  -t, --tag TAG         Docker image tag (default: latest)"
    echo "  -n, --namespace NS    Kubernetes namespace (default: logpilot)"
    echo "  -c, --context CTX     Kubernetes context to use"
    echo "  --no-build           Skip building Docker images"
    echo "  --no-ingress         Skip deploying Ingress"
    echo ""
    echo "Examples:"
    echo "  $0 deploy                           # Deploy all-in-one mode"
    echo "  $0 -m rest deploy                   # Deploy REST-only mode"
    echo "  $0 -m split deploy                  # Deploy both REST and gRPC separately"
    echo "  $0 -t v1.0.0 deploy                 # Deploy with specific image tag"
    echo "  $0 --no-build deploy                # Deploy without building images"
    echo "  $0 status                           # Show current status"
    echo "  $0 cleanup                          # Remove deployment"
    echo ""
}

# Parse command line arguments
SKIP_BUILD=false
SKIP_INGRESS=false

while [[ $# -gt 0 ]]; do
    case $1 in
        -m|--mode)
            DEPLOYMENT_MODE="$2"
            shift 2
            ;;
        -t|--tag)
            IMAGE_TAG="$2"
            shift 2
            ;;
        -n|--namespace)
            NAMESPACE="$2"
            shift 2
            ;;
        -c|--context)
            CONTEXT="$2"
            shift 2
            ;;
        --no-build)
            SKIP_BUILD=true
            shift
            ;;
        --no-ingress)
            SKIP_INGRESS=true
            shift
            ;;
        deploy|build|status|cleanup|help)
            COMMAND="$1"
            shift
            ;;
        *)
            print_error "Unknown option: $1"
            show_help
            exit 1
            ;;
    esac
done

# Set default values
DEPLOYMENT_MODE=${DEPLOYMENT_MODE:-"all"}
COMMAND=${COMMAND:-"help"}

# Set kubectl context if specified
if [[ -n "$CONTEXT" ]]; then
    kubectl config use-context "$CONTEXT"
fi

# Main execution
case $COMMAND in
    "deploy")
        print_info "Starting LogPilot deployment to Kubernetes..."
        print_info "Mode: $DEPLOYMENT_MODE"
        print_info "Image Tag: $IMAGE_TAG"
        print_info "Namespace: $NAMESPACE"
        echo ""

        check_kubectl
        check_cluster

        if [[ "$SKIP_BUILD" != "true" ]]; then
            build_images
            load_images_to_kind
        fi

        create_namespace
        deploy_configmaps
        deploy_services
        deploy_applications

        if [[ "$SKIP_INGRESS" != "true" ]]; then
            deploy_ingress
        fi

        wait_for_deployments
        show_status

        print_success "LogPilot deployed successfully!"
        ;;
    "build")
        print_info "Building Docker images..."
        build_images
        load_images_to_kind
        print_success "Images built successfully!"
        ;;
    "status")
        check_kubectl
        check_cluster
        show_status
        ;;
    "cleanup")
        check_kubectl
        check_cluster
        cleanup
        ;;
    "help"|"")
        show_help
        ;;
    *)
        print_error "Unknown command: $COMMAND"
        show_help
        exit 1
        ;;
esac