FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
# Copy parent POM
COPY pom.xml ./pom.xml
# Copy all module POMs (Maven needs them to validate parent POM)
COPY storage-node/pom.xml ./storage-node/pom.xml
COPY cluster-coordinator/pom.xml ./cluster-coordinator/pom.xml
COPY write-gateway/pom.xml ./write-gateway/pom.xml
COPY read-gateway/pom.xml ./read-gateway/pom.xml
# Copy module source
COPY storage-node/src ./storage-node/src
# Build from parent
RUN mvn clean package -pl storage-node -am -DskipTests

FROM eclipse-temurin:17-jre
RUN apt-get update && apt-get install -y curl && rm -rf /var/lib/apt/lists/*
WORKDIR /app
COPY --from=build /app/storage-node/target/*.jar app.jar
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "app.jar"]
