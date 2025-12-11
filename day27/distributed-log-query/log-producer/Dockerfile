FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
COPY query-coordinator/pom.xml ./query-coordinator/
COPY partition-node/pom.xml ./partition-node/
COPY log-producer/pom.xml ./log-producer/
COPY log-producer/src ./log-producer/src
# Create minimal source directories for other modules to satisfy Maven validation
RUN mkdir -p query-coordinator/src/main/java query-coordinator/src/main/resources && \
    mkdir -p partition-node/src/main/java partition-node/src/main/resources
RUN mvn clean package -DskipTests -pl log-producer -am

FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /app/log-producer/target/*.jar app.jar
EXPOSE 8084
ENTRYPOINT ["java", "-jar", "app.jar"]
