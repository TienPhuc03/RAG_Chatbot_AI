FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /workspace

COPY pom.xml .
COPY src src

RUN mvn -B -DskipTests package

FROM amazoncorretto:21-alpine
WORKDIR /app

COPY --from=build /workspace/target/rag-chatbot-backend-0.0.1-SNAPSHOT.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
