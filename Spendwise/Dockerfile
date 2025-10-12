FROM eclipse-temurin:24-jre

WORKDIR /app

COPY target/Spendwise-0.0.1-SNAPSHOT.jar Spendwise-v1.0.jar

EXPOSE 9090

ENTRYPOINT ["java", "-jar", "Spendwise-v1.0.jar"]
