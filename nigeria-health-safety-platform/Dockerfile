# Stage 1 — Build
FROM eclipse-temurin:17-jdk-alpine AS builder

# Install Maven
RUN apk add --no-cache maven

# Set working directory
WORKDIR /app

# Copy entire project
COPY . .

# Build the parent first, then blood-bank
RUN mvn clean install -DskipTests

# Stage 2 — Run
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

# Copy the built JAR
COPY --from=builder /app/blood-bank/target/blood-bank-1.0.0.jar app.jar

# Expose port
EXPOSE 8081

# Start the app
ENTRYPOINT ["java", "-jar", "app.jar"]