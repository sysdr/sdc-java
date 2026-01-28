FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml ./pom.xml
COPY search-api/pom.xml ./search-api/pom.xml
COPY search-api/src ./search-api/src
WORKDIR /app/search-api
RUN mvn clean package -DskipTests

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=build /app/search-api/target/*.jar app.jar
EXPOSE 8083
ENTRYPOINT ["java", "-jar", "app.jar"]
