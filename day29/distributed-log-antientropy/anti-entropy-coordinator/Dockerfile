FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml ./
COPY anti-entropy-coordinator/pom.xml ./anti-entropy-coordinator/pom.xml
COPY anti-entropy-coordinator/src ./anti-entropy-coordinator/src
WORKDIR /app/anti-entropy-coordinator
RUN mvn clean package -DskipTests

FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /app/anti-entropy-coordinator/target/*.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]
