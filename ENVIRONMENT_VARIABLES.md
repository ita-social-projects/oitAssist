# ENVIRONMENT VARIABLES
Documentation of all environment variables used in the project

This file describes every environment variable referenced in `application.yml` / `application.yaml` and Logback configuration.

**Important**
- Never commit real secrets (passwords, keys, JWT secrets) to the repository
- Use a secrets manager (Vault, AWS Secrets Manager, Doppler, 1Password, Kubernetes Secrets, etc.)

## List of environment variables

| Variable                        | Description                                                               | Required | Default value (if not set)           | Example real value                                              | Where to store / obtain real value               |
|---------------------------------|---------------------------------------------------------------------------|----------|--------------------------------------|-----------------------------------------------------------------|--------------------------------------------------|
| `APP_NAME`                      | Spring Boot application name                                              | No       | `oit-assist`                         | `oit-assist-prod`                                               | Usually in yaml or CI/CD metadata                |
| `DATASOURCE_URL`                | Full JDBC URL to connect to the database                                  | Yes      | — (fails if missing)                 | `jdbc:postgresql://db.example.com:5432/prod_db`                 | Secrets manager / Kubernetes Secret              |
| `DATASOURCE_USERNAME`           | Database username                                                         | Yes      | —                                    | `app_user_prod`                                                 | Secrets manager                                  |
| `DATASOURCE_PASSWORD`           | Database password                                                         | Yes      | —                                    | `s3cr3t-v3ry-l0ng-p@ssw0rd`                                     | Secrets manager (never in git!)                  |
| `SPRING_LIQUIBASE_ENABLED`      | Whether to enable Liquibase on application startup                        | No       | `false`                              | `true`                                                          | Usually `true` in all environments except local  |
| `SPRING_LIQUIBASE_CHANGE_LOG`   | Path to the main Liquibase changelog file                                 | No       | — (falls back to yaml value)         | `classpath:db/changelog/db.changelog-master.yaml`               | Usually kept in yaml                             |
| `SPRING_LIQUIBASE_DROP_FIRST`   | Whether to drop all database objects before applying migrations           | No       | `false`                              | `true` (only for tests/dev – dangerous!)                        | Only for special / destructive scenarios         |
| `JPA_SHOW_SQL`                  | Whether to log all SQL statements executed by Hibernate                   | No       | — (usually false)                    | `true`                                                          | `true` in dev, `false` in prod                   |
| `JPA_HIBERNATE_DDL_AUTO`        | Hibernate schema generation strategy (`hibernate.hbm2ddl.auto`)           | No       | `validate`                           | `validate` / `update` / `create-drop` / `none`                  | `validate` is safest for production              |
| `JWT_ENCRYPTED_KEY`             | Key used to encrypt sensitive data inside JWT (if implemented)            | Yes      | —                                    | 32-byte key in base64 or hex                                    | Secrets manager / Vault                          |
| `JWT_SIGN_KEY`                  | Signing key for JWT (HS256 / RS256 / etc.)                                | Yes      | —                                    | Very long random string or RSA private key                      | Secrets manager                                  |
| `JWT_VALIDITY`                  | Token validity duration in milliseconds                                   | Yes      | —                                    | `3600000` (1 hour) / `86400000` (24 hours)                      | Usually set in yaml, rarely overridden           |
| `LOG_LEVEL_ROOT`                | Root logging level                                                        | No       | `INFO`                               | `INFO` / `DEBUG` / `WARN` / `ERROR`                             | Usually `INFO` in prod, `DEBUG` in dev           |
| `LOG_LEVEL_APP`                 | Logging level for application package (`com.itasocialacademy.oitassist`)  | No       | `DEBUG`                              | `DEBUG` / `INFO`                                                | `DEBUG` in dev, `INFO` in prod                   |
| `LOG_LEVEL_SPRING`              | Logging level for Spring Framework packages                               | No       | `INFO`                               | `INFO` / `DEBUG` / `WARN`                                       | Usually `INFO` or `WARN` in prod                 |
| `LOG_LEVEL_LOGBOOK`             | Logging level for Zalando Logbook (HTTP request/response logging)         | No       | `TRACE`                              | `TRACE` / `DEBUG` / `INFO`                                      | `TRACE` for detailed request logging in dev      |
| `LOG_FILE`                      | Path to the log file                                                      | No       | `logs/app.log`                       | `/var/log/oit-assist/app.log`                                   | Container/VM filesystem or volume mount          |
| `LOG_MAX_HISTORY`               | Number of days / rolled files to keep (Logback rolling policy)            | No       | `14`                                 | `30`                                                            | Usually via CI/CD or helm values                 |
| `LOG_TOTAL_SIZE_CAP`            | Maximum total size of all log files before oldest are deleted             | No       | `2GB`                                | `10GB`                                                          | Adjust depending on disk space                   |
| `LOG_ASYNC_QUEUE_SIZE`          | Queue size for async logging (prevents blocking on I/O spikes)            | No       | `2048`                               | `4096`                                                          | Rarely changed                                   |
| `LOG_CONSOLE_PATTERN`           | Log pattern for console output                                            | No       | (see yaml)                           | `%green(%d{yyyy-MM-dd HH:mm:ss.SSS}) %highlight(%-5level) ...`  | Usually kept in yaml                             |
| `LOG_FILE_PATTERN`              | Log pattern for file output                                               | No       | (see yaml)                           | `%d{yyyy-MM-dd HH:mm:ss.SSS} %-5level [%thread] ...`            | Usually kept in yaml                             |
| `LOCALHOST_SWAGGER_SERVER`      | URL for Swagger local environment server                                  | No       | `http://localhost:8080`              | `http://localhost:8080`                                         | Set in OS env / yaml                             |
| `STAGE_SWAGGER_SERVER`          | URL for Swagger stage environment server                                  | No       | `https://stage.myapp.com`            | `https://stage.myapp.com`                                       | Set in OS env / yaml                             |
| `PROD_SWAGGER_SERVER`           | URL for Swagger production environment server                             | No       | `https://api.myapp.com`              | `https://api.myapp.com`                                         | Set in OS env / yaml                             |
| `API_DOCS_ENABLED`              | Enable or disable Springdoc API docs                                      | No       | `true`                               | `true` / `false`                                                | Usually set in yaml or CI/CD                     |
| `SWAGGER_UI_ENABLED`            | Enable or disable Swagger UI                                              | No       | `true`                               | `true` / `false`                                                | Usually set in yaml or CI/CD                     |

## Notes on selected variables

### JWT_VALIDITY
Usually in **milliseconds**. Common values:
- 15 minutes  → `900000`
- 1 hour      → `3600000`
- 24 hours    → `86400000`
- 7 days      → `604800000`
- 30 days     → `2592000000`

### JPA_HIBERNATE_DDL_AUTO
Recommended values per environment:
- local/dev     → `create-drop` or `update`
- test (test DB) → `create-drop`
- staging/prod  → **only** `validate` or `none`

### Logging-related variables
These environment variables control logging behavior. Pay attention to them especially for production environments, free hosting, or limited disk/CPU resources.

- **LOG_LEVEL_ROOT** – sets the default logging level for all loggers.
    - Use `INFO` or `WARN` in prod to avoid excessive logs.
    - `DEBUG` or `TRACE` is useful for local development.
    - If unset, `INFO` is used by default.

- **LOG_LEVEL_APP** – sets the logging level for your application packages (`com.itasocialacademy.oitassist`).
    - Keep `DEBUG` in dev, switch to `INFO` in prod.
    - Helps reduce noise from verbose frameworks like Spring.

- **LOG_LEVEL_SPRING** – controls Spring framework logs.
    - Usually `INFO` in prod to avoid clutter.

- **LOG_LEVEL_LOGBOOK** – controls HTTP request/response logging (Zalando Logbook).
    - `TRACE` generates very detailed logs (including headers/bodies).
    - Recommended: `TRACE` only in dev, `INFO` or `WARN` in prod.

- **LOG_FILE** – path to the active log file.
    - This should point to a file, not a folder (e.g. `logs/app.log`).
    - Logback will create the folder if it does not exist.
    - Avoid setting it to `./logs/application` — the log file must have an extension for proper rotation.

- **LOG_FILE_PATTERN** – format for rolled/archived files.
    - Example: `${LOG_FILE}.%d{yyyy-MM-dd}.log.gz` → generates files like `app.log.2026-02-12.log.gz`.
    - Ensures there is always a live file (`app.log`) for `tail -f` and archived gzipped logs for historical data.

- **LOG_MAX_HISTORY** – number of days or number of rolled files to keep.
    - Adjust depending on disk space and retention requirements.

- **LOG_TOTAL_SIZE_CAP** – maximum total size of all log files.
    - Once exceeded, oldest logs are deleted.
    - Important on free/limited hosting to avoid disk overrun.

- **LOG_ASYNC_QUEUE_SIZE** – queue size for async logging.
    - Larger queue prevents blocking during I/O spikes but uses more memory.
    - Recommended for web applications to avoid latency issues when disk writes are slow.

- **LOG_CONSOLE_PATTERN** – console output format.
    - Useful for dev and docker logs; usually colored and human-readable.

- **LOG_FILE_PATTERN** – file output format.
    - Include timestamp, level, thread, and traceId to trace production issues.

**Notes / Best practices:**

1. On production / free hosting, avoid `TRACE` levels for Logbook and app logs; use `INFO` / `WARN`.
2. Always ensure `LOG_FILE` points to a file (with `.log`) and not a folder.
3. Async logging is recommended for web apps to prevent blocking requests during heavy I/O.
4. Check that the working directory of your app matches the log path, or use an absolute path (especially in containers).
5. `LOG_MAX_HISTORY` and `LOG_TOTAL_SIZE_CAP` prevent unbounded disk usage — especially critical on limited/free hosting (Heroku, Render, Railway, Fly.io etc.).


## Quick local testing example

```bash
DATASOURCE_URL=jdbc:postgresql://localhost:5432/oitassist
DATASOURCE_USERNAME=postgres
DATASOURCE_PASSWORD=mysecretpassword
SPRING_LIQUIBASE_ENABLED=true
SPRING_LIQUIBASE_CHANGE_LOG=classpath:db/changelog/db.changelog-master.xml
JPA_SHOW_SQL=true
JPA_HIBERNATE_DDL_AUTO=validate
JWT_ENCRYPTED_KEY=32-byte-base64-encoded-secret-here-should-be-very-secure
JWT_SIGN_KEY=another-very-long-random-secret-at-least-32-chars
JWT_VALIDITY=3600000
LOG_LEVEL_ROOT=INFO
LOG_LEVEL_APP=DEBUG
LOG_LEVEL_SPRING=INFO
LOG_LEVEL_LOGBOOK=TRACE
LOG_FILE=logs/local-app.log