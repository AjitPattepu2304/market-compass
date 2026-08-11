FROM maven:3.9-eclipse-temurin-21 AS builder
WORKDIR /build
COPY pom.xml .
COPY ../pom.xml /parent-pom.xml
RUN mvn dependency:go-offline -q || true
COPY src ./src
RUN mvn package -DskipTests -q

FROM eclipse-temurin:21-jre
COPY --from=builder /build/target/*.jar app.jar
EXPOSE 8081
ENTRYPOINT ["java", "-Xmx400m", "-jar", "/app.jar", "--spring.profiles.active=groq"]
