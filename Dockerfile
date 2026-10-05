FROM eclipse-temurin:25-jdk AS build
WORKDIR /app
# Copy the wrapper and pom alone first so the dependency download is cached until they change.
COPY mvnw pom.xml ./
COPY .mvn .mvn
RUN ./mvnw -B -q dependency:go-offline
COPY src ./src
# Tests already run in CI.
RUN ./mvnw -B -q package -DskipTests

FROM eclipse-temurin:25-jre AS runtime
# Don't run the app as root.
RUN groupadd --system app && useradd --system --gid app app
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
USER app
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
