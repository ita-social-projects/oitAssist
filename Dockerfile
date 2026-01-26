FROM eclipse-temurin:25 as runner
WORKDIR runner
COPY **/target/app.jar runner/
CMD java -jar runner/app.jar
