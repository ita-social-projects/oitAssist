FROM eclipse-temurin:25
WORKDIR /runner
COPY target/*.jar app.jar
CMD ["java", "-jar", "app.jar"]
