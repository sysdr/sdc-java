FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml ./pom.xml
COPY log-producer/pom.xml ./log-producer/pom.xml
COPY log-producer/src ./log-producer/src
WORKDIR /app/log-producer
RUN mvn clean package spring-boot:repackage -DskipTests

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
# Spring Boot plugin creates executable JAR - copy it
COPY --from=build /app/log-producer/target/log-producer-*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
