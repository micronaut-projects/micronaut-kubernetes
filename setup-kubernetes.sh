#!/usr/bin/env sh

set -ex

./gradlew clean dockerBuild --refresh-dependencies
killall -9 kubectl

kubectl proxy &
