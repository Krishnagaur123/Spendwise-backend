
FROM eclipse-temurin:24-jdk AS build

WORKDIR /app


COPY . .


RUN chmod +x mvnw


RUN ./mvnw clean package -DskipTests


FROM eclipse-temurin:24-jre
WORKDIR /app


COPY --from=build /app/target/Spendwise-0.0.1-SNAPSHOT.jar Spendwise-v1.0.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "Spendwise-v1.0.jar"]
