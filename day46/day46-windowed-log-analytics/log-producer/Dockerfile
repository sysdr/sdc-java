FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
COPY log-producer ./log-producer
COPY window-processor/pom.xml ./window-processor/pom.xml
COPY api-gateway/pom.xml ./api-gateway/pom.xml
RUN mvn clean package -DskipTests -pl log-producer -am

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=build /app/log-producer/target/*.jar app.jar
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "app.jar"]
