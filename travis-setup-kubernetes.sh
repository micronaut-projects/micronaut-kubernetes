#!/bin/bash
set -x

sudo apt-get update
sudo apt-get install -y socat

# Make root mounted as rshared to fix kube-dns issues.
sudo mount --make-rshared /

# Download kubectl, which is a requirement for using minikube.
curl -Lo kubectl https://storage.googleapis.com/kubernetes-release/release/v1.12.0/bin/linux/amd64/kubectl && chmod +x kubectl && sudo mv kubectl /usr/local/bin/

# Download minikube.
curl -Lo minikube https://storage.googleapis.com/minikube/releases/v0.30.0/minikube-linux-amd64 && chmod +x minikube && sudo mv minikube /usr/local/bin/
sudo minikube start --vm-driver=none --bootstrapper=kubeadm --kubernetes-version=v1.12.0

# Fix the kubectl context, as it's often stale.
minikube update-context

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
kubectl create configmap game-config --from-file=game.properties

# Wait for pods to be up and ready
sleep 5
SERVICE_POD="$(kubectl get pods | grep "example-service" | awk 'FNR <= 1 { print $1 }')"
CLIENT_POD="$(kubectl get pods | grep "example-client" | awk 'FNR <= 1 { print $1 }')"
kubectl wait --for=condition=Ready pod/$SERVICE_POD
kubectl wait --for=condition=Ready pod/$CLIENT_POD

# Expose the client's port locally
kubectl port-forward $CLIENT_POD 8888:8082 &