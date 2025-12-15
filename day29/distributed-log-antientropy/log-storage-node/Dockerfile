FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml ./
COPY log-storage-node/pom.xml ./log-storage-node/pom.xml
COPY log-storage-node/src ./log-storage-node/src
WORKDIR /app/log-storage-node
RUN mvn clean package -DskipTests

FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /app/log-storage-node/target/*.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]
