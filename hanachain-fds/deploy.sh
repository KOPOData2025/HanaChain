#!/bin/bash

set -e

# 설정
SERVICE_NAME="hanachain-fds"
CONTAINER_NAME="hanachain-fds"
IMAGE_NAME="hanachain-fds:latest"
PORT=8000
ENVIRONMENT="${ENVIRONMENT:-prod}"
AWS_REGION="${AWS_REGION:-ap-northeast-2}"
AWS_SECRET_NAME="hanachain/${ENVIRONMENT}/fds"

# 출력 색상
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

log_info() {
    echo -e "${GREEN}[INFO]${NC} $1"
}

log_warn() {
    echo -e "${YELLOW}[WARN]${NC} $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# AWS Secrets Manager에서 secrets 가져오기
fetch_secrets() {
    log_info "AWS Secrets Manager에서 secrets 가져오는 중..."

    if ! command -v aws &> /dev/null; then
        log_error "AWS CLI가 설치되지 않았습니다. 먼저 설치해주세요."
        exit 1
    fi

    # secrets를 가져와서 .env 파일로 저장
    aws secretsmanager get-secret-value \
        --region "$AWS_REGION" \
        --secret-id "$AWS_SECRET_NAME" \
        --query SecretString \
        --output text > .env.tmp

    if [ $? -eq 0 ]; then
        mv .env.tmp .env
        log_info "Secrets를 성공적으로 가져왔습니다"
    else
        log_error "AWS Secrets Manager에서 secrets 가져오기 실패"
        rm -f .env.tmp
        exit 1
    fi
}

# Docker image 빌드
build_image() {
    log_info "Docker image 빌드 중: $IMAGE_NAME"
    docker build -t "$IMAGE_NAME" .

    if [ $? -eq 0 ]; then
        log_info "Docker image 빌드 성공"
    else
        log_error "Docker image 빌드 실패"
        exit 1
    fi
}

# Health check 수행
health_check() {
    local max_attempts=30
    local attempt=1

    log_info "Health check 수행 중..."

    while [ $attempt -le $max_attempts ]; do
        if curl -f http://localhost:$PORT/health > /dev/null 2>&1; then
            log_info "Health check 통과"
            return 0
        fi

        log_warn "Health check 시도 $attempt/$max_attempts 실패, 재시도 중..."
        sleep 2
        attempt=$((attempt + 1))
    done

    log_error "Health check가 $max_attempts 번 시도 후 실패"
    return 1
}

# ML models 확인
check_models() {
    if [ ! -d "./models" ]; then
        log_warn "ML models 디렉토리를 찾을 수 없습니다. 빈 디렉토리 생성 중..."
        mkdir -p ./models
    fi

    if [ -z "$(ls -A ./models)" ]; then
        log_warn "ML models 디렉토리가 비어있습니다. 운영 환경 사용 전 models를 배포하세요."
    else
        log_info "./models 디렉토리에서 ML models를 찾았습니다"
    fi
}

# 기존 container 중지 및 제거
stop_old_container() {
    if [ "$(docker ps -q -f name=$CONTAINER_NAME)" ]; then
        log_info "기존 container 중지 중..."
        docker stop "$CONTAINER_NAME"
    fi

    if [ "$(docker ps -aq -f name=$CONTAINER_NAME)" ]; then
        log_info "기존 container 제거 중..."
        docker rm "$CONTAINER_NAME"
    fi
}

# 새 container 시작
start_container() {
    log_info "새 container 시작 중: $CONTAINER_NAME"

    # 필요한 디렉토리 생성
    mkdir -p ./logs

    docker run -d \
        --name "$CONTAINER_NAME" \
        --env-file .env \
        -p $PORT:$PORT \
        -v "$(pwd)/models:/app/models:ro" \
        -v "$(pwd)/logs:/app/logs" \
        --restart unless-stopped \
        --network hanachain-network \
        "$IMAGE_NAME"

    if [ $? -eq 0 ]; then
        log_info "Container 시작 성공"
    else
        log_error "Container 시작 실패"
        exit 1
    fi
}

# Log 보기
view_logs() {
    log_info "Container log 확인 중 (종료하려면 Ctrl+C)..."
    docker logs -f "$CONTAINER_NAME"
}

# Rollback 수행
rollback() {
    log_warn "이전 버전으로 rollback 중..."

    # 실패한 container 중지
    docker stop "$CONTAINER_NAME" 2>/dev/null || true
    docker rm "$CONTAINER_NAME" 2>/dev/null || true

    # backup에서 이전 container 시작
    if [ "$(docker ps -aq -f name=${CONTAINER_NAME}_backup)" ]; then
        docker start "${CONTAINER_NAME}_backup"
        docker rename "${CONTAINER_NAME}_backup" "$CONTAINER_NAME"
        log_info "Rollback 완료"
    else
        log_error "Rollback을 위한 backup container를 찾을 수 없습니다"
    fi
}

# 정리 작업
cleanup() {
    log_info "오래된 Docker images 정리 중..."
    docker image prune -f

    # backup container가 있으면 제거
    if [ "$(docker ps -aq -f name=${CONTAINER_NAME}_backup)" ]; then
        docker rm "${CONTAINER_NAME}_backup" 2>/dev/null || true
    fi
}

# 메인 배포 프로세스
main() {
    log_info "$SERVICE_NAME 배포 시작..."

    # ML models 확인
    check_models

    # Secrets 가져오기
    fetch_secrets

    # 새 image 빌드
    build_image

    # 현재 container backup
    if [ "$(docker ps -q -f name=$CONTAINER_NAME)" ]; then
        log_info "현재 container backup 생성 중..."
        docker rename "$CONTAINER_NAME" "${CONTAINER_NAME}_backup"
    fi

    # 새 container 시작
    start_container

    # Health check
    if health_check; then
        log_info "배포 성공!"
        cleanup

        # backup container 중지
        if [ "$(docker ps -q -f name=${CONTAINER_NAME}_backup)" ]; then
            docker stop "${CONTAINER_NAME}_backup"
        fi
    else
        log_error "배포 실패!"
        rollback
        exit 1
    fi

    # env 파일 정리
    rm -f .env

    log_info "배포가 성공적으로 완료되었습니다!"

    # 사용자에게 log를 볼 것인지 물어보기
    read -p "Log를 확인하시겠습니까? (y/n) " -n 1 -r
    echo
    if [[ $REPLY =~ ^[Yy]$ ]]; then
        view_logs
    fi
}

# main 함수 실행
main
