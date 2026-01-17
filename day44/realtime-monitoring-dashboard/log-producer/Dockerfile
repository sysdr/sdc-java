FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
# Copy parent POM
COPY pom.xml ./pom.xml
# Copy module POM and source
COPY log-producer/pom.xml ./log-producer/pom.xml
COPY log-producer/src ./log-producer/src
# Install parent POM first, then build module
RUN mvn install -N -DskipTests && \
    cd log-producer && \
    mvn clean package -DskipTests

FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /app/log-producer/target/*.jar app.jar
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "app.jar"]
