FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
COPY api-gateway/pom.xml ./api-gateway/
COPY api-gateway/src ./api-gateway/src
COPY log-producer/pom.xml ./log-producer/
COPY log-shipper/pom.xml ./log-shipper/
COPY log-consumer/pom.xml ./log-consumer/
RUN mvn clean package -pl api-gateway -DskipTests

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=build /app/api-gateway/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
