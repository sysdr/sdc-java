# Multi-stage build: compile in JDK, run in JRE-slim.
# Reduces image size from ~600MB to ~180MB.
FROM maven:3.9.6-eclipse-temurin-17 AS build

WORKDIR /build

# 1. Copy ONLY parent pom first (critical for dependencyManagement)
COPY pom.xml .

# 2. Create empty module directories to satisfy parent POM validation
RUN mkdir -p api-gateway log-producer log-consumer

# 3. Copy only POM files first (needed for parent POM validation)
COPY api-gateway/pom.xml api-gateway/pom.xml
COPY log-producer/pom.xml log-producer/pom.xml
COPY log-consumer/pom.xml log-consumer/pom.xml

# 4. Download and install spring-boot-dependencies BOM directly
RUN mkdir -p /root/.m2/repository/org/springframework/boot/spring-boot-dependencies/3.2.4/ && \
    curl -sL https://repo1.maven.org/maven2/org/springframework/boot/spring-boot-dependencies/3.2.4/spring-boot-dependencies-3.2.4.pom \
    -o /root/.m2/repository/org/springframework/boot/spring-boot-dependencies/3.2.4/spring-boot-dependencies-3.2.4.pom && \
    mvn -B -q install:install-file \
    -Dfile=/root/.m2/repository/org/springframework/boot/spring-boot-dependencies/3.2.4/spring-boot-dependencies-3.2.4.pom \
    -DgroupId=org.springframework.boot \
    -DartifactId=spring-boot-dependencies \
    -Dversion=3.2.4 \
    -Dpackaging=pom || true

# 5. Install parent POM (non-recursive, so it doesn't try to build child modules)
# This makes dependencyManagement available to child modules
RUN mvn -B -q install -N

# 6. Resolve dependencies for all modules (now parent is installed, BOM is available)
RUN mvn -B -q dependency:go-offline || true

# 9. Copy source code for all modules
COPY api-gateway/src api-gateway/src
COPY log-producer/src log-producer/src
COPY log-consumer/src log-consumer/src

# 10. Build only the api-gateway module
RUN mvn -B clean package -pl api-gateway -am -DskipTests

# ---------- Runtime ----------
FROM eclipse-temurin:17-jre

WORKDIR /app
COPY --from=build /build/api-gateway/target/*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java","-jar","app.jar"]
