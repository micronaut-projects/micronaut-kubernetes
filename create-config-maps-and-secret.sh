#!/usr/bin/env sh

kubectl delete configmaps game-config-properties
kubectl delete configmaps game-config-yml
kubectl delete configmaps game-config-json
kubectl delete configmaps literal-config
kubectl delete secret test-secret

kubectl create configmap game-config-properties --from-file=kubernetes-discovery-client/src/k8s/game.properties
kubectl create configmap game-config-yml --from-file=kubernetes-discovery-client/src/k8s/game.yml
kubectl create configmap game-config-json --from-file=kubernetes-discovery-client/src/k8s/game.json
kubectl create configmap literal-config --from-literal=special.how=very --from-literal=special.type=charm
kubectl create secret generic test-secret --from-literal=username='my-app' --from-literal=password='39528$vdg7Jb'