# Multi-stage build for Customer Interface Microservice
FROM gradle:8.3-jdk17-alpine AS builder

# Set working directory
WORKDIR /app

# Copy gradle files
COPY build.gradle settings.gradle gradle.properties* ./
COPY gradle/ gradle/

# Copy source code
COPY src/ src/

# Build the application
RUN gradle clean build -x test --no-daemon

# Production stage
FROM openjdk:17-jdk-slim

# Create non-root user
RUN groupadd -r courier && useradd -r -g courier courier

# Install required packages
RUN apt-get update && apt-get install -y \
    curl \
    dumb-init \
    && rm -rf /var/lib/apt/lists/*

# Set working directory
WORKDIR /app

# Create necessary directories
RUN mkdir -p /var/log/courier /tmp/courier && \
    chown -R courier:courier /app /var/log/courier /tmp/courier

# Copy the built JAR from builder stage
COPY --from=builder /app/build/libs/*.jar app.jar

# Copy configuration files
COPY --from=builder /app/src/main/resources/application*.yml /app/config/

# Set ownership
RUN chown -R courier:courier /app

# Switch to non-root user
USER courier

# Expose port
EXPOSE 8081

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD curl -f http://localhost:8081/actuator/health || exit 1

# Environment variables
ENV SPRING_PROFILES_ACTIVE=local
ENV SERVER_PORT=8081
ENV JAVA_OPTS="-Xms256m -Xmx512m -XX:+UseG1GC -XX:MaxGCPauseMillis=200 -XX:+UseStringDeduplication"

# Use dumb-init to handle signals properly
ENTRYPOINT ["dumb-init", "--"]

# Run the application
CMD ["sh", "-c", "java $JAVA_OPTS -Djava.security.egd=file:/dev/./urandom -jar app.jar"]

# Metadata
LABEL maintainer="courier-team@company.com"
LABEL version="1.0.0"
LABEL description="Courier Customer Interface Microservice"
LABEL service="customer-interface"
