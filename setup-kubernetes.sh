#!/usr/bin/env sh

./gradlew jib
killall -9 kubectl
kubectl proxy &
./recreate.sh

# Wait for pods to be up and ready
sleep 5
SERVICE_POD="$(kubectl get pods | grep "example-service" | awk 'FNR <= 1 { print $1 }')"
CLIENT_POD="$(kubectl get pods | grep "example-client" | awk 'FNR <= 1 { print $1 }')"
kubectl wait --for=condition=Ready pod/$SERVICE_POD
kubectl wait --for=condition=Ready pod/$CLIENT_POD

# Expose the client's port locally
kubectl port-forward $CLIENT_POD 8888:8082 &