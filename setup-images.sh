#!/bin/bash
set -ex

###################################################
# Builds docker images with JIT or Native examples
###################################################

while getopts t:m: flag; do
  case "${flag}" in
  t) TYPE_OPTION="$OPTARG" ;;
  m) IFS=',' read -ra MODULES <<< "$OPTARG" ;;
  *) ;;
  esac
done

TYPE=${TYPE_OPTION:-java}

if [ "$TYPE" = "java" ]; then
  TYPE_CMD="dockerBuild"
elif [ "$TYPE" = "native" ]; then
  TYPE_CMD="dockerBuildNative"
fi

for MODULE in "${MODULES[@]}"
do
  ./gradlew clean "$MODULE:$TYPE_CMD" --refresh-dependencies
done
