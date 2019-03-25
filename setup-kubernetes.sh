#!/usr/bin/env sh

killall -9 kubectl
kubectl proxy &
./recreate.sh

# Wait for Service pod to be up and ready
sleep 5
SERVICE_POD="$(kubectl get pods | grep "example-service" | awk 'FNR <= 1 { print $1 }')"
kubectl wait --for=condition=Ready pod/$SERVICE_POD

# Expose the service's port locally
kubectl port-forward $SERVICE_POD 8888:8081 &