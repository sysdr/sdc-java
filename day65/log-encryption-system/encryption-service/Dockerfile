FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml ./pom.xml
COPY encryption-service/pom.xml ./encryption-service/pom.xml
COPY encryption-service/src ./encryption-service/src
RUN mvn clean install -N -DskipTests && \
    cd encryption-service && \
    mvn clean package -DskipTests

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=build /app/encryption-service/target/encryption-service-*.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]
