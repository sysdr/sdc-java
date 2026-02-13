FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml ./pom.xml
COPY query-service/pom.xml ./query-service/pom.xml
COPY query-service/src ./query-service/src
RUN mvn clean install -N -DskipTests && \
    cd query-service && \
    mvn clean package -DskipTests

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=build /app/query-service/target/query-service-*.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]
