FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml ./pom.xml
COPY query-coordinator/pom.xml ./query-coordinator/pom.xml
COPY query-coordinator/src ./query-coordinator/src
WORKDIR /app/query-coordinator
RUN mvn clean package -DskipTests

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=build /app/query-coordinator/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
