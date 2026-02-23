# Build stage
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /workspace

# Copy parent POM first for dependency caching
COPY pom.xml .
COPY common-lib/pom.xml common-lib/pom.xml
COPY discovery-server/pom.xml discovery-server/pom.xml
COPY api-gateway/pom.xml api-gateway/pom.xml
COPY auth-service/pom.xml auth-service/pom.xml
COPY device-service/pom.xml device-service/pom.xml
COPY weather-service/pom.xml weather-service/pom.xml

# Download dependencies (cached unless POMs change)
RUN mvn dependency:go-offline -pl device-service -am -q || true

# Copy source code
COPY common-lib/src common-lib/src
COPY device-service/src device-service/src

# Build
RUN mvn package -pl device-service -am -DskipTests -q

# Runtime stage
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=build /workspace/device-service/target/*.jar app.jar
EXPOSE 8082
ENTRYPOINT ["java", "-jar", "app.jar"]
