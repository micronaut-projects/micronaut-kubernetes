#!/bin/bash
set -x

sudo apt-get update

KIND_VERSION="v0.9.0"
KUBECTL_VERSION=""
KIND_NODE_IMAGE_VERSION=""
K8S_VERSION=${K8S_VERSION:=1_19}

case $K8S_VERSION in
  1_16)
    KIND_NODE_IMAGE_VERSION="kindest/node:v1.16.15@sha256:a89c771f7de234e6547d43695c7ab047809ffc71a0c3b65aa54eda051c45ed20"
    KUBECTL_VERSION="v1.16.15"
    ;;

  1_17)
    KIND_NODE_IMAGE_VERSION="kindest/node:v1.17.11@sha256:5240a7a2c34bf241afb54ac05669f8a46661912eab05705d660971eeb12f6555"
    KUBECTL_VERSION="v1.17.12"
    ;;

  1_18)
    KIND_NODE_IMAGE_VERSION="kindest/node:v1.18.8@sha256:f4bcc97a0ad6e7abaf3f643d890add7efe6ee4ab90baeb374b4f41a4c95567eb"
    KUBECTL_VERSION="v1.18.9"
    ;;

  1_19 | *)
    KIND_NODE_IMAGE_VERSION="kindest/node:v1.19.1@sha256:98cf5288864662e37115e362b23e4369c8c4a408f99cbc06e58ac30ddc721600"
    KUBECTL_VERSION="v1.19.2"
    ;;
esac

echo "KIND_VERSION = $KIND_VERSION"
echo "KUBECTL_VERSION = $KUBECTL_VERSION"
echo "KIND_NODE_IMAGE_VERSION = $KIND_NODE_IMAGE_VERSION"

# Download and install kubectl
curl -LO https://storage.googleapis.com/kubernetes-release/release/${KUBECTL_VERSION}/bin/linux/amd64/kubectl && chmod +x kubectl && sudo mv kubectl /usr/local/bin/

# Download and install kind
curl -Lo ./kind "https://kind.sigs.k8s.io/dl/${KIND_VERSION}/kind-$(uname)-amd64" && chmod +x ./kind && sudo mv kind /usr/local/bin/

# Create a cluster
kind create cluster --image ${KIND_NODE_IMAGE_VERSION} --wait 5m
kubectl cluster-info
kubectl version

# Run Kubernetes API proxy
kubectl proxy &

# Create a new namespace and set it as the default
kubectl create namespace micronaut-kubernetes
kubectl config set-context --current --namespace=micronaut-kubernetes

# Build the Docker images
./gradlew jibDockerBuild --stacktrace
docker images | grep micronaut
kind load docker-image micronaut-kubernetes-example-service:latest
kind load docker-image micronaut-kubernetes-example-client:latest

# Create roles, deployments and services
kubectl create -f k8s-auth.yml
./create-config-maps-and-secret.sh
kubectl create -f kubernetes.yml

# Wait for pods to be up and ready
sleep 20
CLIENT_POD="$(kubectl get pods | grep "example-client" | awk 'FNR <= 1 { print $1 }')"
SERVICE_POD_1="$(kubectl get pods | grep "example-service" | awk 'FNR <= 1 { print $1 }')"
SERVICE_POD_2="$(kubectl get pods | grep "example-service" | awk 'FNR > 1 { print $1 }')"

kubectl describe pods
echo "Client pod logs:"
kubectl logs $CLIENT_POD

echo "Service pod #1 logs:"
kubectl logs $SERVICE_POD_1

echo "Service pod #2 logs:"
kubectl logs $SERVICE_POD_2

kubectl wait --for=condition=Ready pod/$SERVICE_POD_1 --timeout=60s
kubectl wait --for=condition=Ready pod/$CLIENT_POD --timeout=60s
kubectl wait --for=condition=Ready pod/$SERVICE_POD_2 --timeout=60s

# Expose ports locally
kubectl port-forward $SERVICE_POD_1 9999:8081 &
kubectl port-forward $SERVICE_POD_2 9998:8081 &
kubectl port-forward $CLIENT_POD 8888:8082 &