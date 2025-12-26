FROM maven:3.9-eclipse-temurin-17-alpine AS build
WORKDIR /app
COPY pom.xml /app/pom.xml
COPY api-gateway/pom.xml /app/api-gateway/pom.xml
COPY api-gateway/src /app/api-gateway/src
COPY common /app/common
COPY log-consumer/pom.xml /app/log-consumer/pom.xml
RUN mkdir -p /app/log-consumer/src && touch /app/log-consumer/src/.keep
RUN mvn clean package -DskipTests -pl api-gateway -am -f /app/pom.xml

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=build /app/api-gateway/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
