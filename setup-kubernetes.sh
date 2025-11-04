#!/bin/bash
set -ex

########################################
# Loads docker images into kind cluster
########################################

while getopts c:i: flag; do
  case "${flag}" in
  c) CLUSTER_OPTION=${OPTARG} ;;
  i) IFS=',' read -ra IMAGES <<< "$OPTARG" ;;
  *) ;;
  esac
done

CLUSTER_NAME=${CLUSTER_OPTION:-kind}

kind get clusters

for IMAGE in "${IMAGES[@]}"
do
  kind --name "$CLUSTER_NAME" load docker-image "$IMAGE":latest
done

#
# Run Kubernetes API proxy
pkill -9 kubectl || true
kubectl proxy &
