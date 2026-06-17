# Stage 1: Build with Gradle
FROM gradle:8.10-jdk21 AS builder
WORKDIR /app
COPY settings.gradle.kts build.gradle.kts gradle.properties ./
COPY src ./src
RUN gradle build -x test --no-daemon

# Stage 2: Runtime with JRE only
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=builder /app/build/libs/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
