FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /app

# copy maven wrapper
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./

# copy both module poms so parent can resolve them
COPY market-compass-core/pom.xml market-compass-core/pom.xml
COPY market-compass-llm/pom.xml market-compass-llm/pom.xml

RUN chmod +x mvnw && ./mvnw dependency:go-offline -q --no-transfer-progress -pl market-compass-llm -am

# copy only llm source and build
COPY market-compass-llm/src market-compass-llm/src

RUN ./mvnw package -DskipTests -q --no-transfer-progress -pl market-compass-llm -am

FROM eclipse-temurin:21-jre-alpine
COPY --from=builder /app/market-compass-llm/target/*.jar app.jar
EXPOSE 8081
ENTRYPOINT ["java", "-Xmx400m", "-jar", "/app.jar", "--spring.profiles.active=groq"]
