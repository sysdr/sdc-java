FROM eclipse-temurin:17-jdk-alpine as build
WORKDIR /workspace/app

# Copy parent pom and all module poms (Maven needs all modules for parent validation)
COPY pom.xml ./pom.xml
COPY metrics-aggregator/pom.xml ./metrics-aggregator/pom.xml
COPY dashboard-service/pom.xml ./dashboard-service/pom.xml
COPY metrics-generator/pom.xml ./metrics-generator/pom.xml
# Copy only the source for the module we're building
COPY metrics-generator/src ./metrics-generator/src

WORKDIR /workspace/app
RUN apk add --no-cache maven
RUN mvn clean package -pl metrics-generator -am -DskipTests

FROM eclipse-temurin:17-jre-alpine
COPY --from=build /workspace/app/metrics-generator/target/*.jar app.jar
ENTRYPOINT ["java", "-jar", "/app.jar"]
