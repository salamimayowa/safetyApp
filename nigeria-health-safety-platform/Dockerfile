# Stage 1 — Build
FROM maven:3.9-eclipse-temurin-17-alpine AS builder

WORKDIR /app

# Copy the project's contents (not the repo root) into /app
COPY nigeria-health-safety-platform/ /app/

# Build; -pl/-am builds only blood-bank plus the modules it depends on
RUN mvn -f /app/pom.xml -pl blood-bank -am clean install -DskipTests

# Stage 2 — Run
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

COPY --from=builder /app/blood-bank/target/blood-bank-1.0.0.jar app.jar

EXPOSE 8081

ENTRYPOINT ["java", "-jar", "app.jar"]