#!/usr/bin/env sh

kubectl create -f k8s-auth.yml
./create-config-maps-and-secret.sh
kubectl create -f kubernetes.yml