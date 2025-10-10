FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
COPY log-shipper/pom.xml ./log-shipper/
COPY log-shipper/src ./log-shipper/src
COPY log-producer/pom.xml ./log-producer/
COPY log-consumer/pom.xml ./log-consumer/
COPY api-gateway/pom.xml ./api-gateway/
RUN mvn clean package -pl log-shipper -DskipTests

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=build /app/log-shipper/target/*.jar app.jar
EXPOSE 8082
ENTRYPOINT ["java", "-jar", "app.jar"]
