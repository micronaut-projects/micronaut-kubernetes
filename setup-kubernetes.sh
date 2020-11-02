#!/usr/bin/env sh

./gradlew clean assemble jibDockerBuild --refresh-dependencies
killall -9 kubectl

kubectl proxy &

./setup-test-namespace.sh micronaut-kubernetes-a true
./setup-test-namespace.sh micronaut-kubernetes true

# Wait for pods to be up and ready in namespace micronaut-kubernetes
kubectl config set-context --current --namespace=micronaut-kubernetes
sleep 10
SERVICE_POD_1="$(kubectl get pods | grep "example-service" | awk 'FNR <= 1 { print $1 }')"
SERVICE_POD_2="$(kubectl get pods | grep "example-service" | awk 'FNR > 1 { print $1 }')"
CLIENT_POD="$(kubectl get pods | grep "example-client" | awk 'FNR <= 1 { print $1 }')"
kubectl wait --for=condition=Ready pod/$SERVICE_POD_1
kubectl wait --for=condition=Ready pod/$CLIENT_POD
kubectl wait --for=condition=Ready pod/$SERVICE_POD_2

# Expose ports locally
kubectl port-forward $SERVICE_POD_1 9999:8081 &
kubectl port-forward $SERVICE_POD_1 5004:5004 &
kubectl port-forward $SERVICE_POD_2 9998:8081 &
kubectl port-forward $CLIENT_POD 8888:8082 &
kubectl port-forward $CLIENT_POD 5005:5005 &