FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
COPY state-query-api/pom.xml ./state-query-api/
COPY state-query-api/src ./state-query-api/src
RUN mvn install -N -DskipTests
WORKDIR /app/state-query-api
RUN mvn clean package -DskipTests

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=build /app/state-query-api/target/*.jar app.jar
EXPOSE 8083
ENTRYPOINT ["java", "-jar", "app.jar"]
