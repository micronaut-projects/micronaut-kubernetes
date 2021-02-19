#!/usr/bin/env sh

set -ex

CLUSTER_NAME=${1:-kind}

./gradlew clean dockerBuild --refresh-dependencies
kind --name "$CLUSTER_NAME" load docker-image micronaut-kubernetes-example-service:latest
kind --name "$CLUSTER_NAME" load docker-image micronaut-kubernetes-example-client:latest
kind --name "$CLUSTER_NAME" load docker-image micronaut-kubernetes-client-example:latest

killall -9 kubectl

kubectl proxy &
