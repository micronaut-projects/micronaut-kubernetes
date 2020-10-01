#!/usr/bin/env sh

kubectl create configmap game-config-properties --from-file=kubernetes-discovery-client/src/k8s/game.properties
kubectl create configmap game-config-yml --from-file=kubernetes-discovery-client/src/k8s/game.yml
kubectl create configmap game-config-json --from-file=kubernetes-discovery-client/src/k8s/game.json
kubectl create configmap literal-config --from-literal=special.how=very --from-literal=special.type=charm
kubectl create secret generic test-secret --from-literal=username='my-app' --from-literal=password='39528$vdg7Jb'
kubectl create secret generic another-secret --from-literal=secretProperty='secretValue'
kubectl create secret generic mounted-secret --from-literal=mountedVolumeKey='mountedVolumeValue'

kubectl label configmap game-config-yml app=game
kubectl label configmap literal-config app=game
kubectl label secret another-secret app=game
kubectl label configmap game-config-yml app.kubernetes.io/instance=example-service-1337
kubectl label configmap literal-config app.kubernetes.io/instance=example-service-1337
kubectl label secret another-secret app.kubernetes.io/instance=example-service-1337
