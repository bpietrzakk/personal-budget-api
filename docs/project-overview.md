# Personal Budget API — jak działa projekt

> Dokument do nauki przed rozmową techniczną. Opisuje **co, jak i dlaczego** — z perspektywy kandydata, który ma to obronić na głos.

---

## 1. Co to za projekt?

REST API do zarządzania budżetem osobistym. Użytkownik może:
- zakładać **konta** (np. "Konto główne", "Oszczędności")
- dodawać **transakcje** (przychody i wydatki) przypisane do konta
- sprawdzać **podsumowanie** — łączne przychody, wydatki, wydatki per kategoria
- eksportować transakcje do **CSV**
- ustawiać **limity** na kategorie wydatków (np. max 500 zł miesięcznie na "Jedzenie")

Projekt jest zadaniem rekrutacyjnym — priorytet to czysty kod, przemyślane decyzje, czytelna architektura.

---

## 2. Stack technologiczny

| Technologia | Do czego służy |
|---|---|
| **Java 17** | język programowania |
| **Spring Boot 3.x** | framework — startuje apkę, obsługuje HTTP, zarządza zależnościami |
| **Spring Data JPA + Hibernate** | warstwa dostępu do bazy danych |
| **PostgreSQL** | relacyjna baza danych |
| **Docker Compose** | uruchamia PostgreSQL w kontenerze lokalnie |
| **Lombok** | eliminuje boilerplate (gettery, settery, konstruktory) |
| **Bean Validation** | walidacja requestów HTTP |
| **Springdoc OpenAPI** | automatyczna dokumentacja Swagger UI pod `/swagger-ui.html` |
| **JUnit 5 + Mockito** | testy jednostkowe i integracyjne |
| **Maven** | build tool — zarządza zależnościami, buduje JAR |

---

## 3. Czym jest Spring Boot? (w skrócie dla rozmowy)

Spring Boot to framework, który:
1. **Automatycznie konfiguruje** aplikację na podstawie tego, co jest na classpathie (np. widzi PostgreSQL driver → konfiguruje datasource).
2. **Zarządza zależnościami** między klasami przez mechanizm **Dependency Injection (DI)** — klasy nie tworzą swoich zależności samodzielnie przez `new`, Spring je "wstrzykuje".
3. **Uruchamia wbudowany serwer HTTP** (Tomcat) — nie potrzebujesz osobnego serwera aplikacji.

### Kluczowe adnotacje Spring w tym projekcie

| Adnotacja | Co robi |
|---|---|
| `@RestController` | klasa obsługuje requestów HTTP i zwraca JSON |
| `@Service` | klasa zawiera logikę biznesową; Spring ją zarządza |
| `@Repository` | interfejs dostępu do bazy; Spring generuje implementację |
| `@Entity` | klasa mapuje się na tabelę w bazie danych |
| `@RequiredArgsConstructor` (Lombok) | generuje konstruktor z `final` polami — Spring wstrzykuje przez konstruktor |
| `@Transactional` | metoda działa w transakcji bazodanowej (albo wszystko, albo nic) |
| `@Valid` | włącza walidację Bean Validation na obiekcie requestu |
| `@RestControllerAdvice` | klasa przechwytuje wyjątki ze wszystkich kontrolerów |

---

## 4. Architektura — warstwy aplikacji

```
HTTP Request
     │
     ▼
┌─────────────────────┐
│    Controller        │  ← przyjmuje HTTP, waliduje request, zwraca response
│  (AccountController) │
└─────────┬───────────┘
          │ wywołuje
          ▼
┌─────────────────────┐
│      Service         │  ← logika biznesowa (aktualizacja salda, sprawdzenia)
│  (AccountService)    │
└─────────┬───────────┘
          │ wywołuje
          ▼
┌─────────────────────┐
│     Repository       │  ← dostęp do bazy danych (JPA queries)
│ (AccountRepository)  │
└─────────┬───────────┘
          │ SQL przez Hibernate
          ▼
┌─────────────────────┐
│    PostgreSQL DB     │  ← faktyczne dane
└─────────────────────┘
```

**Każda warstwa ma jedną odpowiedzialność.** Controller nie dotyka bazy. Repository nie zna logiki biznesowej.

### Przepływ danych — przykład: POST /transactions

1. Klient wysyła JSON: `{ "type": "EXPENSE", "amount": 50.00, "category": "Food", "accountId": "..." }`
2. **TransactionController** odbiera request, deserializuje JSON do `TransactionCreateRequest`, uruchamia `@Valid` (walidacja pól)
3. Jeśli walidacja OK → wywołuje `transactionService.create(request)`
4. **TransactionService.create** (oznaczone `@Transactional`):
   - pobiera konto z bazy (`accountRepository.findById(...)`)
   - oblicza nowe saldo: `balance - 50.00` (bo EXPENSE)
   - ustawia `account.setBalance(newBalance)`
   - buduje obiekt `Transaction` i go zapisuje
   - sprawdza limity kategorii → ewentualnie buduje `warnings`
   - zwraca `TransactionResponse` z ostrzeżeniami (lub bez)
5. Controller zwraca `201 Created` z JSON body

---

## 5. Model danych — encje i schemat bazy

### Account (tabela `accounts`)

```java
@Entity
@Table(name = "accounts")
public class Account {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal balance;    // saldo aktualizowane automatycznie

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
```

### Transaction (tabela `transactions`)

```java
@Entity
@Table(name = "transactions")
public class Transaction {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, updatable = false)
    private TransactionType type;  // INCOME lub EXPENSE

    @Column(nullable = false, precision = 19, scale = 2, updatable = false)
    private BigDecimal amount;

    @Column(nullable = false, length = 100, updatable = false)
    private String category;

    private String description;    // nullable

    @Column(nullable = false, updatable = false)
    private LocalDate transactionDate;  // kiedy zdarzenie (może być wstecz)

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;    // kiedy rekord powstał w systemie

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", nullable = false, updatable = false)
    private Account account;  // FK do Account
}
```

### CategoryLimit (tabela `category_limits`)

```java
@Entity
public class CategoryLimit {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true, length = 100)
    private String category;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal limitAmount;
}
```

---

## 6. Endpointy — pełna tabela

### Konta

| Metoda | URL | Co robi | Kody odpowiedzi |
|---|---|---|---|
| GET | `/accounts` | lista wszystkich kont | 200 |
| POST | `/accounts` | utwórz konto (body: `{"name": "..."}`) | 201, 400 |
| GET | `/accounts/{id}` | szczegóły konta z aktualnym saldem | 200, 404 |
| DELETE | `/accounts/{id}` | usuń konto (tylko jeśli brak transakcji) | 204, 404, 409 |
| GET | `/accounts/{id}/transactions/export` | pobierz CSV z transakcjami konta | 200, 404 |

### Transakcje

| Metoda | URL | Co robi | Kody odpowiedzi |
|---|---|---|---|
| GET | `/transactions?from=&to=&category=` | lista transakcji (filtry opcjonalne) | 200 |
| POST | `/transactions` | dodaj transakcję, saldo aktualizuje się | 201, 400, 404 |
| DELETE | `/transactions/{id}` | usuń transakcję, saldo cofa się | 204, 404 |

### Podsumowanie i limity

| Metoda | URL | Co robi | Kody odpowiedzi |
|---|---|---|---|
| GET | `/summary?from=&to=` | łączne przychody, wydatki, wydatki per kategoria | 200 |
| GET | `/category-limits` | lista limitów | 200 |
| POST | `/category-limits` | ustaw limit dla kategorii | 201, 400 |
| DELETE | `/category-limits/{id}` | usuń limit | 204, 404 |

---

## 7. Kluczowe decyzje projektowe — do obrony na rozmowie

### 7.1 UUID zamiast Long (sekwencyjne ID)

**Co:** ID obu encji to `UUID`, generowany automatycznie przez bazę.

**Dlaczego UUID:**
- Long sekwencyjny (`/accounts/1`, `/accounts/2`) pozwala na **enumerację** — każdy może przejrzeć wszystkie zasoby po kolei. UUID to uniemożliwia.
- UUID jest **globalnie unikalny** — gotowy na systemy rozproszone, client-generated IDs.
- W Spring Boot 3.x to jedna adnotacja: `@GeneratedValue(strategy = GenerationType.UUID)`.

**Wada:** UUIDv4 (losowy) powoduje fragmentację indeksu B-tree w bazie, bo nowe rekordy nie wpadają w porządku. Rozwiązanie: **UUIDv7** (time-ordered) — to "co bym dodał z większą ilością czasu".

---

### 7.2 BigDecimal zamiast double/float dla kwot

**Co:** Wszystkie pola finansowe (`balance`, `amount`, `limitAmount`) to `BigDecimal` mapowane na `NUMERIC(19, 2)` w bazie.

**Dlaczego:**
- `double` i `float` to IEEE 754 binary floating point — `0.1 + 0.2 = 0.30000000000000004`.
- To aplikacja finansowa — błędy zaokrągleń są niedopuszczalne.
- `BigDecimal` przechowuje precyzyjnie liczby dziesiętne.
- `NUMERIC(19, 2)` w Postgres: max 17 cyfr przed przecinkiem, 2 po — wystarczy dla każdego realistycznego salda.

---

### 7.3 Dwie daty: transactionDate vs createdAt

**Co:** Każda transakcja ma dwa pola czasu:
- `transaction_date` (DATE) — kiedy zdarzenie ekonomiczne miało miejsce (ustawiany przez użytkownika)
- `created_at` (TIMESTAMP) — kiedy rekord powstał w systemie (automatyczny, niemodyfikowalny)

**Dlaczego:**
- Użytkownik może wpisywać wydatki **wstecz** (np. wpisuje wczorajszy paragon dzisiaj).
- Filtry `?from=&to=` działają na `transaction_date` (logika biznesowa), nie na `created_at` (audyt).
- Gdyby była jedna data, retrospektywne wpisy zaburzałyby raporty.

---

### 7.4 Transakcje są niemutowalne (brak PUT)

**Co:** Pola transakcji (`type`, `amount`, `category`, `transactionDate`, `account_id`) mają `updatable = false` w JPA — Hibernate nigdy ich nie zaktualizuje. Nie ma endpointu `PUT /transactions/{id}`.

**Dlaczego:**
- Prostota: cofnięcie transakcji (DELETE) to prosta operacja odwrócenia kwoty. Gdyby transakcja mogła się zmienić, musielibyśmy śledzić "stary" i "nowy" stan.
- Integralność audytowa — raz zapisana transakcja jest dowodem operacji.
- PDF zadania nie przewiduje edycji — trzymamy się minimum.

---

### 7.5 FK z RESTRICT, nie CASCADE

**Co:** Relacja Transaction → Account ma `RESTRICT` na FK (domyślne zachowanie Postgres bez `ON DELETE CASCADE`).

**Dlaczego:**
- Chcemy **świadomie odrzucić** próbę usunięcia konta z transakcjami z kodem `409 Conflict`, nie cicho usunąć wszystkie transakcje.
- `CASCADE` byłoby niebezpieczne: jeden DELETE na koncie usuwa cały historię transakcji.
- Walidacja w `AccountService.delete`: sprawdzamy `transactionRepository.existsByAccountId(id)` → rzucamy `ConflictException` → `GlobalExceptionHandler` mapuje na `409`.

---

### 7.6 @Transactional na create i delete transakcji

**Co:** Metody `TransactionService.create()` i `TransactionService.delete()` mają adnotację `@Transactional`.

**Dlaczego:**
- Każda z tych metod wykonuje **dwie operacje na bazie**: zmiana salda konta + zapis/usunięcie transakcji.
- Bez `@Transactional`: jeśli drugie zapytanie się posypie, pierwsza zmiana zostaje w bazie — **niespójność danych** (saldo zmienione, transakcja nie zapisana).
- Z `@Transactional`: albo oba się udają (commit), albo oba cofają się (rollback) — atomowość.

---

### 7.7 Limity kategorii — encja w bazie, nie config

**Co:** Limity per kategoria przechowywane jako encja `CategoryLimit` w bazie, nie jako hardkodowane wartości w `application.yml`.

**Dlaczego:**
- Konfiguracja w YAML jest statyczna — zmiana wymaga restartu aplikacji.
- Encja w bazie = CRUD przez API — użytkownik może zarządzać limitami bez wiedzy technicznej.
- Kompromis: trochę więcej kodu, ale znacznie bardziej użyteczne.

---

### 7.8 Agregaty w bazie, nie w pamięci (summary)

**Co:** `/summary` używa `@Query` z `SUM` i `GROUP BY` w JPQL, nie pobiera wszystkich transakcji do Javy i nie liczy w pętli.

**Dlaczego:**
- Wydajność: baza danych jest zoptymalizowana do agregacji, Java nie.
- Gdyby było milion transakcji — pobieranie ich wszystkich do pamięci to OOM i timeout.
- `SUM(...) GROUP BY category` zwraca tyle wierszy co kategorii, nie ile transakcji.

---

## 8. Obsługa błędów

### Wyjątki niestandardowe

| Klasa | Kod HTTP | Kiedy |
|---|---|---|
| `ResourceNotFoundException` | 404 | konto lub transakcja nie istnieje |
| `ConflictException` | 409 | usunięcie konta, które ma transakcje |

### GlobalExceptionHandler

`@RestControllerAdvice` — przechwytuje wyjątki z **wszystkich** kontrolerów w jednym miejscu.

```
Rzucony wyjątek
       │
       ▼
GlobalExceptionHandler
       │
       ├── ResourceNotFoundException → 404 JSON
       ├── ConflictException         → 409 JSON
       ├── MethodArgumentNotValidException → 400 JSON (walidacja)
       └── HttpMessageNotReadableException → 400 JSON (zły format body)
```

Format odpowiedzi błędu (`ErrorResponse`):
```json
{
  "timestamp": "2025-06-04T10:30:00",
  "status": 404,
  "error": "Not Found",
  "message": "Account not found: 550e8400-...",
  "path": "/accounts/550e8400-..."
}
```

### Walidacja requestów

DTO requestów mają adnotacje Bean Validation:
- `@NotNull` — pole nie może być null
- `@NotBlank` — String nie może być pusty ani sama biała spacja
- `@Positive` — liczba musi być > 0
- `@Size(max = 100)` — max długość stringa

Kontroler: `@RequestBody @Valid TransactionCreateRequest request` → Spring automatycznie waliduje przed wywołaniem metody.

---

## 9. Warstwy szczegółowo

### Repository (interfejsy JPA)

```java
public interface AccountRepository extends JpaRepository<Account, UUID> {
    // Spring Data JPA generuje implementację automatycznie
    // JpaRepository daje: save(), findById(), findAll(), delete()...
}
```

`TransactionRepository` ma dodatkowe metody:
- `existsByAccountId(UUID)` — Spring Data generuje query z nazwy metody
- `findByAccountIdOrderByTransactionDateDesc(UUID)` — też z nazwy
- `@Query(...)` — ręczny JPQL dla złożonych zapytań z opcjonalnymi filtrami i agregacjami

### Trick z opcjonalnymi filtrami w JPQL

```sql
SELECT t FROM Transaction t WHERE
    t.transactionDate >= COALESCE(:from, t.transactionDate) AND
    t.transactionDate <= COALESCE(:to, t.transactionDate) AND
    (:category IS NULL OR t.category = :category)
```

`COALESCE(:from, t.transactionDate)` — jeśli `from` jest null, porównuje datę do niej samej (zawsze prawda). Efekt: parametr jest opcjonalny bez pisania wielu wersji zapytania.

---

## 10. DTO — dlaczego nie zwracamy encji bezpośrednio?

**DTO (Data Transfer Object)** — osobna klasa tylko do przesyłania danych przez API.

Zamiast:
```java
public Account create(...) { return accountRepository.save(account); }  // zwraca encję
```

Robimy:
```java
public AccountResponse create(...) { return toResponse(accountRepository.save(account)); }  // zwraca DTO
```

**Dlaczego:**
1. **Bezpieczeństwo** — encja może mieć pola, których nie chcemy eksponować (np. wewnętrzne metadane, hasła).
2. **Decoupling** — zmiana schematu bazy nie musi zmieniać API kontraktu.
3. **Kształt danych** — w response możemy mieć pola pochodne albo zrestrukturyzowane dane.
4. **Cykliczne referencje** — Account ma listę transakcji, Transaction ma Account → przy serializacji do JSON byłoby nieskończone zagnieżdżenie.

---

## 11. Lombok — co generuje

| Adnotacja | Generuje |
|---|---|
| `@Data` | gettery, settery, `equals`, `hashCode`, `toString` |
| `@Builder` | builder pattern: `Account.builder().name("...").build()` |
| `@NoArgsConstructor` | konstruktor bezparametrowy (wymagany przez JPA) |
| `@AllArgsConstructor` | konstruktor ze wszystkimi polami (potrzebny przy `@Builder`) |
| `@RequiredArgsConstructor` | konstruktor z polami `final` (Dependency Injection) |
| `@ToString.Exclude` | wyklucza pole z `toString` (używane na relacji `@ManyToOne` żeby uniknąć lazy loading) |

---

## 12. Co bym zmienił / dodał z większą ilością czasu

To jedno z pewnych pytań na rozmowie — przygotuj to dobrze.

1. **Flyway** zamiast `ddl-auto: update`
   - `ddl-auto: update` — Hibernate sam modyfikuje schemat bazy na starcie. Działa na dev, niebezpieczne na produkcji (może utracić dane).
   - Flyway = migracje jako pliki SQL (`V1__init.sql`, `V2__add_index.sql`) — wersjonowany, audytowalny, odwracalny.

2. **UUIDv7** zamiast UUIDv4
   - UUIDv4 jest losowy → fragmentacja indeksu B-tree w Postgres.
   - UUIDv7 jest time-ordered (monotoniczny) → wkłada nowe rekordy na koniec indeksu, tak jak Auto Increment Long.

3. **Indeksy bazodanowe**
   - `(account_id, transaction_date)` — najczęstsze zapytanie: transakcje konta w zakresie dat.
   - `category` na transactions — filtrowanie i grupowanie.

4. **Wersjonowanie API** (`/api/v1/...`)
   - Umożliwia zmiany API bez łamania klientów na starszej wersji.

5. **Paginacja** na `/transactions` i `/accounts`
   - Przy dużej liczbie transakcji zwracanie wszystkich na raz to problem wydajnościowy.
   - Spring Data: `Pageable` + `Page<T>`.

6. **Spring Security + JWT**
   - Teraz API jest całkowicie otwarte.
   - Produkcyjnie: każdy użytkownik widzi tylko swoje konta.

---

## 13. Jak uruchomić projekt (dla pewności)

```bash
# 1. Uruchom bazę danych
docker-compose up -d

# 2. Uruchom aplikację
./mvnw spring-boot:run

# 3. Swagger UI
open http://localhost:8080/swagger-ui.html
```

Baza konfigurowana w `application.properties`:
```
spring.datasource.url=jdbc:postgresql://localhost:5432/budget
spring.datasource.username=budget
spring.datasource.password=budget
spring.jpa.hibernate.ddl-auto=update  ← Hibernate tworzy/aktualizuje schemat automatycznie
```

---

## 14. Najczęstsze pytania i krótkie odpowiedzi

**Q: Dlaczego Spring Boot zamiast zwykłego Servlet API?**
Spring Boot eliminuje konfigurację XML, ma wbudowany serwer, auto-configuration. Skupiasz się na logice, nie na infrastrukturze.

**Q: Co to jest JPA vs Hibernate?**
JPA (Jakarta Persistence API) to specyfikacja (interfejsy). Hibernate to implementacja tej specyfikacji. Spring Data JPA dodaje warstwę repozytorium ponad Hibernate.

**Q: Dlaczego FetchType.LAZY na relacji Account?**
Domyślne EAGER załadowałoby Account przy każdym pobraniu Transaction, nawet gdy nie potrzebujemy. LAZY ładuje Account tylko gdy faktycznie go użyjemy — oszczędność zapytań.

**Q: Co to jest @Transactional dokładnie?**
Spring otwiera transakcję przed wejściem do metody i commituje po jej wyjściu. Jeśli wyleci wyjątek (RuntimeException), transakcja jest wycofana (rollback). Zapewnia atomowość operacji multi-step.

**Q: Dlaczego walidacja w DTO, nie w Service?**
Walidacja formatu (czy pole jest puste, czy liczba jest dodatnia) to odpowiedzialność warstwy wejścia. Service zakłada, że dane które dostaje są poprawne formalnie i skupia się na logice.

**Q: Co robi @RestControllerAdvice?**
To aspekt (AOP) — Spring przechwytuje wyjątki z każdego `@RestController` w aplikacji. Bez tego każdy kontroler musiałby sam obsługiwać błędy.
