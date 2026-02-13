FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml ./pom.xml
COPY log-producer/pom.xml ./log-producer/pom.xml
COPY log-producer/src ./log-producer/src
RUN mvn clean install -N -DskipTests && \
    cd log-producer && \
    mvn clean package -DskipTests

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=build /app/log-producer/target/log-producer-*.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]
