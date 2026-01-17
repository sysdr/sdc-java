FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
# Copy parent POM
COPY pom.xml ./pom.xml
# Copy module POM and source
COPY stream-processor/pom.xml ./stream-processor/pom.xml
COPY stream-processor/src ./stream-processor/src
# Install parent POM first, then build module
RUN mvn install -N -DskipTests && \
    cd stream-processor && \
    mvn clean package -DskipTests

FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /app/stream-processor/target/*.jar app.jar
EXPOSE 8082
ENTRYPOINT ["java", "-jar", "app.jar"]
