# Transaction Monitor Service

A production-style **REST API** for monitoring financial transactions with risk scoring. Built with **Java 17**, **Spring Boot 3**, and **Maven**. Uses **Spring Data JPA** with an **H2 in-memory** relational database so the application runs with no external database setup. Includes **JUnit 5** and **Mockito** for unit and integration tests, and **GitHub Actions** for CI/CD.

## Tech Stack

- **Java 17**
- **Spring Boot 3** (Web, Data JPA, Validation)
- **Maven** (build and dependency management)
- **Spring Data JPA** + **H2** (in-memory SQL database)
- **JUnit 5** + **Mockito** (unit and integration tests)
- **GitHub Actions** (CI: run `mvn test` on every push and pull request)

## Design

The project follows **object-oriented** and **layered** design:

- **Controller** — REST endpoints, input validation via `@Valid` and Bean Validation
- **Service** — business logic and transaction orchestration (with Javadoc on public methods)
- **Repository** — data access using Spring Data JPA
- **Model** — entity (`Transaction`) and request DTO (`TransactionRequest`)
- **Exception** — global error handling with `@ControllerAdvice` and custom `ResourceNotFoundException`

Persistence uses **SQL** through H2; the schema is created automatically from the JPA entity.

## Prerequisites

- **JDK 17**
- **Maven 3.6+** (or use the Maven Wrapper: `./mvnw` on Unix/macOS or `mvnw.cmd` on Windows)

## Build and Run

```bash
# Run tests
mvn test

# Package (skip tests: mvn package -DskipTests)
mvn package

# Run the application (uses H2 in-memory DB)
mvn spring-boot:run
```

The API is available at `http://localhost:8080`.

## API Endpoints

| Method | Path | Description |
|--------|------|-------------|
| GET | `/transactions` | List all transactions |
| GET | `/transactions/{id}` | Get one transaction by id |
| POST | `/transactions` | Create a transaction (body: JSON) |
| PUT | `/transactions/{id}` | Update a transaction by id |
| DELETE | `/transactions/{id}` | Delete a transaction by id |
| GET | `/transactions/high-risk` | List transactions with `riskScore > 70` |

### Request body (POST / PUT)

```json
{
  "userId": "user123",
  "amount": 99.99,
  "timestamp": "2025-02-22T10:00:00",
  "riskScore": 45
}
```

- **userId**: required, non-blank string  
- **amount**: required, non-negative number  
- **timestamp**: required, ISO-8601 date-time  
- **riskScore**: required, integer between 0 and 100  

Validation errors and not-found cases return structured JSON (e.g. 400 with field errors, 404 with message) via the global exception handler.

## Project Structure

```
src/main/java/com/thomassabu/transactionmonitor/
├── TransactionMonitorApplication.java
├── controller/   — REST API
├── service/      — business logic
├── repository/   — JPA data access
├── model/        — Transaction, TransactionRequest
└── exception/    — ResourceNotFoundException, GlobalExceptionHandler

src/test/java/.../transactionmonitor/
├── service/TransactionServiceTest.java   — unit tests (Mockito)
└── controller/TransactionControllerTest.java — integration tests (MockMvc)
```

## Testing

- **TransactionServiceTest**: unit tests for the service layer with mocked `TransactionRepository` (JUnit 5 + Mockito).
- **TransactionControllerTest**: integration tests for the REST layer with `MockMvc` and mocked `TransactionService`; includes validation and not-found behaviour.

Run tests and (if configured) coverage:

```bash
mvn test
# Coverage report: target/site/jacoco/index.html (when JaCoCo is run)
```

The goal is **80%+** code coverage; add or extend tests as needed to maintain it.

## CI/CD (GitHub Actions)

The workflow in `.github/workflows/ci.yml` runs on every **push** and **pull request** to `main` or `master`:

1. Checkout repository  
2. Set up JDK 17 (Eclipse Temurin)  
3. Cache Maven dependencies  
4. Run `mvn test -B`  

No secrets or deployment steps are required for this CI pipeline.

## License

This project is for educational and portfolio use.
