# ── Stage 1: Build ────────────────────────────────────────────────────────────
FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /app

COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
# copy child module poms so parent resolves without errors
COPY market-compass-core/pom.xml market-compass-core/pom.xml
COPY market-compass-llm/pom.xml market-compass-llm/pom.xml
RUN chmod +x mvnw && ./mvnw dependency:go-offline -q --no-transfer-progress --projects . --also-make

COPY src/ src/
RUN ./mvnw clean package -DskipTests -q --no-transfer-progress --projects .

# ── Stage 2: Run ──────────────────────────────────────────────────────────────
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

RUN addgroup -S appgroup && adduser -S appuser -G appgroup
USER appuser

COPY --from=build /app/target/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", \
  "-XX:+UseContainerSupport", \
  "-XX:MaxRAMPercentage=75.0", \
  "-jar", "app.jar"]

HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=3 \
  CMD wget -qO- http://localhost:8080/api/market/status || exit 1
