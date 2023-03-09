#!/bin/sh

set -x

# Create a new image version with latest code changes.
docker build . --tag pleo-antaeus

# Build the code.
docker run \
  --publish 8080:8080 \
  --rm \
  --interactive \
  --tty \
  --volume pleo-antaeus-build-cache:/root/.gradle \
  pleo-antaeus
