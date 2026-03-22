# Stage 1: Build the application
FROM gradle:8.14-jdk17 AS builder
WORKDIR /app

# Copy the Gradle config and source code
COPY build.gradle.kts settings.gradle.kts ./
COPY src ./src

# Build the JAR file (skipping tests for faster deployment)
RUN gradle bootJar --no-daemon -x test

# Stage 2: Run the application
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Copy the built JAR from the builder stage
COPY --from=builder /app/build/libs/*.jar app.jar

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]
