#!/usr/bin/env sh

kubectl delete deployments.apps example-service
kubectl delete service example-service
kubectl create -f examples/micronaut-service/kubernetes.yml