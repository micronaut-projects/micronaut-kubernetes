#!/bin/bash
set -ex

echo "Execute setup for: $1"

MODULE_NAME="${1%:nativeTest}"

echo "Module Name: $MODULE_NAME"

kind version

kind get clusters

#EXAMPLE_SERVICE_RUNTIME=${EXAMPLE_SERVICE_RUNTIME:="java"}

EXAMPLE_SERVICE_RUNTIME="java"

#if [ "$MODULE_NAME" == "examples:example-client" ]; then
  #./setup-kubernetes.sh -c "test" -t "${EXAMPLE_SERVICE_RUNTIME}" -m "$MODULE_NAME,examples:example-service"
  #./setup-example-images.sh -t "${EXAMPLE_SERVICE_RUNTIME}" -m "$MODULE_NAME,examples:example-service"
#fi
