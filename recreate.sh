#!/usr/bin/env sh

kubectl delete -f k8s-auth.yml
kubectl delete -f kubernetes.yml
kubectl delete configmaps game-config

kubectl create -f k8s-auth.yml
kubectl create -f kubernetes.yml
kubectl create configmap game-config --from-file=game.properties