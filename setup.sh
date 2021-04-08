#!/bin/bash
set -ex

#
# Defaults
K8S_DEFAULT_VERSION="1.19"
KUBECTL_DEFAULT_VERSION="v1.19.2"
KIND_DEFAULT_VERSION="v0.9.0"
KIND_NODE_IMAGE_K8S_DEFAULT_VERSION="v1.19.1@sha256:98cf5288864662e37115e362b23e4369c8c4a408f99cbc06e58ac30ddc721600"

#
# Resolve K8s version
K8S_VERSION=${K8S_VERSION:=$K8S_DEFAULT_VERSION}
echo "K8S_VERSION = $K8S_VERSION"

#
# Resolve kind version
KIND_VERSION=$(curl -X GET -s https://api.github.com/repos/kubernetes-sigs/kind/releases | jq -r 'first(.[]).name')
if [[ $KIND_VERSION != v* ]]; then
    echo "Resolved KIND_VERSION: $KIND_VERSION doesn't start with v*, defaults to $KIND_DEFAULT_VERSION"
fi
echo "KIND_VERSION = $KIND_VERSION"

#
# Resolve kubectl version
KUBECTL_VERSION=$(curl -X GET -s https://api.github.com/repos/kubernetes/kubernetes/releases | jq -r "[.[]| select(.name | match(\"v$K8S_VERSION.[0-9]+$\")).name][0]" )
if [[ $KUBECTL_VERSION != v${K8S_VERSION}* ]]; then
  echo "Resolved KUBECTL_VERSION: $KUBECTL_VERSION doesn't start with v$K8S_VERSION, defaults to $KUBECTL_DEFAULT_VERSION"
  KUBECTL_VERSION=$KUBECTL_DEFAULT_VERSION
fi
echo "KUBECTL_VERSION = $KUBECTL_VERSION"

#
# Resolve kind node image
KIND_NODE_IMAGE_VERSION=$(curl -X GET -s https://hub.docker.com/v2/repositories/kindest/node/tags | jq -r "[.results[]|select(.name | match(\"v$K8S_VERSION.[0-9]+$\"))][0] | .name + \"@\" + .images[0].digest")
if [[ $KIND_NODE_IMAGE_VERSION != v${K8S_VERSION}* ]]; then
  echo "Resolved KIND_NODE_IMAGE_VERSION: $KIND_NODE_IMAGE_VERSION doesn't start with v$K8S_VERSION, defaults to $KIND_NODE_IMAGE_K8S_DEFAULT_VERSION"
  KIND_NODE_IMAGE_VERSION=$KIND_NODE_IMAGE_K8S_DEFAULT_VERSION
fi
KIND_NODE_IMAGE_VERSION="kindest/node:${KIND_NODE_IMAGE_VERSION}"
echo "KIND_NODE_IMAGE_VERSION = $KIND_NODE_IMAGE_VERSION"

#
# Download and install kubectl
curl -LO https://storage.googleapis.com/kubernetes-release/release/${KUBECTL_VERSION}/bin/linux/amd64/kubectl && chmod +x kubectl

#
# Download and install kind
curl -Lo ./kind "https://kind.sigs.k8s.io/dl/${KIND_VERSION}/kind-$(uname)-amd64" && chmod +x ./kind
#
# Create a cluster
KIND_CLUSTER=$(echo $K8S_VERSION | tr -cd '[:alnum:]')
KIND_CLUSTER_NAME="k8s${KIND_CLUSTER}java${JAVA_VERSION}"
./kind create cluster  --name ${KIND_CLUSTER_NAME}  --image ${KIND_NODE_IMAGE_VERSION} --wait 5m
./kubectl cluster-info
./kubectl version

#
# Run Kubernetes API proxy
./kubectl proxy &

# Build the Docker images
./gradlew dockerBuild --stacktrace
docker images | grep micronaut
./kind load docker-image --name ${KIND_CLUSTER_NAME} micronaut-kubernetes-example-service:latest
./kind load docker-image --name ${KIND_CLUSTER_NAME} micronaut-kubernetes-example-client:latest
./kind load docker-image --name ${KIND_CLUSTER_NAME} micronaut-kubernetes-client-example:latest
