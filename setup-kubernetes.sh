#!/usr/bin/env sh

set -ex

./gradlew clean dockerBuild --refresh-dependencies
kind load docker-image micronaut-kubernetes-example-service:latest
kind load docker-image micronaut-kubernetes-example-client:latest

killall -9 kubectl

kubectl proxy &
