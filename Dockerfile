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
RUN ./gradlew :logpilot-server:build -x test

# Production image
FROM eclipse-temurin:17-jre

# Install curl for health checks
RUN apt-get update && apt-get install -y curl && rm -rf /var/lib/apt/lists/*

# Create app user
RUN addgroup --system appgroup && adduser --system --ingroup appgroup appuser

# Set working directory
WORKDIR /app

# Copy jar from build stage
COPY --from=build /app/logpilot-server/build/libs/*.jar app.jar

# Change ownership
RUN chown -R appuser:appgroup /app

# Switch to non-root user
USER appuser

# Expose ports (8080 for REST, 50051 for gRPC)
EXPOSE 8080 50051

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
  CMD curl -f http://localhost:8080/actuator/health || exit 1

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]