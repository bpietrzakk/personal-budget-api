# Personal Budget API — dokumentacja (PL)

REST API do zarządzania budżetem osobistym. Zadanie rekrutacyjne na staż Software Engineer w SoftNet via Pragmatic Coders.

---

## Stack technologiczny

- **Java 21** + **Spring Boot 3.5.14**
- **PostgreSQL 16** uruchamiane przez Docker Compose
- **Spring Data JPA** + **Hibernate**
- **Lombok** — eliminacja boilerplate'u
- **Springdoc OpenAPI** — Swagger UI pod `/swagger-ui.html`
- **JUnit 5** + **Mockito** + **Testcontainers**

---

## Uruchomienie

### Wymagania

- **Git**
- **Docker** 20.10+ (dla PostgreSQL i testów integracyjnych)
- **JDK 21**

### Jak uruchomić

```bash
# 1. Uruchom PostgreSQL
docker-compose up -d

# 2. Uruchom aplikację
./mvnw spring-boot:run
```

Aplikacja dostępna pod: `http://localhost:8080`

Swagger UI: `http://localhost:8080/swagger-ui.html`

### Zatrzymanie

```bash
# Zatrzymaj i usuń dane
docker-compose down -v

# Zatrzymaj, zachowaj dane
docker-compose down
```

---

## Endpointy API

Przykłady używają `localhost:8080`. Przykładowe ID konta: `a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11`.

---

### Konta

| Metoda | Ścieżka | Opis |
|--------|---------|------|
| `GET` | `/accounts` | Lista wszystkich kont |
| `POST` | `/accounts` | Utwórz konto |
| `GET` | `/accounts/{id}` | Szczegóły konta z aktualnym saldem |
| `DELETE` | `/accounts/{id}` | Usuń konto (tylko jeśli brak transakcji) |
| `GET` | `/accounts/{id}/transactions/export` | Eksport transakcji do CSV |

#### `POST /accounts` — Utwórz konto

```bash
curl -X POST http://localhost:8080/accounts \
  -H "Content-Type: application/json" \
  -d '{"name": "Konto główne"}'
```

Odpowiedź `201`:
```json
{
  "id": "a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11",
  "name": "Konto główne",
  "balance": 0,
  "createdAt": "2024-06-03T12:00:00"
}
```

#### `DELETE /accounts/{id}` — Usuń konto

Zwraca `409` jeśli konto ma przypisane transakcje.

---

### Transakcje

| Metoda | Ścieżka | Opis |
|--------|---------|------|
| `GET` | `/transactions` | Lista transakcji (opcjonalne filtry) |
| `POST` | `/transactions` | Dodaj transakcję, aktualizuje saldo konta |
| `DELETE` | `/transactions/{id}` | Usuń transakcję, cofa saldo konta |

#### `POST /transactions` — Dodaj transakcję

`transactionDate` jest opcjonalne — domyślnie dzisiaj.

```bash
curl -X POST http://localhost:8080/transactions \
  -H "Content-Type: application/json" \
  -d '{
    "type": "INCOME",
    "amount": 5000.00,
    "category": "Wynagrodzenie",
    "description": "Czerwiec",
    "accountId": "a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11"
  }'
```

Odpowiedź `201`:
```json
{
  "id": "b1ffcd00-...",
  "type": "INCOME",
  "amount": 5000.00,
  "category": "Wynagrodzenie",
  "transactionDate": "2024-06-03",
  "accountId": "a0eebc99-...",
  "warnings": null
}
```

Jeśli przekroczony jest miesięczny limit kategorii, pole `warnings` zawiera komunikat — transakcja i tak jest zapisywana.

#### `GET /transactions` — Lista z filtrami

```bash
# Wszystkie
curl http://localhost:8080/transactions

# Zakres dat
curl "http://localhost:8080/transactions?from=2024-06-01&to=2024-06-30"

# Po kategorii
curl "http://localhost:8080/transactions?category=Jedzenie"
```

---

### Podsumowanie

| Metoda | Ścieżka | Opis |
|--------|---------|------|
| `GET` | `/summary` | Łączne przychody, wydatki i podział po kategorii |

#### `GET /summary`

```bash
# Całkowite
curl http://localhost:8080/summary

# Za zakres dat
curl "http://localhost:8080/summary?from=2024-06-01&to=2024-06-30"
```

Odpowiedź:
```json
{
  "totalIncome": 5000.00,
  "totalExpenses": 1200.00,
  "expensesByCategory": {
    "Jedzenie": 400.00,
    "Transport": 200.00,
    "Rozrywka": 600.00
  }
}
```

---

### Limity kategorii

| Metoda | Ścieżka | Opis |
|--------|---------|------|
| `GET` | `/limits` | Lista wszystkich limitów |
| `POST` | `/limits` | Ustaw miesięczny limit dla kategorii |
| `DELETE` | `/limits/{category}` | Usuń limit dla kategorii |

#### `POST /limits` — Ustaw limit

```bash
curl -X POST http://localhost:8080/limits \
  -H "Content-Type: application/json" \
  -d '{"category": "Jedzenie", "limitAmount": 500.00}'
```

Po przekroczeniu limitu przy dodawaniu wydatku `POST /transactions` zwróci ostrzeżenie w polu `warnings`. Transakcja jest nadal zapisywana.

---

### Format błędów

Każdy błąd ma ten sam format:

```json
{
  "timestamp": "2024-06-03T12:00:00",
  "status": 404,
  "error": "Not Found",
  "message": "Account not found: a0eebc99-...",
  "path": "/accounts/a0eebc99-..."
}
```

| Kod | Kiedy |
|-----|-------|
| `400` | Błąd walidacji (puste wymagane pole, ujemna kwota) |
| `404` | Konto lub transakcja nie istnieje |
| `409` | Próba usunięcia konta z transakcjami |

---

## Decyzje projektowe

- **UUID jako klucz główny** — ID są wystawiane w URL-ach. Sekwencyjny `Long` pozwala na enumerację zasobów (`/accounts/1`, `/2`...). UUID to uniemożliwia i jest gotowy na systemy rozproszone.

- **`BigDecimal` zamiast `double`** — aplikacja finansowa. `0.1 + 0.2 ≠ 0.3` w binarnym IEEE 754. `BigDecimal` przechowuje dokładne wartości dziesiętne. `NUMERIC(19, 2)` w PostgreSQL zachowuje tę precyzję.

- **`transaction_date` ≠ `created_at`** — `transaction_date` to kiedy zdarzenie ekonomiczne miało miejsce (ustawiane przez użytkownika, używane do filtrowania). `created_at` to kiedy rekord powstał (audit, automatyczne). Użytkownik musi móc wpisywać wydatki wstecz.

- **Transakcja niemutowalna po utworzeniu** — brak endpointu `PUT`. `@Column(updatable = false)` na polach `type`, `amount`, `category`, `transactionDate`, `accountId`. Prostsze cofanie salda przy usuwaniu, czystszy audit trail.

- **FK z `RESTRICT`, nie `CASCADE`** — `DELETE /accounts/{id}` zwraca `409` gdy konto ma transakcje, zamiast je cicho usunąć. Wymusza świadomą decyzję, zapobiega przypadkowej utracie danych.

- **Agregaty w bazie danych** — `GET /summary` używa `SUM` + `GROUP BY` w JPQL, nie strumieni Java w pamięci. Baza jest znacznie wydajniejsza przy agregacji dużych zbiorów.

- **Limity kategorii w bazie (CRUD)** — encja `CategoryLimit` zamiast `application.properties`. Limity można zmieniać przez API bez restartu aplikacji.

---

## Struktura projektu

```
src/main/java/com/bpietrzak/budget/
├── controller/       # Kontrolery REST
├── service/          # Logika biznesowa
├── repository/       # Repozytoria Spring Data JPA
├── model/            # Encje JPA
│   └── enums/        # TransactionType (INCOME, EXPENSE)
├── dto/              # Obiekty request/response
└── exception/        # Wyjątki + GlobalExceptionHandler
```

---

## Uruchamianie testów

```bash
./mvnw test
```

> **Uwaga:** testy integracyjne używają Testcontainers — wymaga działającego Dockera.

- **Testy jednostkowe** (`AccountServiceTest`, `TransactionServiceTest`) — warstwa serwisów z Mockito, bez Springa i bazy. Pokrywają: aktualizację salda, wyjątki ConflictException i ResourceNotFoundException, domyślną datę.
- **Testy integracyjne** (`BudgetIntegrationTest`) — pełne testy HTTP z MockMvc + Testcontainers + prawdziwy PostgreSQL. Pokrywają: happy path (saldo + summary), cofanie salda przy usuwaniu transakcji, `409` przy usuwaniu konta z transakcjami, format błędów.

---

## Co bym dodał z większą ilością czasu

- **Flyway** — aktualnie `ddl-auto=update` dla uproszczenia. Produkcja wymaga wersjonowanych migracji schematu.
- **UUIDv7** — aktualnie generowany losowy UUIDv4, który fragmentuje indeks B-tree w PostgreSQL. UUIDv7 jest porządkowany czasowo i przyjazny dla insertów.
- **Indeksy** — złożony indeks `(account_id, transaction_date)` na `transactions` dla szybkich zapytań z filtrami; indeks na `category` dla agregacji w summary.
- **Wersjonowanie API** — prefix `/api/v1/` dla nieblokujących zmian w przyszłości.
- **Paginacja** — `GET /transactions` zwraca wszystkie wyniki. Wsparcie dla `Pageable` przy dużych zbiorach danych.
