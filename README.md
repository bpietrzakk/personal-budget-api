# Personal Budget API

REST API for personal budget management. Built for SoftNet internship recruitment task via Pragmatic Coders.

---

## Tech Stack

- **Java 21** + **Spring Boot 3.5.14**
- **PostgreSQL 16** via Docker Compose
- **Spring Data JPA** + **Hibernate**
- **Lombok** — boilerplate reduction
- **Springdoc OpenAPI** — Swagger UI at `/swagger-ui.html`
- **JUnit 5** + **Mockito** + **Testcontainers**

---

## Quick Start

### Prerequisites

- **Git**
- **Docker** 20.10+ (for PostgreSQL and integration tests)
- **JDK 21**

### Run

```bash
# 1. Start PostgreSQL
docker-compose up -d

# 2. Start the application
./mvnw spring-boot:run
```

The application will be available at `http://localhost:8080`.

Swagger UI: `http://localhost:8080/swagger-ui.html`

### Stop

```bash
# Stop PostgreSQL and remove data
docker-compose down -v

# Stop PostgreSQL but keep data
docker-compose down
```

---

## API Endpoints

All examples use `localhost:8080`. Sample account ID: `a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11`.

`curl` works on Linux, macOS, and Windows CMD (Windows 10+). On Windows PowerShell use `curl.exe`.

---

### Accounts

| Method | Path | Description |
|--------|------|-------------|
| `GET` | `/accounts` | List all accounts |
| `POST` | `/accounts` | Create account |
| `GET` | `/accounts/{id}` | Get account with current balance |
| `DELETE` | `/accounts/{id}` | Delete account (only if no transactions) |
| `GET` | `/accounts/{id}/transactions/export` | Export transactions as CSV |

#### `POST /accounts` — Create account

```bash
curl -X POST http://localhost:8080/accounts \
  -H "Content-Type: application/json" \
  -d '{"name": "Main Account"}'
```

Response `201`:
```json
{
  "id": "a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11",
  "name": "Main Account",
  "balance": 0,
  "createdAt": "2024-06-03T12:00:00"
}
```

#### `GET /accounts` — List all accounts

```bash
curl http://localhost:8080/accounts
```

#### `GET /accounts/{id}` — Get account by ID

```bash
curl http://localhost:8080/accounts/a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11
```

#### `DELETE /accounts/{id}` — Delete account

Fails with `409` if any transactions exist for this account.

```bash
curl -X DELETE http://localhost:8080/accounts/a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11
```

#### `GET /accounts/{id}/transactions/export` — Export as CSV

```bash
curl http://localhost:8080/accounts/a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11/transactions/export \
  -o transactions.csv
```

CSV columns: `date,type,amount,category,description`

---

### Transactions

| Method | Path | Description |
|--------|------|-------------|
| `GET` | `/transactions` | List transactions (optional filters) |
| `POST` | `/transactions` | Create transaction, updates account balance |
| `DELETE` | `/transactions/{id}` | Delete transaction, reverts account balance |

#### `POST /transactions` — Create transaction

`transactionDate` is optional — defaults to today.

```bash
curl -X POST http://localhost:8080/transactions \
  -H "Content-Type: application/json" \
  -d '{
    "type": "INCOME",
    "amount": 5000.00,
    "category": "Salary",
    "description": "June salary",
    "accountId": "a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11"
  }'
```

Response `201`:
```json
{
  "id": "b1ffcd00-...",
  "type": "INCOME",
  "amount": 5000.00,
  "category": "Salary",
  "description": "June salary",
  "transactionDate": "2024-06-03",
  "createdAt": "2024-06-03T12:00:00",
  "accountId": "a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11",
  "warnings": null
}
```

If a monthly category limit is exceeded, `warnings` contains a message — the transaction is still saved.

#### `GET /transactions` — List with optional filters

All parameters are optional:

```bash
# All transactions
curl http://localhost:8080/transactions

# Filter by date range
curl "http://localhost:8080/transactions?from=2024-06-01&to=2024-06-30"

# Filter by category
curl "http://localhost:8080/transactions?category=Food"

# Combined filters
curl "http://localhost:8080/transactions?from=2024-06-01&to=2024-06-30&category=Food"
```

#### `DELETE /transactions/{id}` — Delete transaction

Balance is automatically reverted.

```bash
curl -X DELETE http://localhost:8080/transactions/b1ffcd00-9c0b-4ef8-bb6d-6bb9bd380a11
```

---

### Summary

| Method | Path | Description |
|--------|------|-------------|
| `GET` | `/summary` | Total income, expenses and breakdown by category |

#### `GET /summary` — Budget summary

```bash
# All time
curl http://localhost:8080/summary

# For a date range
curl "http://localhost:8080/summary?from=2024-06-01&to=2024-06-30"
```

Response:
```json
{
  "totalIncome": 5000.00,
  "totalExpenses": 1200.00,
  "expensesByCategory": {
    "Food": 400.00,
    "Transport": 200.00,
    "Entertainment": 600.00
  }
}
```

---

### Category Limits

| Method | Path | Description |
|--------|------|-------------|
| `GET` | `/limits` | List all category limits |
| `POST` | `/limits` | Set monthly limit for a category |
| `DELETE` | `/limits/{category}` | Remove limit for a category |

#### `POST /limits` — Set a monthly limit

```bash
curl -X POST http://localhost:8080/limits \
  -H "Content-Type: application/json" \
  -d '{"category": "Food", "limitAmount": 500.00}'
```

Response `201`:
```json
{
  "id": "c2ggde11-...",
  "category": "Food",
  "limitAmount": 500.00
}
```

Once a limit is set, exceeding it when adding an EXPENSE transaction returns a warning in the response — the transaction is still recorded.

#### `DELETE /limits/{category}` — Remove limit

```bash
curl -X DELETE http://localhost:8080/limits/Food
```

---

### Error Responses

All errors follow the same format:

```json
{
  "timestamp": "2024-06-03T12:00:00",
  "status": 404,
  "error": "Not Found",
  "message": "Account not found: a0eebc99-...",
  "path": "/accounts/a0eebc99-..."
}
```

#### Account not found — `404`

```bash
curl http://localhost:8080/accounts/00000000-0000-0000-0000-000000000000
```

```json
{"timestamp": "...","status": 404,"error": "Not Found","message": "Account not found: 00000000-...","path": "/accounts/00000000-..."}
```

#### Delete account with transactions — `409`

```bash
curl -X DELETE http://localhost:8080/accounts/a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11
```

```json
{"status": 409,"error": "Conflict","message": "Cannot delete account with existing transactions",...}
```

#### Validation error — `400`

```bash
curl -X POST http://localhost:8080/accounts \
  -H "Content-Type: application/json" \
  -d '{"name": ""}'
```

```json
{"status": 400,"error": "Bad Request","message": "name: Name is required",...}
```

---

## Design Decisions

- **UUID as primary key** — IDs are exposed in URLs. Sequential `Long` IDs allow enumeration (`/accounts/1`, `/accounts/2`). UUID prevents this and is ready for distributed systems.

- **`BigDecimal` for amounts, not `double`** — financial application. `0.1 + 0.2 ≠ 0.3` in IEEE 754 binary floating point. `BigDecimal` stores exact decimal values. `NUMERIC(19, 2)` in PostgreSQL preserves this precision.

- **`transaction_date` ≠ `created_at`** — `transaction_date` is when the economic event happened (user-settable, used for filtering). `created_at` is when the record was created (audit, auto-set). Users need to record past expenses.

- **Transactions are immutable after creation** — no `PUT` endpoint. `@Column(updatable = false)` on `type`, `amount`, `category`, `transactionDate`, `accountId`. Simpler balance reversal on delete, cleaner audit trail.

- **FK with `RESTRICT`, not `CASCADE`** — `DELETE /accounts/{id}` returns `409` if transactions exist rather than silently deleting them. This forces a conscious decision and prevents accidental data loss.

- **Aggregates in the database** — `GET /summary` uses `SUM` + `GROUP BY` in JPQL, not in-memory Java streams. The database is far more efficient for aggregating large datasets.

- **Category limits are stored in the database (CRUD)** — a `CategoryLimit` entity instead of `application.properties`. Limits can be changed via API without restarting the application.

---

## Project Structure

```
src/main/java/com/bpietrzak/budget/
├── controller/       # REST controllers
├── service/          # Business logic
├── repository/       # Spring Data JPA repositories
├── model/            # JPA entities
│   └── enums/        # TransactionType (INCOME, EXPENSE)
├── dto/              # Request/response objects
└── exception/        # Custom exceptions + GlobalExceptionHandler
```

---

## Running Tests

```bash
./mvnw test
```

> **Note:** integration tests use Testcontainers, which requires Docker to be running.

Tests include:

- **Unit tests** (`AccountServiceTest`, `TransactionServiceTest`) — service layer with Mockito, no Spring context, no database. Cover: balance updates on create/delete, conflict and not-found exceptions, default date logic.
- **Integration tests** (`BudgetIntegrationTest`) — full HTTP tests with MockMvc + Testcontainers + real PostgreSQL. Cover: happy path (create account → add transactions → check balance → check summary), balance reversal on transaction delete, `409` on account delete with transactions, `404` and `400` error responses.

---

## What I'd Add With More Time

- **Flyway** — currently using `spring.jpa.hibernate.ddl-auto=update` for simplicity. Production needs versioned schema migrations for safe rollouts.
- **UUIDv7** — current implementation generates random UUIDv4, which causes index fragmentation in PostgreSQL B-tree indexes. UUIDv7 is time-ordered and insert-friendly.
- **Database indexes** — `(account_id, transaction_date)` composite index on `transactions` for fast filtered queries; `category` index for summary aggregation.
- **API versioning** — prefix `/api/v1/` to allow non-breaking future changes.
- **Pagination** — `GET /transactions` returns all results. `Pageable` support needed for large datasets.
