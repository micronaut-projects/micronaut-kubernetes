#!/bin/bash
set -x

sudo apt-get update

# Download and install kubectl
curl -LO https://storage.googleapis.com/kubernetes-release/release/$(curl -s https://storage.googleapis.com/kubernetes-release/release/stable.txt)/bin/linux/amd64/kubectl && chmod +x kubectl && sudo mv kubectl /usr/local/bin/

# Download and install kind
curl -Lo kind https://github.com/kubernetes-sigs/kind/releases/download/v0.4.0/kind-linux-amd64 && chmod +x ./kind && sudo mv kind /usr/local/bin/

# Create a cluster
kind create cluster

# Setup kubectl
export KUBECONFIG="$(kind get kubeconfig-path)"

# Wait for Kubernetes to be up and ready.
JSONPATH='{range .items[*]}{@.metadata.name}:{range @.status.conditions[*]}{@.type}={@.status};{end}{end}'; until kubectl get nodes -o jsonpath="$JSONPATH" 2>&1 | grep -q "Ready=True"; do sleep 1; done
kubectl cluster-info
kubectl version

# Run Kubernetes API proxy
kubectl proxy &

# Login to the Docker hub and push the images
echo "$DOCKER_PASSWORD" | docker login -u "$DOCKER_USERNAME" --password-stdin
./gradlew jib --stacktrace

# Create roles, deployments and services
kubectl create -f k8s-auth.yml
kubectl create -f kubernetes.yml
./create-config-maps-and-secret.sh
# Wait for pods to be up and ready
sleep 20
SERVICE_POD="$(kubectl get pods | grep "example-service" | awk 'FNR <= 1 { print $1 }')"
CLIENT_POD="$(kubectl get pods | grep "example-client" | awk 'FNR <= 1 { print $1 }')"
kubectl wait --for=condition=Ready pod/$SERVICE_POD
kubectl wait --for=condition=Ready pod/$CLIENT_POD

# Expose the client's port locally
kubectl port-forward $CLIENT_POD 8888:8082 &

kubectl logs -f $SERVICE_POD &
kubectl logs -f $CLIENT_POD &