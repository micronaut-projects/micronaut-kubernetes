#!/bin/bash
######################################
# Setup script for local developemnt   #
######################################
set -ex

while getopts c:t: flag; do
  case "${flag}" in
  c) CLUSTER_OPTION=${OPTARG} ;;
  t) TYPE_OPTION=${OPTARG} ;;
  *) ;;
  esac
done

CLUSTER_NAME=${CLUSTER_OPTION:-micronaut-k8s}
TYPE=${TYPE_OPTION:-java}

if [ "$TYPE" = "java" ]; then
  TYPE_CMD="dockerBuild"
elif [ "$TYPE" = "native" ]; then
  TYPE_CMD="dockerBuildNative"
fi

TAG_NAME="$TYPE-$JAVA_VERSION-${GIT_COMMIT_HASH:-latest}"

./gradlew clean $TYPE_CMD --refresh-dependencies

docker login $OCI_REGION.ocir.io -u $OCIR_USERNAME -p $AUTH_TOKEN

OCIR_REPOSITORY="$OCI_REGION.ocir.io/$OCI_TENANCY_NAME"

arr=("micronaut-kubernetes-example-service" "micronaut-kubernetes-example-client" "micronaut-kubernetes-client-example" "micronaut-kubernetes-informer-example" "micronaut-kubernetes-operator-example")

## now loop through the above array
for image in ${arr[@]}; do
  docker tag "$image:latest" "$OCIR_REPOSITORY/$image:$TAG_NAME"
  docker push "$OCIR_REPOSITORY/$image:$TAG_NAME"
done

sed -i -e "s|micronaut-kubernetes-client-example|${OCIR_REPOSITORY}/micronaut-kubernetes-client-example:${TAG_NAME}|g" kubernetes-client/src/test/resources/k8s/kubernetes-client-example-deployment.yml

sed -i -e "s|micronaut-kubernetes-example-service|${OCIR_REPOSITORY}/micronaut-kubernetes-example-service:${TAG_NAME}|g" kubernetes.yml
sed -i -e "s|micronaut-kubernetes-example-client|${OCIR_REPOSITORY}/micronaut-kubernetes-example-client:${TAG_NAME}|g" kubernetes.yml

sed -i -e "s|micronaut-kubernetes-example-client|${OCIR_REPOSITORY}/micronaut-kubernetes-example-client:${TAG_NAME}|g" test-utils/src/main/resources/k8s/example-client-deployment.yml
sed -i -e "s|micronaut-kubernetes-example-service|${OCIR_REPOSITORY}/micronaut-kubernetes-example-service:${TAG_NAME}|g" test-utils/src/main/resources/k8s/example-service-deployment.yml

#
# Run Kubernetes API proxy
pkill -9 kubectl || true
kubectl proxy &
