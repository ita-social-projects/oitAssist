FROM eclipse-temurin:25
WORKDIR /runner
COPY target/OITAssist-0.0.1-SNAPSHOT.jar app.jar
CMD ["java", "-jar", "app.jar"]
