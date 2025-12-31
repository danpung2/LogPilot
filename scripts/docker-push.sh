#!/bin/bash

# 프로젝트 루트 디렉토리로 이동
cd "$(dirname "$0")/.."

# build.gradle에서 버전 추출
VERSION=$(grep "version =" logpilot-server/build.gradle | awk -F"'" '{print $2}')

if [ -z "$VERSION" ]; then
    echo "❌ 버전을 찾을 수 없습니다."
    exit 1
fi

IMAGE_NAME="danpung2/logpilot"

echo "🚀 감지된 버전: $VERSION"
read -p "멀티 플랫폼 빌드 및 푸시를 진행하시겠습니까? (y/n) " -n 1 -r
echo
if [[ ! $REPLY =~ ^[Yy]$ ]]; then
    echo "❌ 사용자에 의해 푸시가 취소되었습니다."
    exit 1
fi

echo "🚀 Pushing LogPilot Server v$VERSION (Multi-Arch: amd64 + arm64)..."

# 멀티 플랫폼 빌더 확인 및 생성
if ! docker buildx inspect multi-arch-builder > /dev/null 2>&1; then
  echo "🛠 Creating new buildx builder..."
  docker buildx create --use --name multi-arch-builder
  docker buildx inspect --bootstrap
else
  echo "🛠 Using existing buildx builder..."
  docker buildx use multi-arch-builder
fi

# 멀티 플랫폼 빌드 및 푸시
docker buildx build --platform linux/amd64,linux/arm64 \
  -t $IMAGE_NAME:$VERSION \
  -t $IMAGE_NAME:latest \
  -f Dockerfile \
  --push .

echo "🚀 Pushing LogPilot Fullstack v$VERSION (Multi-Arch: amd64 + arm64)..."

FULLSTACK_IMAGE_NAME="danpung2/logpilot-fullstack"

docker buildx build --platform linux/amd64,linux/arm64 \
  -t $FULLSTACK_IMAGE_NAME:$VERSION \
  -t $FULLSTACK_IMAGE_NAME:latest \
  -f Dockerfile.all \
  --push .

echo "✅ Push Complete"
