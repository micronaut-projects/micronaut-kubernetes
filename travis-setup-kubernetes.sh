#!/bin/bash
set -x

sudo apt-get update

# Download kubectl
curl -Lo kubectl https://storage.googleapis.com/kubernetes-release/release/v1.15.0/bin/linux/amd64/kubectl && chmod +x kubectl && sudo mv kubectl /usr/local/bin/

# Download and install kind
GO111MODULE="on" go get sigs.k8s.io/kind@v0.4.0 && kind create cluster

# Wait for Kubernetes to be up and ready.
JSONPATH='{range .items[*]}{@.metadata.name}:{range @.status.conditions[*]}{@.type}={@.status};{end}{end}'; until kubectl get nodes -o jsonpath="$JSONPATH" 2>&1 | grep -q "Ready=True"; do sleep 1; done
kubectl cluster-info

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