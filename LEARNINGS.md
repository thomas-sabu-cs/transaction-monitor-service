# Technical Learnings — Transaction Monitor Service

This document explains the **technical decisions** and **production-style practices** used in this Spring Boot 3 service. The project prioritizes **Clean Code** and **Type Safety** over "just making it work."

---

## 1. Layered Architecture: Controller → Service → Repository

### What the code does

- **Controller** (`TransactionController`): Receives HTTP requests, calls the service, returns `ResponseEntity` with status and body. No business logic.
- **Service** (`TransactionService`): Holds business rules (e.g. high-risk threshold `70`), coordinates repository calls, throws `ResourceNotFoundException`, and uses `@Transactional` for consistency.
- **Repository** (`TransactionRepository`): Extends `JpaRepository`; only talks to the database (e.g. `findByRiskScoreGreaterThan`).

### Why this is better than putting logic in the controller

| Concern | Controller-only approach | Layered approach (this project) |
|--------|----------------------------|----------------------------------|
| **Testability** | Hard to test HTTP + DB + logic together | Service logic tested in isolation with mocked repository (no HTTP, no DB). |
| **Reuse** | Logic tied to one endpoint | Same service methods can be used by other controllers, jobs, or APIs. |
| **Transaction boundaries** | Unclear where transactions start/end | `@Transactional` on service methods defines clear boundaries. |
| **Single responsibility** | Controller does routing + business + persistence | Controller = HTTP; Service = business; Repository = data access. |

The **technical delta**: the controller stays a thin adapter. All "what to do" lives in the service; the service is written in plain Java with clear types (`Transaction`, `TransactionRequest`, `Long`), which keeps the code easier to reason about and refactor (Clean Code, type-safe design).

---

## 2. Data Integrity: Bean Validation

### How it’s used in this project

- **Request DTO** (`TransactionRequest`): Every field that comes from the client is constrained with Bean Validation:
  - `@NotNull` on `userId`, `amount`, `timestamp`, `riskScore` — rejects missing values.
  - `@Size(min = 1, max = 255)` on `userId` — rejects empty or oversized strings.
  - `@Min(0)` on `amount` — rejects negative amounts.
  - `@Min(0)` and `@Max(100)` on `riskScore` — enforces 0–100 range.

- **Controller**: `@Valid` on `TransactionRequest` in POST and PUT:
  - If validation fails, Spring never calls the service; `MethodArgumentNotValidException` is thrown and handled by `GlobalExceptionHandler`, returning 400 with field-level errors.

### How this protects the system

- **Bad data is stopped at the boundary**: Invalid payloads never reach the service or database, so the domain and persistence layers can assume inputs are already valid (fail-fast, type-safe assumptions).
- **Explicit contracts**: The DTO + annotations document exactly what the API accepts; the frontend (and API consumers) get a stable, predictable contract.
- **Consistent error shape**: The handler turns validation failures into a structured response (status, message, timestamp, field errors), so clients can display errors in a consistent way.

This is **type safety and data integrity at the API boundary**, not only inside the JVM.

---

## 3. The Testing Delta: Unit Tests vs Integration Tests

### Unit tests (`TransactionServiceTest`)

- **Setup**: `@ExtendWith(MockitoExtension.class)`, `@Mock TransactionRepository`, `@InjectMocks TransactionService`. No Spring context, no HTTP, no real DB.
- **What they prove**: Service behavior in isolation — e.g. `findById` throws `ResourceNotFoundException` when the repo returns empty; `findHighRisk()` calls `findByRiskScoreGreaterThan(70)` and returns the list; create/update/delete interact with the repository as expected.
- **Value**: Fast, deterministic, and focused on business logic and boundaries (e.g. "when repo says not found, service throws"). They don’t depend on Spring MVC or JSON.

### Integration tests (`TransactionControllerTest`)

- **Setup**: `@WebMvcTest(TransactionController.class)`, `@Import(GlobalExceptionHandler.class)`, `MockMvc`, `@MockBean TransactionService`. Only the web layer and the controller are loaded; service is mocked.
- **What they prove**: HTTP contract — correct method/path, status codes (200, 201, 204, 400, 404), JSON shape (`jsonPath`), and that validation and `GlobalExceptionHandler` produce the expected 400/404 responses when the service throws or when `@Valid` fails.
- **Value**: Confidence that the API a client calls (URLs, status codes, body structure, error format) matches what the code actually does, without running a full app or database.

**Summary**: Unit tests prove **service logic and repository interaction**; integration tests prove **REST contract and error handling**. Together they give both correct behavior and a stable, type-aware API surface.

---

## 4. Error Handling: The Frontend Contract

### How `GlobalExceptionHandler` creates a contract

The handler uses **record types** and **fixed response shapes** so every error type has a predictable JSON structure:

- **404 Not Found** (`ResourceNotFoundException`): Returns a body like `{ "status": 404, "message": "Transaction not found with id: 999", "timestamp": "..." }` via `ErrorResponse`.
- **400 Bad Request** (validation): Returns `ValidationErrorResponse` with `status`, `message`, `timestamp`, and `errors` (map of field name → validation message), e.g. `"amount": "amount must be non-negative"`.
- **500** (generic `Exception`): Returns `ErrorResponse` with status 500 and a message.

So the frontend can:

- Rely on **HTTP status** (404 vs 400 vs 500) for flow control.
- Parse a **consistent JSON shape**: always `status`, `message`, `timestamp`, and for 400 also `errors` per field.
- Show user-friendly messages from `message` or `errors` without guessing.

That’s the **contract**: same status codes and same body structure for the same exception type, which supports Clean Code and type-safe error handling on the client.

---

## 5. CI/CD: Value of the GitHub Actions Workflow

### What the workflow does (`.github/workflows/ci.yml`)

- Triggers on **push** and **pull_request** to `main` and `master`.
- Checks out the repo, sets up **JDK 17** (Temurin), uses Maven cache, and runs **`mvn test -B`**.

### Why it matters

- **Regression safety**: Every push and every PR runs the full test suite (unit + integration). Breaking changes are caught before merge.
- **Environment parity**: Tests run in a clean Ubuntu runner with a fixed Java version, so "works on my machine" is validated in a consistent environment.
- **No manual step**: Developers don’t have to remember to run tests; the pipeline enforces it.
- **Foundation for more**: The same workflow can later add coverage reporting, build artifacts, or deployment steps without changing how tests are run.

So the value is: **automated, repeatable verification** of build and tests on every change, which protects the Clean Code and type-safe design from regressions.

---

## Summary

This service is built with:

- **Layered architecture** (Controller → Service → Repository) for testability, reuse, and clear transaction and responsibility boundaries.
- **Bean Validation** (`@Valid`, `@NotNull`, `@Min`/`@Max`, `@Size`) to enforce data integrity at the API boundary and keep the rest of the system type-safe.
- **Two testing levels**: unit tests (Mockito) for service logic, integration tests (MockMvc) for the REST contract and error responses.
- **GlobalExceptionHandler** to give the frontend a stable contract for 404, 400, and 500 with consistent JSON shapes.
- **GitHub Actions CI** to run `mvn test` on every push and PR for regression protection and consistent feedback.

Together, these choices emphasize **Clean Code** and **Type Safety** over simply "making it work."
