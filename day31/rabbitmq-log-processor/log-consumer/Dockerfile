FROM eclipse-temurin:17-jdk-alpine AS builder
WORKDIR /app
COPY pom.xml ./pom.xml
COPY log-consumer/pom.xml ./log-consumer/pom.xml
COPY log-consumer/src ./log-consumer/src
RUN apk add --no-cache maven && \
    cd log-consumer && \
    mvn clean package -DskipTests

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=builder /app/log-consumer/target/*.jar app.jar
EXPOSE 8082
ENTRYPOINT ["java", "-jar", "app.jar"]
