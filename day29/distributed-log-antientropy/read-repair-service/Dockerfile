FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml ./
COPY read-repair-service/pom.xml ./read-repair-service/pom.xml
COPY read-repair-service/src ./read-repair-service/src
WORKDIR /app/read-repair-service
RUN mvn clean package -DskipTests

FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /app/read-repair-service/target/*.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]
