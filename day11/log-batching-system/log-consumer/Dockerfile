FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
COPY log-consumer/pom.xml ./log-consumer/
COPY log-consumer/src ./log-consumer/src
COPY log-producer/pom.xml ./log-producer/
COPY log-shipper/pom.xml ./log-shipper/
COPY api-gateway/pom.xml ./api-gateway/
RUN mvn clean package -pl log-consumer -DskipTests

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=build /app/log-consumer/target/*.jar app.jar
EXPOSE 8083
ENTRYPOINT ["java", "-jar", "app.jar"]
