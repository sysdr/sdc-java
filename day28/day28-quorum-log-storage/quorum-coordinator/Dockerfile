FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml ./pom.xml
COPY quorum-coordinator/pom.xml ./quorum-coordinator/pom.xml
COPY quorum-coordinator/src ./quorum-coordinator/src
WORKDIR /app/quorum-coordinator
RUN mvn clean package -DskipTests

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=build /app/quorum-coordinator/target/*.jar app.jar
EXPOSE 8082
ENTRYPOINT ["java", "-jar", "app.jar"]
