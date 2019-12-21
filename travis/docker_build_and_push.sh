#!/bin/sh

IMAGE_BASE=$DOCKER_USERNAME/wolfia

IMAGE_BRANCH=$(echo "$TRAVIS_BRANCH" | sed -e 's/\//_/g')
echo "Image branch: $IMAGE_BRANCH"

GIT_HASH=$(git rev-parse HEAD)
echo "Full git hash: $GIT_HASH"
GIT_HASH_SHORT=$(echo "$GIT_HASH" | sed 's/\(.\{8\}\).*/\1/')
echo "Git hash version tag: $GIT_HASH_SHORT"

docker build \
  -t "$IMAGE_BASE:$IMAGE_BRANCH" \
  -t "$IMAGE_BASE:$GIT_HASH_SHORT" \
  -f docker/Dockerfile \
  .

echo "$DOCKER_PASSWORD" | docker login -u="$DOCKER_USERNAME" --password-stdin
docker push "$IMAGE_BASE:$IMAGE_BRANCH"
docker push "$IMAGE_BASE:$GIT_HASH_SHORT"
docker logout
