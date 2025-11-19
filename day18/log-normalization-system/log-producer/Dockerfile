FROM eclipse-temurin:17-jdk-alpine as builder
WORKDIR /app
COPY common/target/common-1.0.0.jar ./lib/
COPY log-producer/target/log-producer-1.0.0.jar ./app.jar

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=builder /app/lib/ ./lib/
COPY --from=builder /app/app.jar ./app.jar
EXPOSE 8082
ENTRYPOINT ["java", "-jar", "app.jar"]
