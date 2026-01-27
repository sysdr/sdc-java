FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
# Copy parent POM
COPY pom.xml ./pom.xml
# Copy module POM and source
COPY alert-manager/pom.xml ./alert-manager/pom.xml
COPY alert-manager/src ./alert-manager/src
WORKDIR /app/alert-manager
RUN mvn clean package -DskipTests

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=build /app/alert-manager/target/*.jar app.jar
EXPOSE 8082
ENTRYPOINT ["java", "-jar", "app.jar"]
