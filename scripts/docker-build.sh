#!/bin/bash

# 프로젝트 루트 디렉토리로 이동 (스크립트가 어디서 실행되든 동작하도록)
cd "$(dirname "$0")/.."

# build.gradle에서 버전 추출
VERSION=$(grep "version =" logpilot-server/build.gradle | awk -F"'" '{print $2}')

if [ -z "$VERSION" ]; then
    echo "❌ 버전을 찾을 수 없습니다."
    exit 1
fi

IMAGE_NAME="danpung2/logpilot"

echo "🐳 감지된 버전: $VERSION"
read -p "빌드를 진행하시겠습니까? (y/n) " -n 1 -r
echo
if [[ ! $REPLY =~ ^[Yy]$ ]]; then
    echo "❌ 사용자에 의해 빌드가 취소되었습니다."
    exit 1
fi

echo "🐳 Building LogPilot Server v$VERSION (Local Architecture)..."

# 로컬 아키텍처용 빌드 (로컬 도커 데몬에 저장)
docker build \
  -t $IMAGE_NAME:$VERSION \
  -t $IMAGE_NAME:latest \
  -f Dockerfile \
  .

echo "🐳 Building LogPilot Fullstack v$VERSION (Local Architecture)..."

FULLSTACK_IMAGE_NAME="danpung2/logpilot-fullstack"

docker build \
  -t $FULLSTACK_IMAGE_NAME:$VERSION \
  -t $FULLSTACK_IMAGE_NAME:latest \
  -f Dockerfile.all \
  .

echo "✅ Build Complete (Local)"
