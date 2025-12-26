FROM maven:3.9-eclipse-temurin-17-alpine AS build
WORKDIR /app
COPY pom.xml /app/pom.xml
COPY log-consumer/pom.xml /app/log-consumer/pom.xml
COPY log-consumer/src /app/log-consumer/src
COPY common /app/common
COPY api-gateway/pom.xml /app/api-gateway/pom.xml
RUN mkdir -p /app/api-gateway/src && touch /app/api-gateway/src/.keep
RUN mvn clean package -DskipTests -pl log-consumer -am -f /app/pom.xml

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=build /app/log-consumer/target/*.jar app.jar
EXPOSE 8082
ENTRYPOINT ["java", "-jar", "app.jar"]
