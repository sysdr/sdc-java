FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml ./pom.xml
COPY normal-consumer/pom.xml ./normal-consumer/pom.xml
COPY normal-consumer/src ./normal-consumer/src
WORKDIR /app/normal-consumer
RUN mvn clean package spring-boot:repackage -DskipTests

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
# Spring Boot plugin creates executable JAR - copy it
COPY --from=build /app/normal-consumer/target/normal-consumer-*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
