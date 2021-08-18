#!/bin/bash
set -ex

#
# Defaults
K8S_DEFAULT_VERSION="1.21"
KUBECTL_DEFAULT_VERSION="v1.19.2"
KIND_VERSION="v0.11.1"

#
# K8s images required for KIND version
K8S_121="kindest/node:v1.21.1@sha256:69860bda5563ac81e3c0057d654b5253219618a22ec3a346306239bba8cfa1a6"
K8S_120="kindest/node:v1.20.7@sha256:cbeaf907fc78ac97ce7b625e4bf0de16e3ea725daf6b04f930bd14c67c671ff9"
K8S_119="kindest/node:v1.19.11@sha256:07db187ae84b4b7de440a73886f008cf903fcf5764ba8106a9fd5243d6f32729"
K8S_118="kindest/node:v1.18.19@sha256:7af1492e19b3192a79f606e43c35fb741e520d195f96399284515f077b3b622c"
K8S_117="kindest/node:v1.17.17@sha256:66f1d0d91a88b8a001811e2f1054af60eef3b669a9a74f9b6db871f2f1eeed00"

#
# Resolve K8s version
K8S_VERSION=${K8S_VERSION:=$K8S_DEFAULT_VERSION}
echo "K8S_VERSION = $K8S_VERSION"

#
# Resolve kind version
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
if [[ "1.21" == "${K8S_VERSION}" ]]; then
  KIND_NODE_IMAGE_VERSION=$K8S_121
elif [[ "1.20" == "${K8S_VERSION}" ]]; then
  KIND_NODE_IMAGE_VERSION=$K8S_120
elif [[ "1.19" == "${K8S_VERSION}" ]]; then
  KIND_NODE_IMAGE_VERSION=$K8S_119
elif [[ "1.18" == "${K8S_VERSION}" ]]; then
  KIND_NODE_IMAGE_VERSION=$K8S_118
elif [[ "1.17" == "${K8S_VERSION}" ]]; then
  KIND_NODE_IMAGE_VERSION=$K8S_117
fi
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

# Test the cluster was created
./kubectl get ns kube-system || exit 1

./kubectl cluster-info
./kubectl version

#
# Run Kubernetes API proxy
pkill -9 kubectl || true
./kubectl proxy &

# Build the Docker images
./gradlew dockerBuild --stacktrace
docker images | grep micronaut
./kind load docker-image --name ${KIND_CLUSTER_NAME} micronaut-kubernetes-example-service:latest
./kind load docker-image --name ${KIND_CLUSTER_NAME} micronaut-kubernetes-example-client:latest
./kind load docker-image --name ${KIND_CLUSTER_NAME} micronaut-kubernetes-client-example:latest
