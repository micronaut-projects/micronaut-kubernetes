#!/bin/bash
set -x

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

# Grant system user API access
kubectl create clusterrolebinding permissive-binding --clusterrole=cluster-admin --user=admin --user=kubelet --group=system:serviceaccounts

# ./gradlew jib

# Create deployments and services
kubectl create -f kubernetes.yml

# Wait for Service pod to be up and ready
SERVICE_POD="$()"
kubectl wait --for=condition=Ready pod/$SERVICE_POD

# Expose the service's port locally
kubectl port-forward $SERVICE_POD 8081:8081 &