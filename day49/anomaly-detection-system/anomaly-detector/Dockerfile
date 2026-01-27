FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml ./pom.xml
COPY anomaly-detector/pom.xml ./anomaly-detector/pom.xml
COPY anomaly-detector/src ./anomaly-detector/src
RUN mvn install -N -DskipTests && cd anomaly-detector && mvn clean package -DskipTests

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=build /app/anomaly-detector/target/anomaly-detector-*.jar app.jar
EXPOSE 8082
ENTRYPOINT ["java", "-jar", "app.jar"]
