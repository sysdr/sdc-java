FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml ./pom.xml
COPY auth-service ./auth-service
COPY api-gateway ./api-gateway
COPY log-query-service ./log-query-service
COPY audit-service ./audit-service
RUN mvn clean package -pl log-query-service -am -DskipTests

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=build /app/log-query-service/target/log-query-service-*.jar app.jar
RUN ls -lh /app/app.jar
EXPOSE 8083
ENTRYPOINT ["java", "-jar", "app.jar"]
