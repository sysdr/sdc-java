FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
# Copy parent POM
COPY pom.xml ./pom.xml
# Copy module POM and source
COPY alert-rule-engine/pom.xml ./alert-rule-engine/pom.xml
COPY alert-rule-engine/src ./alert-rule-engine/src
WORKDIR /app/alert-rule-engine
RUN mvn clean package -DskipTests

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=build /app/alert-rule-engine/target/*.jar app.jar
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "app.jar"]
