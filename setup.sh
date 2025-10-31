#!/bin/bash
set -ex

echo "Execute setup for: $1"

MODULE_NAME="${1%:nativeTest}"

echo "Module Name: $MODULE_NAME"
