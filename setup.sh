#!/bin/bash
set -ex

echo "Execute setup: $1"

MODULE_NAME="${1%:nativeTest}"

declare -A IMAGE_MAP
IMAGE_MAP["examples:example-service"]="micronaut-kubernetes-example-service"
IMAGE_MAP["examples:example-client"]="micronaut-kubernetes-example-client"
IMAGE_MAP["examples:example-service-config-import"]="micronaut-kubernetes-example-service-config-import"
IMAGE_MAP["examples:example-service-openapi-config-import"]="micronaut-kubernetes-example-service-openapi-config-import"
IMAGE_MAP["examples:example-kubernetes-client"]="micronaut-kubernetes-client-example"
IMAGE_MAP["examples:example-kubernetes-informer"]="micronaut-kubernetes-informer-example"
IMAGE_MAP["examples:example-kubernetes-operator"]="micronaut-kubernetes-operator-example"
IMAGE_MAP["examples:example-service-openapi"]="micronaut-kubernetes-example-service-openapi"
IMAGE_MAP["examples:example-client-openapi"]="micronaut-kubernetes-example-client-openapi"
IMAGE_MAP["examples:example-kubernetes-client-openapi-informer-java"]="micronaut-kubernetes-example-informer-openapi"
IMAGE_MAP["examples:example-kubernetes-client-openapi-operator-java"]="micronaut-kubernetes-example-operator-openapi"

MODULE_ARRAY=()
IMAGE_ARRAY=()
if [ -z "$MODULE_NAME" ]; then
  EXAMPLE_SERVICE_RUNTIME="java"
  for module in "${!IMAGE_MAP[@]}"; do
    MODULE_ARRAY+=("${module}")
    IMAGE_ARRAY+=("${IMAGE_MAP[$module]}")
  done
elif [ "$MODULE_NAME" == "examples:example-client" ]; then
  EXAMPLE_SERVICE_RUNTIME="native"
  MODULE_ARRAY+=("examples:example-service" "examples:example-client")
  IMAGE_ARRAY+=("${IMAGE_MAP["examples:example-service"]}" "${IMAGE_MAP["examples:example-client"]}")
elif [ "$MODULE_NAME" == "examples:example-client-openapi" ]; then
  EXAMPLE_SERVICE_RUNTIME="native"
  MODULE_ARRAY+=("examples:example-service-openapi" "examples:example-client-openapi")
  IMAGE_ARRAY+=("${IMAGE_MAP["examples:example-service-openapi"]}" "${IMAGE_MAP["examples:example-client-openapi"]}")
elif [[ "$MODULE_NAME" == "examples:example-kubernetes-informer" ||
        "$MODULE_NAME" == "examples:example-kubernetes-operator" ||
        "$MODULE_NAME" == "examples:example-service-config-import" ||
        "$MODULE_NAME" == "examples:example-service-openapi-config-import" ||
        "$MODULE_NAME" == "examples:example-kubernetes-client-openapi-informer-java" ||
        "$MODULE_NAME" == "examples:example-kubernetes-client-openapi-operator-java" ]]; then
  EXAMPLE_SERVICE_RUNTIME="native"
  MODULE_ARRAY+=("$MODULE_NAME")
  IMAGE_ARRAY+=("${IMAGE_MAP[$MODULE_NAME]}")
fi

if [ "${#MODULE_ARRAY[@]}" -eq 0 ]; then
  echo "There are no example images which need to be built"
  exit 0
fi

#
# Kubernetes toolchain
K8S_VERSION="1.36"
KUBECTL_VERSION="v1.36.1"
KIND_VERSION="v0.32.0"
KIND_NODE_IMAGE_VERSION="kindest/node:v1.36.1@sha256:3489c7674813ba5d8b1a9977baea8a6e553784dab7b84759d1014dbd78f7ebd5"

echo "K8S_VERSION = $K8S_VERSION"
echo "KIND_VERSION = $KIND_VERSION"
echo "KUBECTL_VERSION = $KUBECTL_VERSION"
echo "KIND_NODE_IMAGE_VERSION = $KIND_NODE_IMAGE_VERSION"

#
# Download and install kubectl
curl --fail --location --silent --show-error --retry 3 \
  -o ./kubectl "https://dl.k8s.io/release/${KUBECTL_VERSION}/bin/linux/amd64/kubectl"
chmod +x ./kubectl
mv ./kubectl "${HOME}/kubectl"

#
# Download and install kind
curl --fail --location --silent --show-error --retry 3 \
  -o "${HOME}/kind" "https://kind.sigs.k8s.io/dl/${KIND_VERSION}/kind-$(uname)-amd64"
chmod +x "${HOME}/kind"

export PATH="$PATH:${HOME}"

#
# Create a cluster
KIND_CLUSTER=$(echo $K8S_VERSION | tr -cd '[:alnum:]')
KIND_CLUSTER_NAME="k8s${KIND_CLUSTER}java${JAVA_VERSION}"
$HOME/kind create cluster  --name ${KIND_CLUSTER_NAME}  --image ${KIND_NODE_IMAGE_VERSION} --wait 5m

# Test the cluster was created
$HOME/kubectl get ns kube-system || exit 1

$HOME/kubectl cluster-info
$HOME/kubectl version

MODULES=$(IFS=, ; echo "${MODULE_ARRAY[*]}")
IMAGES=$(IFS=, ; echo "${IMAGE_ARRAY[*]}")

./setup-images.sh -t "${EXAMPLE_SERVICE_RUNTIME}" -m "${MODULES}"
./setup-kubernetes.sh -c "${KIND_CLUSTER_NAME}" -i "${IMAGES}"
