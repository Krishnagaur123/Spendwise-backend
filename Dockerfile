# ─── Build stage ─────────────────────────────────────────────────────────────
# Use a full JDK image to compile and package the application.
# eclipse-temurin is the recommended OpenJDK distribution for production.
FROM eclipse-temurin:24-jdk AS build

WORKDIR /app

# Copy the full project so Maven can resolve dependencies and run the build.
COPY . .

RUN chmod +x mvnw

# Skip tests in the Docker build — run tests separately in CI.
RUN ./mvnw clean package -DskipTests

# ─── Runtime stage ───────────────────────────────────────────────────────────
# Switch to a lean JRE-only image to minimise the final image size.
FROM eclipse-temurin:24-jre
WORKDIR /app

COPY --from=build /app/target/Spendwise-0.0.1-SNAPSHOT.jar Spendwise-v1.0.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "Spendwise-v1.0.jar"]
