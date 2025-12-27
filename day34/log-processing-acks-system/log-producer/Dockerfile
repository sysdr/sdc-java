FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn install -N -DskipTests
RUN mkdir -p api-gateway log-consumer && \
    echo '<?xml version="1.0"?><project><modelVersion>4.0.0</modelVersion><parent><groupId>com.example</groupId><artifactId>log-processing-acks-system</artifactId><version>1.0.0</version></parent><artifactId>api-gateway</artifactId></project>' > api-gateway/pom.xml && \
    echo '<?xml version="1.0"?><project><modelVersion>4.0.0</modelVersion><parent><groupId>com.example</groupId><artifactId>log-processing-acks-system</artifactId><version>1.0.0</version></parent><artifactId>log-consumer</artifactId></project>' > log-consumer/pom.xml
COPY log-producer/pom.xml ./log-producer/
COPY log-producer/src ./log-producer/src
RUN mvn clean package -pl log-producer -DskipTests

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=build /app/log-producer/target/*.jar app.jar
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "app.jar"]
