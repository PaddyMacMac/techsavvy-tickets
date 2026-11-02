# ---- build ----
FROM maven:3.9-eclipse-temurin-25 AS build
WORKDIR /workspace
COPY pom.xml .
RUN mvn -B -q dependency:go-offline
COPY src src
RUN mvn -B -q package -DskipTests

# ---- run ----
FROM eclipse-temurin:25-jre
RUN useradd --system --uid 10001 appuser
WORKDIR /app
COPY --from=build /workspace/target/tickets-api-*.jar app.jar
USER appuser
EXPOSE 8080
# --enable-preview is required because the code uses StructuredTaskScope (preview in JDK 25).
ENTRYPOINT ["java", "--enable-preview", "-XX:MaxRAMPercentage=75", "-jar", "app.jar"]
