# Build stage
FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /app

# Copy only pom and wrapper first for better caching
COPY ["pom.xml", "mvnw", "mvnw.cmd", "./"]
COPY .mvn/ .mvn/

RUN chmod +x mvnw
# Resolve dependencies
RUN ./mvnw dependency:go-offline -B

# Copy source and build
COPY src/ ./src/
RUN ./mvnw -DskipTests package

# Run stage
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/target/*.jar /app/app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]

