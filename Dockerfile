# Stage 1: Build the application
FROM maven:3.9.6-eclipse-temurin-17-alpine AS build
WORKDIR /app
COPY pom.xml .
# Download dependencies first to cache them
RUN mvn dependency:go-offline -B
COPY src ./src
# Package the application (skip tests for faster deployment)
RUN mvn clean package -DskipTests

# Stage 2: Run the application
FROM eclipse-temurin:17-jre
WORKDIR /app
# Expose the default Spring Boot port
EXPOSE 8080
# Copy the built JAR from the build stage
COPY --from=build /app/target/*.jar app.jar
# Command to run the application
ENTRYPOINT ["java", "-jar", "app.jar"]
