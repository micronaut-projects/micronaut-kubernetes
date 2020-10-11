#!/bin/bash
set -x

sudo apt-get update

KIND_VERSION=v0.9.0

# Download and install kubectl
curl -LO https://storage.googleapis.com/kubernetes-release/release/$(curl -s https://storage.googleapis.com/kubernetes-release/release/stable.txt)/bin/linux/amd64/kubectl && chmod +x kubectl && sudo mv kubectl /usr/local/bin/

# Download and install kind
curl -Lo ./kind "https://kind.sigs.k8s.io/dl/${KIND_VERSION}/kind-$(uname)-amd64" && chmod +x ./kind && sudo mv kind /usr/local/bin/

# Create a cluster
kind create cluster --wait 5m

# Configure kubectl
cp $(kind get kubeconfig-path) $HOME/.kube/config

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