#!/bin/bash
set -ex

# Defaults
EXAMPLE_SERVICE_RUNTIME=${EXAMPLE_SERVICE_RUNTIME:="java"}

# Download vcluster CLI
curl -L -o vcluster "https://github.com/loft-sh/vcluster/releases/latest/download/vcluster-linux-amd64" && sudo install -c -m 0755 vcluster /usr/local/bin && rm -f vcluster

#Print vcluster version
vcluster --version

kubectl get ns kube-system || exit 1

CLUSTER_NAME="micronaut-${JOB_ID:-k8s-cluster}"

# create cluster
vcluster create "$CLUSTER_NAME" --isolate > vcluster-out.log 2>&1 &

# Test the cluster was created
kubectl get ns kube-system || exit 1

kubectl cluster-info

kubectl version

bash setup-kubernetes.sh -c "${CLUSTER_NAME}" -t "${EXAMPLE_SERVICE_RUNTIME}"