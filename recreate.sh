#!/usr/bin/env sh

kubectl delete -f k8s-auth.yml
kubectl delete -f kubernetes.yml

kubectl create -f k8s-auth.yml
kubectl create -f kubernetes.yml
./create-config-maps-and-secret.sh