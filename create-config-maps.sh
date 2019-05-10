#!/usr/bin/env sh

kubectl delete configmaps game-config-properties
kubectl delete configmaps game-config-yml

kubectl create configmap game-config-properties --from-file=kubernetes-core/src/k8s/game.properties
kubectl create configmap game-config-yml --from-file=kubernetes-core/src/k8s/game.yml