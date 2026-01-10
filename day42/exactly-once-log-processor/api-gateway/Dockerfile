FROM eclipse-temurin:17-jdk-alpine as build
WORKDIR /workspace/app

# Copy parent pom.xml and all modules for multi-module build
COPY pom.xml pom.xml
COPY log-producer/pom.xml log-producer/pom.xml
COPY log-producer/src log-producer/src
COPY log-consumer/pom.xml log-consumer/pom.xml
COPY log-consumer/src log-consumer/src
COPY api-gateway/pom.xml api-gateway/pom.xml
COPY api-gateway/src api-gateway/src

RUN apk add --no-cache maven
WORKDIR /workspace/app
RUN mvn clean package -pl api-gateway -am -DskipTests

FROM eclipse-temurin:17-jre-alpine
RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

COPY --from=build /workspace/app/api-gateway/target/*.jar app.jar

ENTRYPOINT ["java", "-jar", "/app.jar"]
