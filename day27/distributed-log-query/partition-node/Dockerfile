FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
COPY query-coordinator/pom.xml ./query-coordinator/
COPY partition-node/pom.xml ./partition-node/
COPY log-producer/pom.xml ./log-producer/
COPY partition-node/src ./partition-node/src
# Create minimal source directories for other modules to satisfy Maven validation
RUN mkdir -p query-coordinator/src/main/java query-coordinator/src/main/resources && \
    mkdir -p log-producer/src/main/java log-producer/src/main/resources
RUN mvn clean package -DskipTests -pl partition-node -am

FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /app/partition-node/target/*.jar app.jar
COPY partition-node/src/main/resources/application-template.yml /app/config/application.yml
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "app.jar", "--spring.config.location=file:/app/config/application.yml"]
