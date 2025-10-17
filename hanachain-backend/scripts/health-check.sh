#!/bin/bash

# Health check script for backend service
# Usage: ./health-check.sh [host] [port]

HOST="${1:-localhost}"
PORT="${2:-8080}"
MAX_RETRIES=30
RETRY_INTERVAL=3

check_health() {
    curl -f -s "http://${HOST}:${PORT}/actuator/health" > /dev/null
    return $?
}

main() {
    echo "Starting health check for backend at ${HOST}:${PORT}..."

    for i in $(seq 1 $MAX_RETRIES); do
        if check_health; then
            echo "✓ Health check passed (attempt $i/$MAX_RETRIES)"
            exit 0
        fi

        echo "✗ Health check failed (attempt $i/$MAX_RETRIES), retrying in ${RETRY_INTERVAL}s..."
        sleep $RETRY_INTERVAL
    done

    echo "✗ Health check failed after $MAX_RETRIES attempts"
    exit 1
}

main
