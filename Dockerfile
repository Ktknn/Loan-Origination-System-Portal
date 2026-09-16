# =============================================================================
# Dockerfile — All-in-One: React Portal + Spring Boot Backend → 1 JAR
# Build tại root: docker build -t los-app .
# Run:           docker run -p 8080:8080 --env-file los-backend/.env los-app
# =============================================================================

# ─── Stage 1: Build React Frontend ──────────────────────────────────────────
FROM node:22-alpine AS frontend

WORKDIR /app/los-portal

# Cài dependencies trước (tận dụng layer cache khi chỉ source thay đổi)
COPY los-portal/package*.json ./
RUN npm ci --silent

# Copy source và build
COPY los-portal/ .
RUN npm run build
# Kết quả: /app/los-portal/dist/

# ─── Stage 2: Build Spring Boot JAR (embed frontend) ────────────────────────
FROM eclipse-temurin:21-jdk-alpine AS backend

WORKDIR /app/los-backend

# Maven wrapper + pom trước (cache dependencies)
COPY los-backend/.mvn/ .mvn/
COPY los-backend/mvnw los-backend/mvnw.cmd* ./
COPY los-backend/pom.xml ./
RUN chmod +x mvnw && ./mvnw dependency:go-offline -q

# Copy Java source
COPY los-backend/src ./src

# Copy frontend dist vào Spring Boot static resources (giống build-demo.ps1)
COPY --from=frontend /app/los-portal/dist ./src/main/resources/static/

# Build JAR (frontend đã được embed)
RUN ./mvnw package -DskipTests -q

# ─── Stage 3: Runtime (JRE nhẹ, không cần JDK) ──────────────────────────────
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

RUN addgroup -S appgroup && adduser -S appuser -G appgroup
USER appuser

COPY --from=backend /app/los-backend/target/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-XX:+UseContainerSupport", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]
