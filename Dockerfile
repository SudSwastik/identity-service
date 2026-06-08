# Build stage
FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /workspace

RUN apk add --no-cache maven

COPY pom.xml .
# Pre-download dependencies (cached layer)
RUN mvn dependency:go-offline -q

COPY src src
RUN mvn clean package -DskipTests -q

# Runtime stage
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

RUN addgroup -S spring && adduser -S spring -G spring
USER spring

COPY --from=build /workspace/target/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
