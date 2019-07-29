#!/usr/bin/env sh

kubectl create -f k8s-auth.yml
kubectl create -f kubernetes.yml
./create-config-maps-and-secret.sh