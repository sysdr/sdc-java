FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
COPY state-producer/pom.xml ./state-producer/
COPY state-producer/src ./state-producer/src
RUN mvn install -N -DskipTests
WORKDIR /app/state-producer
RUN mvn clean package -DskipTests

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=build /app/state-producer/target/*.jar app.jar
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "app.jar"]
