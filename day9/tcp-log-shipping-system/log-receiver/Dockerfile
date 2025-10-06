FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
COPY log-receiver/pom.xml ./log-receiver/
COPY log-receiver/src ./log-receiver/src
RUN mvn clean install -N -DskipTests
RUN mvn clean package -DskipTests -f log-receiver/pom.xml

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=build /app/log-receiver/target/*.jar app.jar
EXPOSE 8082 9090
ENTRYPOINT ["java", "-jar", "app.jar"]
