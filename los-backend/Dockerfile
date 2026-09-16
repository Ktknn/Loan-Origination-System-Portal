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

# ─── Stage 2: Runtime ────────────────────────────────────────────────────────
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Non-root user (security best practice)
RUN addgroup -S appgroup && adduser -S appuser -G appgroup
USER appuser

COPY --from=build /app/target/*.jar app.jar

EXPOSE 8080

# UseContainerSupport: JVM tự detect RAM limit của container
# MaxRAMPercentage: dùng tối đa 75% RAM container
ENTRYPOINT ["java", \
  "-XX:+UseContainerSupport", \
  "-XX:MaxRAMPercentage=75.0", \
  "-jar", "app.jar"]