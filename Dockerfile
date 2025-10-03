# Multi-stage build for LogPilot Java application
FROM gradle:8.5-jdk17 AS build

# Set working directory
WORKDIR /app

# Copy gradle files
COPY gradle/ gradle/
COPY gradlew gradlew.bat build.gradle settings.gradle ./

# Copy source code
COPY logpilot-core/ logpilot-core/
COPY logpilot-client/ logpilot-client/
COPY logpilot-server/ logpilot-server/

# Make gradlew executable
RUN chmod +x gradlew

# Build the application
RUN ./gradlew clean :logpilot-server:build -x test

# Production image
FROM eclipse-temurin:17-jre

# Create app user
RUN addgroup --system appgroup && adduser --system --ingroup appgroup appuser

# Set working directory
WORKDIR /app

# Copy jar from build stage
COPY --from=build /app/logpilot-server/build/libs/logpilot-server-*.jar app.jar

# Change ownership
RUN chown -R appuser:appgroup /app

# Switch to non-root user
USER appuser

# Expose ports (8080 for REST, 50051 for gRPC)
EXPOSE 8080 50051

# Health check - Kubernetes will handle this via liveness/readiness probes
# HEALTHCHECK removed to avoid dependency on curl

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]