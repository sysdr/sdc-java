FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml ./
COPY merkle-tree-service/pom.xml ./merkle-tree-service/pom.xml
COPY merkle-tree-service/src ./merkle-tree-service/src
WORKDIR /app/merkle-tree-service
RUN mvn clean package -DskipTests

FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /app/merkle-tree-service/target/*.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]
