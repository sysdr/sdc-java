FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
COPY window-processor ./window-processor
COPY log-producer/pom.xml ./log-producer/pom.xml
COPY api-gateway/pom.xml ./api-gateway/pom.xml
RUN mvn clean package -DskipTests -pl window-processor -am

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=build /app/window-processor/target/*.jar app.jar
EXPOSE 8082
ENTRYPOINT ["java", "-jar", "app.jar"]
