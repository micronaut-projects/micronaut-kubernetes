#!/usr/bin/env sh

./gradlew jib
#kubectl set image deployment.v1.apps/example-service example-service=registry.hub.docker.com/alvarosanchez/example-service --record
kubectl patch deployment example-service -p "{\"spec\":{\"template\":{\"metadata\":{\"labels\":{\"date\":\"`date +'%s'`\"}}}}}"