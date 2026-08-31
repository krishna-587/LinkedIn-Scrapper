# Build stage
FROM maven:3.9.6-eclipse-temurin-21 AS builder

WORKDIR /build

# Copy pom.xml and download dependencies
COPY pom.xml .
RUN mvn dependency:go-offline

# Copy source code
COPY src ./src

# Build the application
RUN mvn clean package -DskipTests

# Runtime stage
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Copy the JAR from the builder stage
COPY --from=builder /build/target/profileforge-*.jar app.jar

# Expose the port
EXPOSE 8080

# Environment variables for LinkedIn credentials
ENV LI_AT=""
ENV JSESSIONID=""
ENV SPRING_PORT=8080

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=5s --retries=3 \
    CMD wget --quiet --tries=1 --spider http://localhost:8080/api/profile || exit 1

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]
