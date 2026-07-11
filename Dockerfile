# ==== Build stage ====
FROM eclipse-temurin:21-jdk AS build

WORKDIR /app

COPY . .

RUN ./mvnw clean package -DskipTests


# ==== Run stage ====
FROM eclipse-temurin:21-jdk

WORKDIR /app

COPY target/*.jar app.jar

# Activate Docker profile for Spring Boot configuration
ENTRYPOINT ["java", "-jar", "app.jar", "--spring.profiles.active=docker"]