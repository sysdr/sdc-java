FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml ./pom.xml
COPY escalation-service/pom.xml ./escalation-service/pom.xml
COPY escalation-service/src ./escalation-service/src
WORKDIR /app/escalation-service
RUN mvn clean package spring-boot:repackage -DskipTests

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
# Spring Boot plugin creates executable JAR - copy it
COPY --from=build /app/escalation-service/target/escalation-service-*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
