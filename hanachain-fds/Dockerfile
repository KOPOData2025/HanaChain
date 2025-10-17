# Stage 1: Builder
FROM python:3.11-slim AS builder
WORKDIR /app

# 빌드 dependencies 설치
RUN apt-get update && \
    apt-get install -y --no-install-recommends \
    gcc \
    g++ \
    && rm -rf /var/lib/apt/lists/*

# requirements 파일 복사
COPY requirements.txt .

# Python dependencies 설치
RUN pip install --no-cache-dir --user -r requirements.txt

# Stage 2: Runtime
FROM python:3.11-slim
WORKDIR /app

# 런타임 dependencies 설치
RUN apt-get update && \
    apt-get install -y --no-install-recommends \
    curl \
    && rm -rf /var/lib/apt/lists/*

# non-root 사용자 생성
RUN useradd -m -u 1001 fastapi && \
    chown -R fastapi:fastapi /app

# builder에서 Python dependencies 복사
COPY --from=builder /root/.local /home/fastapi/.local

# 애플리케이션 코드 복사
COPY --chown=fastapi:fastapi . .

# 환경 변수 설정
ENV PYTHONUNBUFFERED=1 \
    PYTHONDONTWRITEBYTECODE=1 \
    PATH=/home/fastapi/.local/bin:$PATH

USER fastapi

EXPOSE 8000

# Health check
HEALTHCHECK --interval=30s --timeout=10s --retries=3 --start-period=40s \
    CMD curl -f http://localhost:8000/health || exit 1

# uvicorn으로 실행
CMD ["uvicorn", "main:app", "--host", "0.0.0.0", "--port", "8000", "--workers", "4"]
