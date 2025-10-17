#!/bin/bash

# FDS service를 위한 health check script
# 사용법: ./health-check.sh [host] [port]

HOST="${1:-localhost}"
PORT="${2:-8000}"
MAX_RETRIES=30
RETRY_INTERVAL=2

check_health() {
    curl -f -s "http://${HOST}:${PORT}/health" > /dev/null
    return $?
}

main() {
    echo "FDS health check 시작: ${HOST}:${PORT}..."

    for i in $(seq 1 $MAX_RETRIES); do
        if check_health; then
            echo "✓ Health check 통과 (시도 $i/$MAX_RETRIES)"
            exit 0
        fi

        echo "✗ Health check 실패 (시도 $i/$MAX_RETRIES), ${RETRY_INTERVAL}초 후 재시도..."
        sleep $RETRY_INTERVAL
    done

    echo "✗ Health check가 $MAX_RETRIES 번 시도 후 실패"
    exit 1
}

main
