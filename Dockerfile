# =========================
# Stage 1: Build the app
# =========================
FROM gradle:8.7-jdk17 AS build

WORKDIR /app

# Copy all project files
COPY . .

# Build Spring Boot jar
RUN gradle clean build -x test

# =========================
# Stage 2: Run the app
# =========================
FROM ghcr.io/graalvm/jdk-community:17.0.9

WORKDIR /app

# Copy generated jar from build stage
COPY --from=build /app/build/libs/*.jar CargoSetuApp.jar

# Expose application port
EXPOSE 31201

# Start application
ENTRYPOINT ["java", "-jar", "CargoSetuApp.jar"]