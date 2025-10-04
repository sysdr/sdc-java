FROM eclipse-temurin:17-jdk-alpine AS build
WORKDIR /app
COPY pom.xml .
COPY log-server/pom.xml ./log-server/
COPY log-server/src ./log-server/src
RUN apk add --no-cache maven && mvn clean package -DskipTests -pl log-server

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=build /app/log-server/target/*.jar app.jar
EXPOSE 8080 9090
ENTRYPOINT ["java", "-jar", "app.jar"]
