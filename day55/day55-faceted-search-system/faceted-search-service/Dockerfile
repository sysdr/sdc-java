FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
COPY faceted-search-service/pom.xml faceted-search-service/
COPY faceted-search-service/src faceted-search-service/src
COPY log-producer/pom.xml log-producer/
COPY aggregation-service/pom.xml aggregation-service/
COPY api-gateway/pom.xml api-gateway/
RUN mvn -pl faceted-search-service -am clean package -DskipTests

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=build /app/faceted-search-service/target/faceted-search-service-*.jar app.jar
EXPOSE 8082
ENTRYPOINT ["java", "-jar", "app.jar"]
