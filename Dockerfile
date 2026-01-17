# Start from a base image with a Java Runtime Environment (JRE)
#FROM openjdk:17-jdk-slim
FROM ghcr.io/graalvm/jdk-community:17.0.9

# Set the working directory inside the container
WORKDIR /app

# Copy the built JAR file from your host machine's target directory to the container's /app directory
# Replace 'your-application-0.0.1-SNAPSHOT.jar' with your actual JAR file name
#ARG JAR_FILE=build/libs/CargoSetuApp-0.0.1-SNAPSHOT
ARG JAR_FILE=build/libs/CargoSetuApp-0.0.1-SNAPSHOT.jar
COPY ${JAR_FILE} CargoSetuApp.jar

# Expose the port your Spring Boot app listens on (default is 8080)
EXPOSE 31201

# Define the command to run your application when the container starts
ENTRYPOINT ["java", "-jar", "CargoSetuApp.jar"]
