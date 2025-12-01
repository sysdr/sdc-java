FROM eclipse-temurin:17-jdk-alpine AS build
WORKDIR /app
# Copy parent POM
COPY pom.xml ./pom.xml
# Copy all modules (Maven parent POM requires all modules to exist)
COPY common ./common
COPY api-gateway ./api-gateway
COPY storage-coordinator ./storage-coordinator
COPY storage-node ./storage-node
RUN apk add --no-cache maven
# Build from root to ensure common module is built first
RUN mvn clean install -DskipTests -pl storage-coordinator -am

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=build /app/storage-coordinator/target/*.jar app.jar
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "app.jar"]
