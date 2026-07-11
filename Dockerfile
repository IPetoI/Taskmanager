# ==== Build stage ====
FROM eclipse-temurin:21-jdk AS build

WORKDIR /app

COPY . .

RUN ./mvnw clean package -DskipTests


# ==== Run stage ====
FROM eclipse-temurin:21-jdk

WORKDIR /app

COPY --from=build /app/target/*.jar app.jar

EXPOSE 8080

# Activate Docker profile for Spring Boot configuration
ENTRYPOINT ["java", "-jar", "app.jar", "--spring.profiles.active=docker"]