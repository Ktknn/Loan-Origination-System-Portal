# =============================================================================
# Dockerfile — Spring Boot Backend Only (dùng cho Render deploy)
# Build: docker build -t los-backend .   (chạy trong los-backend/)
# =============================================================================

# ─── Stage 1: Build ──────────────────────────────────────────────────────────
FROM eclipse-temurin:21-jdk-alpine AS build

WORKDIR /app

# Copy Maven wrapper + pom TRƯỚC (để cache dependencies riêng)
COPY .mvn/ .mvn/
COPY mvnw mvnw.cmd* ./
RUN chmod +x mvnw

COPY pom.xml ./
# Download tất cả dependencies (layer này chỉ rebuild khi pom.xml đổi)
RUN ./mvnw dependency:go-offline -q

# Copy source và build JAR
COPY src ./src
RUN ./mvnw package -DskipTests -q