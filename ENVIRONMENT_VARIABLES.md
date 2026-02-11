# ENVIRONMENT VARIABLES
Documentation of all environment variables used in the project

This file describes every environment variable referenced in `application.yml` / `application.yaml`.

**Important**
- Never commit real secrets (passwords, keys, JWT secrets) to the repository
- Use a secrets manager (Vault, AWS Secrets Manager, Doppler, 1Password, Kubernetes Secrets, etc.)

## List of environment variables

| Variable                      | Description                                                                    | Required | Default value (if not set)  | Example real value                                              | Where to store / obtain real value              |
|-------------------------------|--------------------------------------------------------------------------------|----------|-----------------------------|-----------------------------------------------------------------|-------------------------------------------------|
| `DATASOURCE_URL`              | Full JDBC URL to connect to the database                                       | Yes      | — (fails if missing)        | `jdbc:postgresql://db.example.com:5432/prod_db`                 | Secrets manager / Kubernetes Secret             |
| `DATASOURCE_USERNAME`         | Database username                                                              | Yes      | —                           | `app_user_prod`                                                 | Secrets manager                                 |
| `DATASOURCE_PASSWORD`         | Database password                                                              | Yes      | —                           | `s3cr3t-v3ry-l0ng-p@ssw0rd`                                     | Secrets manager (never in git!)                 |
| `SPRING_LIQUIBASE_ENABLED`    | Whether to enable Liquibase on application startup                             | No       | `false`                     | `true`                                                          | Usually `true` in all environments except local |
| `SPRING_LIQUIBASE_CHANGE_LOG` | Path to the main Liquibase changelog file                                      | No       | —                           | `classpath:db/changelog/db.changelog-master.yaml`               | Usually kept in yaml                            |
| `SPRING_LIQUIBASE_DROP_FIRST` | Whether to drop all database objects before applying migrations                | No       | `false`                     | `true` (only for tests/dev – dangerous!)                        | Only for special / destructive scenarios        |
| `JPA_SHOW_SQL`                | Whether to log all SQL statements executed by Hibernate                        | No       | — (usually false)           | `true`                                                          | `true` in dev, `false` in prod                  |
| `JPA_HIBERNATE_DDL_AUTO`      | Hibernate schema generation strategy (hibernate.hbm2ddl.auto)                  | No       | `validate`                  | `validate` / `update` / `create-drop` / `none`                  | `validate` is safest for production             |
| `JWT_ENCRYPTED_KEY`           | Key used to encrypt sensitive data inside JWT (if implemented)                 | Yes      | —                           | 32-byte key in base64 or hex                                    | Secrets manager / Vault                         |
| `JWT_SIGN_KEY`                | Signing key for JWT (HS256 / RS256 / etc.)                                     | Yes      | —                           | Very long random string or RSA private key                      | Secrets manager                                 |
| `JWT_VALIDITY`                | Token validity duration in milliseconds (or seconds/minutes – depends on code) | Yes      | —                           | `3600000` (1 hour) / `86400000` (24 hours)                      | Usually set in yaml, rarely overridden          |

### Notes on selected variables

- **JWT_VALIDITY**  
  Usually specified in milliseconds. Common values:
    - 15 minutes  → `900000`
    - 1 hour      → `3600000`
    - 24 hours    → `86400000`
    - 7 days      → `604800000`

- **JPA_HIBERNATE_DDL_AUTO**  
  Recommended values per environment:
    - local/dev     → `create-drop` or `update`
    - test (test DB) → `create-drop`
    - staging/prod  → **only** `validate` or `none`

## Quick local testing example

```bash
DATASOURCE_URL=jdbc:postgresql://localhost:5432/mydb
DATASOURCE_USERNAME=postgres
DATASOURCE_PASSWORD=mysecretpassword
SPRING_LIQUIBASE_ENABLED=true
SPRING_LIQUIBASE_CHANGE_LOG=classpath:db/changelog/db.changelog-master.xml
JPA_SHOW_SQL=true
JWT_ENCRYPTED_KEY=super-secret-key-should-be-at-least-32-characters-long
JWT_SIGN_KEY=super-secret-key-should-be-at-least-32-characters-long
JWT_VALIDITY=3600000
