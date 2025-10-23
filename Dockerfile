# Multi-stage build for Wish Keeper Java application

# Stage 1: Build
FROM maven:3.9-eclipse-temurin-17 AS builder

WORKDIR /app

# Copy pom.xml first for better layer caching
COPY pom.xml .

# Download dependencies
RUN mvn dependency:go-offline -B

# Copy source code
COPY src ./src

# Build the application
RUN mvn clean package -DskipTests

# Stage 2: Run
FROM eclipse-temurin:17-jre-jammy

WORKDIR /app

# Copy the built JAR from builder stage
COPY --from=builder /app/target/*.jar app.jar

# Expose the application port
EXPOSE 8000

# Environment variables for database connection will be provided by docker-compose.yml
# Required: DB_USER, DB_PASSWORD
# Optional (with defaults): DB_HOST, DB_PORT, DB_NAME

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]
