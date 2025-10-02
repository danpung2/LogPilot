#!/bin/bash
# Script to create Grafana dashboard ConfigMap from JSON files

set -e

# Get the directory where this script is located
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
MONITORING_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"

NAMESPACE="logpilot"
CONFIGMAP_NAME="grafana-dashboards"
DASHBOARD_DIR="$MONITORING_DIR/dashboards"

echo "Creating Grafana Dashboard ConfigMap..."

# Delete existing ConfigMap if it exists
kubectl delete configmap $CONFIGMAP_NAME -n $NAMESPACE --ignore-not-found=true

# Create ConfigMap from dashboard JSON files
kubectl create configmap $CONFIGMAP_NAME \
  --from-file=$DASHBOARD_DIR/logpilot-overview.json \
  --from-file=$DASHBOARD_DIR/logpilot-performance.json \
  --from-file=$DASHBOARD_DIR/logpilot-business.json \
  --from-file=$DASHBOARD_DIR/logpilot-infrastructure.json \
  -n $NAMESPACE

# Label the ConfigMap
kubectl label configmap $CONFIGMAP_NAME \
  app=grafana \
  component=monitoring \
  -n $NAMESPACE

echo "✅ Dashboard ConfigMap created successfully!"
echo ""
echo "Dashboards included:"
echo "  - logpilot-overview.json"
echo "  - logpilot-performance.json"
echo "  - logpilot-business.json"
echo "  - logpilot-infrastructure.json"
