#!/usr/bin/env sh

kubectl delete deployments.apps example-service
kubectl delete service example-service
kubectl delete deployments.apps example-client
kubectl delete service example-client
./gradlew clean jib
kubectl create -f kubernetes.yml