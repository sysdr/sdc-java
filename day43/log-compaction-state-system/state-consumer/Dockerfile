FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
COPY state-consumer/pom.xml ./state-consumer/
COPY state-consumer/src ./state-consumer/src
RUN mvn install -N -DskipTests
WORKDIR /app/state-consumer
RUN mvn clean package -DskipTests

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=build /app/state-consumer/target/*.jar app.jar
EXPOSE 8082
ENTRYPOINT ["java", "-jar", "app.jar"]
