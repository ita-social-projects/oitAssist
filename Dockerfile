# STAGE 1: Build the Frontend (Node.js)
FROM node:20-alpine AS frontend-build
WORKDIR /frontend
# Install pnpm (since your YAML used it)
RUN npm install -g pnpm
COPY frontend/package.json frontend/pnpm-lock.yaml ./
RUN pnpm install
COPY frontend/ ./
RUN pnpm run build

# STAGE 2: Build the Backend (Java 25)
# Change this line:
FROM maven:3.9.9-eclipse-temurin-25 AS backend-build
WORKDIR /app
COPY . .
COPY --from=frontend-build /frontend/dist/ src/main/resources/static/
# Now 'mvn' will work
RUN mvn clean package -DskipTests

# STAGE 3: Final Runtime
FROM eclipse-temurin:25-jre-alpine
WORKDIR /app
# Copy only the final JAR from STAGE 2
COPY --from=backend-build /app/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]