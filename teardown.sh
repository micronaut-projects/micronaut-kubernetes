#!/bin/bash
set -ex

#
# Defaults
K8S_DEFAULT_VERSION="1.19"

#
# Resolve K8s version
K8S_VERSION=${K8S_VERSION:=$K8S_DEFAULT_VERSION}
echo "K8S_VERSION = $K8S_VERSION"

#
# Create a cluster
KIND_CLUSTER=$(echo $K8S_VERSION | tr -cd '[:alnum:]')
KIND_CLUSTER_NAME="k8s${KIND_CLUSTER}java${JAVA_VERSION}"
./kind delete cluster  --name ${KIND_CLUSTER_NAME} || true

#
# Stop kubernetes API proxy
pkill -9 kubectl || true