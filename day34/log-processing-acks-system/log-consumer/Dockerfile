FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn install -N -DskipTests
RUN mkdir -p api-gateway log-producer && \
    echo '<?xml version="1.0"?><project><modelVersion>4.0.0</modelVersion><parent><groupId>com.example</groupId><artifactId>log-processing-acks-system</artifactId><version>1.0.0</version></parent><artifactId>api-gateway</artifactId></project>' > api-gateway/pom.xml && \
    echo '<?xml version="1.0"?><project><modelVersion>4.0.0</modelVersion><parent><groupId>com.example</groupId><artifactId>log-processing-acks-system</artifactId><version>1.0.0</version></parent><artifactId>log-producer</artifactId></project>' > log-producer/pom.xml
COPY log-consumer/pom.xml ./log-consumer/
COPY log-consumer/src ./log-consumer/src
RUN mvn clean package -pl log-consumer -DskipTests

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=build /app/log-consumer/target/*.jar app.jar
EXPOSE 8082
ENTRYPOINT ["java", "-jar", "app.jar"]
