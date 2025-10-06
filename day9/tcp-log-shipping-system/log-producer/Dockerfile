FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
COPY log-producer/pom.xml ./log-producer/
COPY log-producer/src ./log-producer/src
RUN mvn clean install -N -DskipTests
RUN mvn clean package -DskipTests -f log-producer/pom.xml

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=build /app/log-producer/target/*.jar app.jar
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "app.jar"]
