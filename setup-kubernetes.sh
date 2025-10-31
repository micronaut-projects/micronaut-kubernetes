#!/bin/bash
######################################
# Setup script for local developemnt   #
######################################
set -ex

while getopts c:t:m: flag; do
  case "${flag}" in
  c) CLUSTER_OPTION="$OPTARG" ;;
  t) TYPE_OPTION="$OPTARG" ;;
  m) IFS=',' read -ra MODULES <<< "$OPTARG" ;;
  *) ;;
  esac
done

CLUSTER_NAME=${CLUSTER_OPTION:-kind}
TYPE=${TYPE_OPTION:-java}

if [ "$TYPE" = "java" ]; then
  TYPE_CMD="dockerBuild"
elif [ "$TYPE" = "native" ]; then
  TYPE_CMD="dockerBuildNative"
fi

#kind get clusters

for MODULE in "${MODULES[@]}"
do
  ./gradlew clean "$MODULE:$TYPE_CMD" --refresh-dependencies
done


#./gradlew clean $TYPE_CMD --refresh-dependencies
#kind --name "$CLUSTER_NAME" load docker-image micronaut-kubernetes-example-service:latest
#kind --name "$CLUSTER_NAME" load docker-image micronaut-kubernetes-example-client:latest
#kind --name "$CLUSTER_NAME" load docker-image micronaut-kubernetes-client-example:latest
#kind --name "$CLUSTER_NAME" load docker-image micronaut-kubernetes-informer-example:latest
#kind --name "$CLUSTER_NAME" load docker-image micronaut-kubernetes-operator-example:latest

#
# Run Kubernetes API proxy
#pkill -9 kubectl || true
#kubectl proxy &
