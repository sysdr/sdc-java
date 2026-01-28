FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
COPY aggregation-service/pom.xml aggregation-service/
COPY aggregation-service/src aggregation-service/src
COPY log-producer/pom.xml log-producer/
COPY faceted-search-service/pom.xml faceted-search-service/
COPY api-gateway/pom.xml api-gateway/
RUN mvn -pl aggregation-service -am clean package -DskipTests

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=build /app/aggregation-service/target/aggregation-service-*.jar app.jar
EXPOSE 8083
ENTRYPOINT ["java", "-jar", "app.jar"]
