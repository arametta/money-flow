FROM maven:3.10.0-eclipse-temurin-25 AS build
WORKDIR /app
# Copy the pom alone first so the dependency download is cached until it changes.
COPY pom.xml .
RUN mvn -B -q dependency:go-offline
COPY src ./src
# Tests already run in CI.
RUN mvn -B -q package -DskipTests

FROM eclipse-temurin:25-jre AS runtime
# Don't run the app as root.
RUN groupadd --system app && useradd --system --gid app app
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
USER app
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
