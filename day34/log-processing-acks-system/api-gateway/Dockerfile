FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn install -N -DskipTests
RUN mkdir -p log-producer log-consumer && \
    echo '<?xml version="1.0"?><project><modelVersion>4.0.0</modelVersion><parent><groupId>com.example</groupId><artifactId>log-processing-acks-system</artifactId><version>1.0.0</version></parent><artifactId>log-producer</artifactId></project>' > log-producer/pom.xml && \
    echo '<?xml version="1.0"?><project><modelVersion>4.0.0</modelVersion><parent><groupId>com.example</groupId><artifactId>log-processing-acks-system</artifactId><version>1.0.0</version></parent><artifactId>log-consumer</artifactId></project>' > log-consumer/pom.xml
COPY api-gateway/pom.xml ./api-gateway/
COPY api-gateway/src ./api-gateway/src
RUN mvn clean package -pl api-gateway -DskipTests

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=build /app/api-gateway/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
