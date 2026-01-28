FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml ./pom.xml
COPY indexing-service/pom.xml ./indexing-service/pom.xml
COPY indexing-service/src ./indexing-service/src
WORKDIR /app/indexing-service
RUN mvn clean package -DskipTests

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=build /app/indexing-service/target/*.jar app.jar
EXPOSE 8082
ENTRYPOINT ["java", "-jar", "app.jar"]
