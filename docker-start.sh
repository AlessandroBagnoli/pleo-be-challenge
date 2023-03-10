#!/bin/sh

set -x

# Create a new image version with latest code changes.
docker compose --profile app up -d