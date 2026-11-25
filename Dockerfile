# --- Build stage -------------------------------------------------------------
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /workspace

# Cache dependencies first: only pom.xml changes invalidate this layer.
COPY pom.xml ./
RUN mvn -B -q -DskipTests dependency:go-offline || true

COPY src ./src
RUN mvn -B -DskipTests package

# --- Runtime stage -----------------------------------------------------------
FROM eclipse-temurin:21-jre
WORKDIR /app

RUN groupadd --system app && useradd --system --gid app --home /app app

COPY --from=build /workspace/target/backend-catalogo-*.jar /app/app.jar
RUN chown -R app:app /app

USER app
EXPOSE 8081

ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75.0", "-jar", "/app/app.jar"]
