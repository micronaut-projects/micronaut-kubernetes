#!/usr/bin/env sh

killall -9 kubectl
kubectl delete -f k8s-auth.yml
kubectl delete -f kubernetes.yml
kubectl delete configmaps game-config-properties
kubectl delete configmaps game-config-yml
kubectl delete configmaps game-config-json
kubectl delete configmaps literal-config
kubectl delete configmaps hello-controller-spec
kubectl delete secret test-secret
kubectl delete secret another-secret