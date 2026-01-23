FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
COPY api-gateway ./api-gateway
COPY log-producer/pom.xml ./log-producer/pom.xml
COPY window-processor/pom.xml ./window-processor/pom.xml
RUN mvn clean package -DskipTests -pl api-gateway -am

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=build /app/api-gateway/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
