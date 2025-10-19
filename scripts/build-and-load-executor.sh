#!/usr/bin/env bash
# Build and load executor image into kind
# Author: Aditya R

set -euo pipefail

CLUSTER_NAME="${1:-kaiburr}"
IMAGE_NAME="kaiburr-executor:dev"

echo "Building executor image: $IMAGE_NAME"
cd executor
docker build -t "$IMAGE_NAME" .
cd ..

echo "Loading image into kind cluster: $CLUSTER_NAME"
kind load docker-image "$IMAGE_NAME" --name "$CLUSTER_NAME"

echo "✓ Image loaded successfully"

# Verify
echo "Verifying image in kind..."
docker exec -it "${CLUSTER_NAME}-control-plane" crictl images | grep kaiburr-executor || echo "Warning: Image not found in crictl"
