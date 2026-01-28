FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
COPY log-producer/pom.xml log-producer/
COPY log-producer/src log-producer/src
COPY faceted-search-service/pom.xml faceted-search-service/
COPY aggregation-service/pom.xml aggregation-service/
COPY api-gateway/pom.xml api-gateway/
RUN mvn -pl log-producer -am clean package -DskipTests

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=build /app/log-producer/target/log-producer-*.jar app.jar
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "app.jar"]
