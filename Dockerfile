FROM eclipse-temurin:25-jre
WORKDIR /app
COPY target/*.jar app.jar
EXPOSE 80
ENTRYPOINT ["java", "-jar", "app.jar"]