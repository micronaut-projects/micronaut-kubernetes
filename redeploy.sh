#!/usr/bin/env sh
set -e

./gradlew jibDockerBuild
for NS in micronaut-kubernetes micronaut-kubernetes-a; do
  kubectl -n $NS patch deployment example-service -p "{\"spec\":{\"template\":{\"metadata\":{\"labels\":{\"date\":\"$(date +'%s')\"}}}}}"
  kubectl -n $NS patch deployment example-client -p "{\"spec\":{\"template\":{\"metadata\":{\"labels\":{\"date\":\"$(date +'%s')\"}}}}}"
done