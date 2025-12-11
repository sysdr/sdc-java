FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
COPY query-coordinator/pom.xml ./query-coordinator/
COPY partition-node/pom.xml ./partition-node/
COPY log-producer/pom.xml ./log-producer/
COPY query-coordinator/src ./query-coordinator/src
# Create minimal source directories for other modules to satisfy Maven validation
RUN mkdir -p partition-node/src/main/java partition-node/src/main/resources && \
    mkdir -p log-producer/src/main/java log-producer/src/main/resources
RUN mvn clean package -DskipTests -pl query-coordinator -am

FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /app/query-coordinator/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
