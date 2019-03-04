#!/usr/bin/env sh

./gradlew jib
kubectl patch deployment example-service -p "{\"spec\":{\"template\":{\"metadata\":{\"labels\":{\"date\":\"`date +'%s'`\"}}}}}"