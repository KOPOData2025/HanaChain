# Stage 1: Build
FROM gradle:8.5-jdk17-alpine AS builder
WORKDIR /app

# gradle 파일 복사
COPY build.gradle settings.gradle ./
COPY gradle ./gradle

# dependencies 다운로드
RUN gradle dependencies --no-daemon || return 0

# 소스 코드 복사
COPY src ./src

# 애플리케이션 빌드
RUN gradle clean build -x test --no-daemon

# Stage 2: Runtime
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# 필수 도구 설치
RUN apk add --no-cache curl bash

# non-root 사용자 생성
RUN addgroup --system --gid 1001 spring && \
    adduser --system --uid 1001 spring

# builder에서 jar 복사
COPY --from=builder /app/build/libs/*.jar app.jar

# 소유권 설정
RUN chown -R spring:spring /app

USER spring

EXPOSE 8080

# Health check
HEALTHCHECK --interval=30s --timeout=10s --retries=3 --start-period=40s \
    CMD curl -f http://localhost:8080/actuator/health || exit 1

ENTRYPOINT ["java", \
    "-XX:+UseContainerSupport", \
    "-XX:MaxRAMPercentage=75.0", \
    "-Djava.security.egd=file:/dev/./urandom", \
    "-jar", \
    "app.jar"]
