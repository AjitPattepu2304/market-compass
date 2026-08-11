FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /app

# copy maven wrapper from repo root
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./

# copy only the llm module (skip core to avoid missing module errors)
COPY market-compass-llm/pom.xml market-compass-llm/pom.xml

# strip market-compass-core module from parent pom so Maven doesn't look for it
RUN sed -i 's|<module>market-compass-core</module>||g' pom.xml

RUN chmod +x mvnw && ./mvnw dependency:go-offline -q --no-transfer-progress -pl market-compass-llm

COPY market-compass-llm/src market-compass-llm/src

RUN ./mvnw package -DskipTests -q --no-transfer-progress -pl market-compass-llm

FROM eclipse-temurin:21-jre-alpine
COPY --from=builder /app/market-compass-llm/target/*.jar app.jar
EXPOSE 8081
ENTRYPOINT ["java", "-Xmx400m", "-jar", "/app.jar", "--spring.profiles.active=groq"]
