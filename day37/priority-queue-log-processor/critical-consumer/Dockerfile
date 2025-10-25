FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml ./pom.xml
COPY critical-consumer/pom.xml ./critical-consumer/pom.xml
COPY critical-consumer/src ./critical-consumer/src
WORKDIR /app/critical-consumer
RUN mvn clean package spring-boot:repackage -DskipTests

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
# Spring Boot plugin creates executable JAR - copy it
COPY --from=build /app/critical-consumer/target/critical-consumer-*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
