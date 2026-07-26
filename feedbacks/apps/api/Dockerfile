# syntax=docker/dockerfile:1
# Multi-stage — API Quarkus JVM (FR-15 / AD-1)
FROM maven:3.9.9-eclipse-temurin-17 AS build
WORKDIR /build
COPY pom.xml .
COPY src ./src
RUN mvn -B -DskipTests package

FROM eclipse-temurin:17-jre-jammy
WORKDIR /app
RUN groupadd -r app && useradd -r -g app app
COPY --from=build /build/target/quarkus-app /app/
USER app
EXPOSE 8080
ENTRYPOINT ["java", "-Dquarkus.http.host=0.0.0.0", "-jar", "quarkus-run.jar"]
