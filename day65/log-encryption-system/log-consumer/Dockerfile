FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml ./pom.xml
COPY log-consumer/pom.xml ./log-consumer/pom.xml
COPY log-consumer/src ./log-consumer/src
RUN mvn clean install -N -DskipTests && \
    cd log-consumer && \
    mvn clean package -DskipTests

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=build /app/log-consumer/target/log-consumer-*.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]
