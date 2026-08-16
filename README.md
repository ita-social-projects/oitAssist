# OITAssist 🚀

[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)
[![Github Issues](https://img.shields.io/github/issues/ita-social-projects/oitAssist?style=flat-square)](https://github.com/ita-social-projects/oitAssist/issues)
[![Pending Pull-Requests](https://img.shields.io/github/issues-pr/ita-social-projects/oitAssist?style=flat-square)](https://github.com/ita-social-projects/oitAssist/pulls)
[![GitHub contributors](https://img.shields.io/github/contributors/ita-social-projects/oitAssist?style=flat)](https://github.com/ita-social-projects/oitAssist/graphs/contributors)

OITAssist is an enterprise-grade, modern, and modular monolithic platform designed to orchestrate and manage technical and programming competitions (such as the **Olympiad in Information Technology - OIT**). It supports hierarchical competition planning, advanced multi-provider file storage, automated scheduling, secure multi-role JWT authentication, and robust asynchronous email workflows.

Built on top of **Spring Boot 4.0.1** and **Java 25**, OITAssist leverages **Spring Modulith** to maintain a clean domain boundary with event-driven integration and strict package decoupling.

---

## 🌟 Key Features

*   **Modular Monolith Architecture**: Uses **Spring Modulith** to enforce package boundaries and ensure compile-time verification of modular dependencies (cycle-free).
*   **Hierarchical Competition Lifecycle**: Full lifecycle management of competitions, stages, and tours (e.g., `DRAFT` ➡️ `PUBLISHED` ➡️ `FINISHED` ➡️ `ARCHIVED`) with structural integrity validations.
*   **Secure Authentication (JWT)**: JSON Web Token-based authentication using HS256/RS256 with signing and encryption keys, supporting both Access and Refresh tokens.
*   **Role-Based Access Control (RBAC)**: Support for multiple system roles: `USER`, `AUTHOR`, `JURY`, `ORG`, and `ADMIN`.
*   **Dual-Provider File Management**: Highly configurable file subsystem supporting local disk storage and Microsoft SharePoint cloud storage (integrated via Azure Identity and Microsoft Graph SDK).
*   **Asynchronous Registration Lifecycle**: Uses Modulith's transactional event publisher to trigger activation token generation, send HTML emails asynchronously via **FreeMarker templates**, and track activation statuses.
*   **Daily Schedulers & Cleanups**:
    *   *News Archiver*: Automatically archives published announcements older than 30 days.
    *   *File Purger*: Regularly sweeps temporary, orphaned, and soft-deleted file uploads.
*   **Full Observability Stack**: Actuator integration with **Micrometer Prometheus** metrics and **Grafana Alloy** (scraping Prometheus metrics and Loki logs) to stream data to Grafana Cloud.
*   **Liquibase Migration**: Managed database schema versioning with structured XML migration logs.
*   **Automated Formatting & Formatting Checks**: Continuous integration formatting check with Maven Checkstyle and XML-based formatters.

---

## 🛠️ Tech Stack & Versions

| Category | Technology | Version |
| :--- | :--- | :--- |
| **Core Framework** | Java | 25 |
| | Spring Boot | 4.0.1 |
| | Spring Modulith | 2.0.1 |
| **Database & Migrations**| PostgreSQL | 16 |
| | Spring Data JPA / Hibernate | (Spring Boot parent managed) |
| | Liquibase | (Spring Boot parent managed) |
| **Security & Auth** | Spring Security | (Spring Boot parent managed) |
| | io.jsonwebtoken (jjwt) | 0.13.0 |
| **Email & Templating** | Spring Mail | (Spring Boot parent managed) |
| | FreeMarker | (Spring Boot parent managed) |
| **Integration** | Microsoft Graph SDK | 6.62.0 |
| | Azure Identity | 1.13.1 |
| **API Documentation** | Springdoc OpenAPI UI | 3.0.1 |
| **Logging & Metrics** | Spring Actuator & Prometheus | (Spring Boot parent managed) |
| | Zalando Logbook | 4.0.2 |
| | Grafana Alloy | latest |
| **Mappers & Utils** | MapStruct | 1.6.3 |
| | Lombok | (Spring Boot parent managed) |
| **Build & Quality** | Maven | 3.9+ |
| | Checkstyle Plugin | 3.6.0 |
| | Formatter Plugin | 2.29.0 |

---

## 📋 Prerequisites

Before running the application locally, make sure you have:

*   **Java 25 Development Kit (JDK)**
*   **Maven 3.9+**
*   **Docker & Docker Compose** (for database and logging agents)
*   An active SMTP server (e.g., Gmail) or Microsoft Graph configuration for cloud operations.

---

## 🚀 How to Run / Getting Started

### 1. Database Setup
Spin up the PostgreSQL database container using Docker Compose:
```bash
docker compose up -d postgres
```
This runs PostgreSQL on port `5432` with a default database named `oitassist`.

### 2. Configure Environment Variables
Copy and configure environment variables. Check [ENVIRONMENT_VARIABLES.md](ENVIRONMENT_VARIABLES.md) for a comprehensive guide on all available keys.

> [!IMPORTANT]
> Since the project uses external services (SharePoint storage, database credentials, JWT keys, and SMTP mail), **ask your teammates** for the current development `.env` file or secrets manager keys. Do not guess these values.

You can configure them in your OS terminal or create a local `.env` file:
```bash
export DATASOURCE_URL=jdbc:postgresql://localhost:5432/oitassist
export DATASOURCE_USERNAME=your_db_user
export DATASOURCE_PASSWORD=your_db_password
export SPRING_LIQUIBASE_ENABLED=true
export JWT_ENCRYPTED_KEY=your-32-byte-base64-key-here
export JWT_SIGN_KEY=your-jwt-signing-secret-here
```

### 3. Build the Application
Verify code style, validate formatting, and compile the code:
```bash
mvn clean compile
```

### 4. Run the Dev Server

#### Option A: Run via CLI
Launch the Spring Boot application locally:
```bash
./mvnw spring-boot:run
```
The server starts on `http://localhost:8080` by default.

#### Option B: Run via IntelliJ IDEA
1. Open IntelliJ IDEA, select **Open** or **Import**, and choose the project's root `pom.xml`.
2. Allow Maven to import all dependencies.
3. Enable annotation processing (required for Lombok and MapStruct):
   * Go to `File` ➡️ `Settings` (or `IntelliJ IDEA` ➡️ `Preferences` on macOS).
   * Navigate to `Build, Execution, Deployment` ➡️ `Compiler` ➡️ `Annotation Processors`.
   * Check the box for **Enable annotation processing** and click **Apply/OK**.
4. Open the [OitAssistApplication.java](file: src/main/java/com/itasocialacademy/oitassist/OitAssistApplication.java) file.
5. Click the green play icon next to the class definition or main method and select **Run 'OitAssistApplication'**.
6. *To set environment variables:* Open the Run/Debug Configurations dropdown, edit the configuration for `OitAssistApplication`, and enter the required keys in the **Environment variables** field.


---

## ⚙️ Configuration & Profiles

Important configurations in [application.yaml](src/main/resources/application.yaml):
*   **Storage Providers**: Can toggle between `LOCAL` and `SHAREPOINT` by updating environment properties.
*   **Orphan Cleanups**: Configured under `app.filemanager.cleanup.*` using cron patterns.
*   **News Archiving**: Configured under `app.news.archiving.cron`.
*   **JWT Validity**: Managed through `jwt.validity` (access token) and `jwt.refresh-validity` (refresh token).

For deep monitoring configurations (metrics pushing to Grafana Cloud), check [config.alloy](config.alloy) and run the logging agent with:
```bash
docker compose -f docker-compose-grafana.yml up -d
```

---

## 📖 API Documentation

Interactive API documentation is generated using **Springdoc OpenAPI** and Swagger UI.
*   **Swagger UI URL**: [http://localhost:8080/docs](http://localhost:8080/docs) (mapped to `/docs` in `application.yaml`)
*   **OpenAPI JSON**: `http://localhost:8080/v3/api-docs`

---

## 📂 Project Structure & Architecture

OITAssist strictly adheres to a **modular monolithic** structure. Every major functional domain is isolated in its own package under `com.itasocialacademy.oitassist`.

```
src/main/java/com/itasocialacademy/oitassist
├── OitAssistApplication.java     # Boot & Modulith entrypoint
│
├── core/                         # Shared utilities, generic CRUD controllers, config, exceptions
│
├── auth/                         # User registration & verification flow, event listeners
├── security/                     # Spring Security, JWT, Access/Refresh tokens
├── user/                         # Profile details, Roles, Activation tokens, UserFacade
│
├── competition/                  # Competitions, Stages, Tours CRUD & rules
├── task/                         # Competitive programming tasks
│
├── filemanager/                  # File storage provider (Local/SharePoint), File sweeps & cleaners
├── news/                         # News feeds, Draft-to-Published lifecycle, Auto-archiver scheduler
│
└── chat/                         # [Stub] Future Real-time messages & chats
└── submission/                   # [Stub] Future code submission records
└── evaluation/                   # [Stub] Future scoring engine
└── usercompetition/              # [Stub] Future user registration to competitions
```

### Dependency Rules (Spring Modulith)
*   Cross-module references must only go through package facades (e.g. `UserFacade` in `user::api`).
*   Modulith verification runs on every test suite execution to prevent circular dependencies.

---

## 🧪 Testing

To run the unit and integration tests (including the Spring Modulith architectural compliance test):
```bash
mvn test
```

To run formatting checks only:
```bash
mvn checkstyle:check
```

---

## 🚢 Deployment

The project is dockerized. The included multi-stage environment build compiles a target jar and prepares it inside a minimal runtime environment.

Build the docker image:
```bash
docker build -t oitassist-backend .
```
Or run the complete docker-compose environment:
```bash
docker compose up -d
```
The Docker container exposes Port `80` by default and maps it internally to port `8080`.

---

## 🤝 Contributing

We welcome contributions! Please adhere to the following steps:
1.  Verify checkstyle and formatting before opening a PR:
2.  Follow the [Setup Checkstyle Wiki guide](https://github.com/ita-social-projects/oitAssist/wiki/Setup-CheckStyle-and-Formatter-to-your-IDE) to integrate formatter profiles directly into your IDE.
3.  Add unit and integration tests for new modules.

---

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.