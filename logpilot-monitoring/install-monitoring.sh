#!/bin/bash

# LogPilot Monitoring Stack Installation Script
# Deploys Prometheus and Grafana for LogPilot metrics visualization.

set -e

# Configuration
NAMESPACE="logpilot"
MONITORING_DIR="logpilot-monitoring/k8s"

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Helper Functions
log_info() { echo -e "${BLUE}[INFO]${NC} $1"; }
log_success() { echo -e "${GREEN}[SUCCESS]${NC} $1"; }
log_error() { echo -e "${RED}[ERROR]${NC} $1"; }

check_prereqs() {
    log_info "Checking prerequisites..."
    if ! command -v kubectl &> /dev/null; then
        log_error "kubectl is not installed."
        exit 1
    fi
}

deploy_prometheus() {
    log_info "Deploying Prometheus..."
    
    # Create monitoring directory structure if it only exists in K8S context
    # Note: Assuming script is run from project root
    
    if [ ! -d "$MONITORING_DIR/prometheus" ]; then
        log_error "Prometheus manifests not found at $MONITORING_DIR/prometheus"
        exit 1
    fi

    kubectl apply -f "$MONITORING_DIR/prometheus/"
    log_success "Prometheus deployed."
}

deploy_grafana() {
    log_info "Deploying Grafana..."

    if [ ! -d "$MONITORING_DIR/grafana" ]; then
        log_error "Grafana manifests not found at $MONITORING_DIR/grafana"
        exit 1
    fi

    # Create Dashboards ConfigMap from local files first
    log_info "Updating Dashboards ConfigMap..."
    # Execute the dashboard creation script
    ./logpilot-monitoring/scripts/create-dashboard-configmap.sh
    
    kubectl apply -f "$MONITORING_DIR/grafana/"
    log_success "Grafana deployed."
}

verify_deployment() {
    log_info "Verifying deployment..."
    kubectl rollout status deployment/prometheus -n "$NAMESPACE" --timeout=60s
    kubectl rollout status deployment/grafana -n "$NAMESPACE" --timeout=60s
    log_success "Monitoring stack is ready!"
}

show_access_info() {
    echo ""
    log_info "=== Access Information ==="
    echo "Prometheus: http://localhost:9090 (via port-forward)"
    echo "Grafana:    http://localhost:3000 (via port-forward)"
    echo "Default Grafana Login: admin / admin"
    echo ""
    echo "To access locally, run:"
    echo "  kubectl port-forward svc/prometheus 9090:9090 -n $NAMESPACE &"
    echo "  kubectl port-forward svc/grafana 3000:3000 -n $NAMESPACE &"
    echo ""
}

# Main
main() {
    echo "=== LogPilot Monitoring Stack Installer ==="
    check_prereqs
    
    # Ensure Namespace exists (LogPilot should be deployed first)
    if ! kubectl get namespace "$NAMESPACE" &> /dev/null; then
        log_error "Namespace '$NAMESPACE' does not exist. Please deploy LogPilot core first."
        exit 1
    fi

    deploy_prometheus
    deploy_grafana
    verify_deployment
    show_access_info
}

main "$@"
