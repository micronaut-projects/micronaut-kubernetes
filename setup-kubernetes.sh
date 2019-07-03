#!/usr/bin/env sh

./gradlew clean assemble jib
killall -9 kubectl
kubectl proxy &
./recreate.sh

# Wait for pods to be up and ready
sleep 10
SERVICE_POD="$(kubectl get pods | grep "example-service" | awk 'FNR <= 1 { print $1 }')"
CLIENT_POD="$(kubectl get pods | grep "example-client" | awk 'FNR <= 1 { print $1 }')"
kubectl wait --for=condition=Ready pod/$SERVICE_POD
kubectl wait --for=condition=Ready pod/$CLIENT_POD

# Expose ports locally
kubectl port-forward $SERVICE_POD 9999:8081 &
kubectl port-forward $SERVICE_POD 5004:5004 &
kubectl port-forward $CLIENT_POD 8888:8082 &
kubectl port-forward $CLIENT_POD 5005:5005 &

kubectl logs -f $SERVICE_POD &
kubectl logs -f $CLIENT_POD &
