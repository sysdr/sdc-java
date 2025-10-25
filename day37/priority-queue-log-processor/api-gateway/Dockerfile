FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml ./pom.xml
COPY api-gateway/pom.xml ./api-gateway/pom.xml
COPY api-gateway/src ./api-gateway/src
WORKDIR /app/api-gateway
RUN mvn clean package spring-boot:repackage -DskipTests

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
# Spring Boot plugin creates executable JAR - copy it
COPY --from=build /app/api-gateway/target/api-gateway-*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
