#!/bin/bash
set -ex

#
# Defaults
CLUSTER_NAME="micronaut-${JOB_ID:-k8s-cluster}"

vcluster delete "$CLUSTER_NAME"

# Stop kubernetes API proxy
pkill -9 kubectl || true
# Stop vcluster
pkill -9 vcluster || true