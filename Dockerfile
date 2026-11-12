# syntax=docker/dockerfile:1

# ---- Build: compile the jar with JDK 21 + Maven (no local Java needed) ----
# Uses the image's `mvn` instead of ./mvnw: on Windows checkouts mvnw may get
# CRLF line endings, which break it inside a Linux container.
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build
COPY pom.xml .
RUN --mount=type=cache,target=/root/.m2 mvn -B -q dependency:go-offline
COPY src ./src
# Tests already run in CI; the image only needs the jar.
RUN --mount=type=cache,target=/root/.m2 mvn -B -q package -DskipTests \
    && cp target/prisma-backend-*.jar /build/app.jar

# ---- Run: slim JRE 21, non-root user ----
FROM eclipse-temurin:21-jre-alpine
RUN addgroup -S prisma && adduser -S prisma -G prisma \
    && mkdir -p /app/logs && chown -R prisma:prisma /app
WORKDIR /app
COPY --from=build --chown=prisma:prisma /build/app.jar app.jar
USER prisma
EXPOSE 8080
# Size the heap from the container's memory limit, not the host's.
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75"
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
