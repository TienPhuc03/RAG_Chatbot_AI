FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /workspace

# Copy toàn bộ thư mục backend vào workspace
COPY pom.xml .
COPY src src

# Build dự án
RUN mvn -B -DskipTests clean package

FROM amazoncorretto:21-alpine
WORKDIR /app

# Copy từ target của thư mục backend
COPY --from=build /workspace/target/rag-chatbot-backend-0.0.1-SNAPSHOT.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]