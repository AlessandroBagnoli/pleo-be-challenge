#!/bin/sh

set -x

docker compose --profile app down 

# Remove the docker volume that stores cached build artifacts.
# This also stops and removes any container using the volume.
echo 'Clearing build cache: '
docker volume remove -f pleo-be-challenge_pleo-antaeus-build-cache

echo 'Cleaning docker images'
# Remove all pleo-antaeus images.
docker images --quiet --filter="reference=pleo-antaeus:*" | \
 while read image; do
   docker rmi -f "$image"
 done
