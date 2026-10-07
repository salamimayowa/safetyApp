# Stage 1 — Build
FROM eclipse-temurin:17-jdk-alpine AS builder

# Install Maven
RUN apk add --no-cache maven

# Set working directory
WORKDIR /app

# Copy entire project into /app
COPY . /app/

# List files to confirm they are there (for debugging)
RUN ls -la /app/

# Build the entire project
RUN mvn -f /app/pom.xml clean install -DskipTests

# Stage 2 — Run
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

# Copy the built JAR from builder
COPY --from=builder /app/blood-bank/target/blood-bank-1.0.0.jar app.jar

# Expose port
EXPOSE 8081

# Start the app
ENTRYPOINT ["java", "-jar", "app.jar"]