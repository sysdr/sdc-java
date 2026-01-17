FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
# Copy parent POM
COPY pom.xml ./pom.xml
# Copy module POM and source
COPY dashboard-api/pom.xml ./dashboard-api/pom.xml
COPY dashboard-api/src ./dashboard-api/src
# Install parent POM first, then build module
RUN mvn install -N -DskipTests && \
    cd dashboard-api && \
    mvn clean package -DskipTests

FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /app/dashboard-api/target/*.jar app.jar
EXPOSE 8083
ENTRYPOINT ["java", "-jar", "app.jar"]
