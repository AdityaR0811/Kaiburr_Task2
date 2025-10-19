#!/usr/bin/env bash
# Kaiburr Task 2 — Kind Development Bootstrap
# Author: Aditya R
#
# This script sets up a complete development environment with kind cluster

set -euo pipefail

CLUSTER_NAME="${KIND_CLUSTER_NAME:-kaiburr}"
NAMESPACE="kaiburr"
EXECUTOR_IMAGE="kaiburr-executor:dev"

echo "========================================="
echo " Kaiburr Task 2 — Kind Bootstrap"
echo " Author: Aditya R"
echo "========================================="

# Check prerequisites
command -v kind >/dev/null 2>&1 || { echo "Error: kind not found. Install from https://kind.sigs.k8s.io/"; exit 1; }
command -v kubectl >/dev/null 2>&1 || { echo "Error: kubectl not found"; exit 1; }
command -v docker >/dev/null 2>&1 || { echo "Error: docker not found"; exit 1; }

# Create kind cluster
echo "→ Creating kind cluster: $CLUSTER_NAME..."
if kind get clusters | grep -q "^${CLUSTER_NAME}$"; then
    echo "  Cluster already exists, skipping creation"
else
    kind create cluster --name "$CLUSTER_NAME" --config - <<EOF
kind: Cluster
apiVersion: kind.x-k8s.io/v1alpha4
nodes:
- role: control-plane
  image: kindest/node:v1.28.0
EOF
    echo "  ✓ Cluster created"
fi

# Set kubectl context
echo "→ Setting kubectl context..."
kubectl cluster-info --context "kind-${CLUSTER_NAME}"

# Create namespace
echo "→ Creating namespace: $NAMESPACE..."
kubectl create namespace "$NAMESPACE" --dry-run=client -o yaml | kubectl apply -f -
echo "  ✓ Namespace ready"

# Apply Kubernetes manifests
echo "→ Applying Kubernetes manifests..."
kubectl apply -f deploy/k8s/ -n "$NAMESPACE"
echo "  ✓ Manifests applied"

# Build executor image
echo "→ Building executor image..."
cd executor
docker build -t "$EXECUTOR_IMAGE" .
cd ..
echo "  ✓ Executor image built"

# Load image into kind
echo "→ Loading executor image into kind..."
kind load docker-image "$EXECUTOR_IMAGE" --name "$CLUSTER_NAME"
echo "  ✓ Image loaded"

# Start MongoDB (if not running)
echo "→ Starting MongoDB container..."
if docker ps --format '{{.Names}}' | grep -q "mongo-kaiburr"; then
    echo "  MongoDB already running"
else
    docker run -d \
        --name mongo-kaiburr \
        -p 27017:27017 \
        mongo:5.0
    echo "  ✓ MongoDB started"
fi

# Wait for resources
echo "→ Waiting for Kubernetes resources..."
kubectl wait --for=condition=ready --timeout=60s \
    -n "$NAMESPACE" \
    serviceaccount/kaiburr-runner

echo ""
echo "========================================="
echo " ✓ Bootstrap Complete!"
echo "========================================="
echo ""
echo "Next steps:"
echo "  1. Build application:"
echo "     mvn clean package"
echo ""
echo "  2. Run application:"
echo "     SPRING_PROFILES_ACTIVE=k8s \\"
echo "     MONGODB_URI=mongodb://localhost:27017/kaiburrdb \\"
echo "     K8S_NAMESPACE=$NAMESPACE \\"
echo "     K8S_EXECUTOR_IMAGE=$EXECUTOR_IMAGE \\"
echo "     mvn spring-boot:run"
echo ""
echo "  3. Test endpoints:"
echo "     ./scripts/demo-commands.sh"
echo ""
echo "  4. View resources:"
echo "     kubectl get all -n $NAMESPACE"
echo ""
