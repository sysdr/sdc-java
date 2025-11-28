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
COPY cluster-coordinator/src ./cluster-coordinator/src
# Build from parent
RUN mvn clean package -pl cluster-coordinator -am -DskipTests

FROM eclipse-temurin:17-jre-alpine
RUN apk add --no-cache curl
WORKDIR /app
COPY --from=build /app/cluster-coordinator/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
