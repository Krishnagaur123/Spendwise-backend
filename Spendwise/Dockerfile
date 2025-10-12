FROM eclipse-temurin:24-jdk AS build

WORKDIR /app

# Copy pom.xml and source
COPY pom.xml .
COPY src ./src

# Build the jar
RUN ./mvnw clean package -DskipTests

# Use a smaller runtime image
FROM eclipse-temurin:24-jre
WORKDIR /app

COPY --from=build /app/target/Spendwise-0.0.1-SNAPSHOT.jar Spendwise-v1.0.jar

EXPOSE 9090
ENTRYPOINT ["java", "-jar", "Spendwise-v1.0.jar"]
