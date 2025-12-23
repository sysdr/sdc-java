FROM eclipse-temurin:17-jdk-alpine AS builder
WORKDIR /app
COPY pom.xml ./pom.xml
COPY log-producer/pom.xml ./log-producer/pom.xml
COPY log-producer/src ./log-producer/src
RUN apk add --no-cache maven && \
    cd log-producer && \
    mvn clean package -DskipTests

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=builder /app/log-producer/target/*.jar app.jar
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "app.jar"]
