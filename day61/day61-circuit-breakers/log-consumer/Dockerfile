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

# 4. Download Spring Boot parent and dependencies BOM first
RUN mvn -B -q dependency:get \
    -Dartifact=org.springframework.boot:spring-boot-starter-parent:3.2.4:pom \
    -Dtransitive=false || true

# 5. Install parent POM to local repository (non-recursive)
# This makes dependencyManagement available to child modules
RUN mvn -B -q install -N

# 6. Pre-resolve dependencies to ensure all POMs are available
RUN mvn -B -q dependency:resolve -pl log-consumer -DskipTests || true

# 9. Copy source code for all modules
COPY api-gateway/src api-gateway/src
COPY log-producer/src log-producer/src
COPY log-consumer/src log-consumer/src

# 10. Build from the module's own directory to avoid multi-module validation issues
RUN cd log-consumer && mvn -B clean package -DskipTests -U

# ---------- Runtime ----------
FROM eclipse-temurin:17-jre

WORKDIR /app
COPY --from=build /build/log-consumer/target/*.jar app.jar

EXPOSE 8082
ENTRYPOINT ["java","-jar","app.jar"]
