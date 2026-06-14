supe# Notatki — personal-budget-api

Plik prywatny, nie wchodzi do repo. Tu zbieram wyjaśnienia rzeczy, o które pytałem podczas pracy nad projektem.

glowne: d3dc66fd-2fa7-4654-9968-0113d9de2de2
oszczednosci: 882a94ba-62f5-43c4-bc4a-0bcb8850e887

---

## Adnotacje na klasie encji (`Account.java`)

### JPA / Hibernate — mówią Hibernate'owi co to za klasa

- **`@Entity`** — "ta klasa to tabela w bazie". Hibernate zobaczy ją przy starcie i stworzy/zaktualizuje tabelę.
- **`@Table(name = "accounts")`** — jawna nazwa tabeli w bazie. Bez tego Hibernate użyłby domyślnie `account` (nazwa klasy lowercase). Lepiej pisać jawnie.

### Lombok — generują kod Java w czasie kompilacji (nie ma go w źródle, ale jest w skompilowanym `.class`)

- **`@Data`** — generuje gettery, settery, `equals()`, `hashCode()`, `toString()` dla wszystkich pól. Bez tego ~50 linii boilerplate'u ręcznie.
- **`@Builder`** — generuje fluent builder: `Account.builder().name("Konto").balance(BigDecimal.ZERO).build()`. Używamy w serwisie zamiast konstruktora z 4 argumentami.
- **`@NoArgsConstructor`** — generuje konstruktor bezargumentowy `Account()`. **JPA tego wymaga** — Hibernate tworzy obiekty przez refleksję i potrzebuje pustego konstruktora.
- **`@AllArgsConstructor`** — generuje konstruktor ze wszystkimi polami. **Wymagany przez `@Builder`** — Lombok's builder korzysta z niego wewnętrznie. Musi być jawny, bo `@NoArgsConstructor` "cofa" automatyczny all-args z `@Builder`.

### Dlaczego `@NoArgsConstructor` + `@AllArgsConstructor` razem?

Normalnie `@Builder` sam generuje konstruktor all-args. Ale gdy dodajesz `@NoArgsConstructor`, Lombok cofa automatyczny all-args z `@Builder` i musisz go dopisać ręcznie — stąd `@AllArgsConstructor`.

### Analogia w Pythonie

```python
# @Data to mniej więcej to samo co:
@dataclass
class Account:
    name: str
    balance: Decimal
```

Tylko że Java bez adnotacji wymaga ręcznego pisania getterów/setterów.

---

## `@ManyToOne(fetch = FetchType.LAZY)` — relacja i strategia wczytywania

`@ManyToOne` mówi JPA: "wiele transakcji należy do jednego konta" — relacja FK w bazie.

`FetchType.LAZY` (leniwy) — wczytaj dane konta dopiero gdy ktoś wywoła `transaction.getAccount()`. Samo pobranie transakcji nie robi JOINa z tabelą accounts.

`FetchType.EAGER` (zachłanny) — zawsze rób JOIN, nawet jeśli konta nie potrzebujesz. Droższe.

LAZY jest lepszy — nie ładujesz danych których nie potrzebujesz.

---

## `@JoinColumn(name = "account_id", nullable = false, updatable = false)`

Mówi Hibernate jak nazwać kolumnę FK w bazie. Jak `@Column(name = "...")` ale dla relacji.

- `name = "account_id"` → nazwa kolumny w tabeli `transactions`
- `nullable = false` → NOT NULL
- `updatable = false` → nie można zmienić przypisania transakcji do konta po zapisie

---

## `@Query` z opcjonalnymi filtrami — wzorzec `:param IS NULL OR warunek`

JPQL (Java Persistence Query Language) — język zapytań podobny do SQL ale operuje na klasach Javy:
```sql
SELECT t FROM Transaction t   -- zamiast: SELECT * FROM transactions
```

Wzorzec obsługujący opcjonalne parametry:
```sql
(:from IS NULL OR t.transactionDate >= :from)
```
- `from = null` → `IS NULL` jest true → cały `OR` jest true → filtr pomijany
- `from = 2024-01-01` → `IS NULL` false → sprawdzany drugi człon: `transactionDate >= 2024-01-01`

Jedno zapytanie obsługuje wszystkie kombinacje filtrów. `@Param("from")` wiąże parametr metody z nazwą `:from` w zapytaniu.

---

## Bean Validation — `@NotBlank`, `@Size` i po co to w DTO?

### Co to jest

Adnotacje z Bean Validation (standard Javy). Działają w momencie gdy Spring odbiera request HTTP — **zanim cokolwiek trafi do bazy**.

- `@NotBlank` — pole nie może być `null`, `""` ani samymi spacjami `"   "`
- `@Size(max = 100)` — długość stringa max 100 znaków

Żeby zadziałały, kontroler musi mieć `@Valid` na parametrze:
```java
public AccountResponse create(@RequestBody @Valid AccountCreateRequest request)
//                                                ↑ to uruchamia walidację
```
Jeśli walidacja nie przejdzie → Spring zwraca `400 Bad Request` bez wchodzenia do serwisu.

### Dwie warstwy walidacji — obie są potrzebne

| Warstwa | Gdzie | Po co |
|---|---|---|
| Bean Validation (`@NotBlank`) | Kontroler, przed serwisem | Szybki `400` z czytelnym komunikatem dla klienta API |
| Constrainty w bazie (`NOT NULL`) | PostgreSQL | Ostatnia linia obrony, niezależna od aplikacji |

Baza i tak ma `NOT NULL` — ale jej komunikat błędu byłby techniczny i brzydki. Bean Validation daje `400` z `"Name is required"`.

---

## Swagger / OpenAPI — adnotacje `@Tag`, `@Operation`, `@ApiResponse`

Springdoc OpenAPI czyta adnotacje i generuje automatycznie dokumentację dostępną pod `/swagger-ui.html`.

### `@Tag(name = "Accounts", description = "...")`
Naklejka na całą klasę kontrolera. Grupuje wszystkie endpointy z tej klasy w jedną sekcję w Swagger UI. Bez tego wszystkie endpointy wiszą płasko bez grupowania.

### `@Operation(summary = "Create a new account")`
Opis pojedynczego endpointu — pojawia się jako tytuł w Swagger UI. Można dodać `description = "..."` dla dłuższego opisu.

### `@ApiResponse(responseCode = "201", description = "Account created")`
Dokumentuje możliwe kody odpowiedzi HTTP. **Swagger nie wymusza tych kodów** — to tylko dokumentacja dla konsumenta API. Zbiera się je w `@ApiResponses({...})` gdy jest ich więcej niż jeden.

### Jak to wygląda bez adnotacji vs z adnotacjami
Bez adnotacji Springdoc i tak generuje dokumentację (bo widzi `@GetMapping`, `@PostMapping` itd.), ale bez opisów — same suche ścieżki. Adnotacje dodają tylko czytelność.

---

## CSV export — `GET /accounts/{id}/transactions/export`

### `Content-Type: text/csv`
Mówi przeglądarce/klientowi jaki format ma odpowiedź. Bez tego przeglądarka nie wiedziałaby co z tym zrobić.

### `Content-Disposition: attachment; filename=transactions_XYZ.csv`
Kluczowy nagłówek. `attachment` mówi przeglądarce "nie wyświetlaj tego — zaproponuj pobranie jako plik". Bez tego przeglądarka próbowałaby wyświetlić CSV jako tekst.

### `HttpServletResponse response` w kontrolerze
Zamiast zwracać obiekt DTO, piszemy bezpośrednio do strumienia odpowiedzi HTTP przez `response.getWriter()`. Dzięki temu nie ładujemy całego CSV do pamięci — dane płyną wiersz po wierszu.

### `escapeCsv(String value)` — dlaczego?
CSV używa przecinka jako separatora. Jeśli pole (np. `description`) zawiera przecinek, plik się rozjedzie:
```
2024-01-01,EXPENSE,50.00,Food,Pizza, duża    ← ZŁE: parser widzi 6 kolumn zamiast 5
```
Rozwiązanie: pole zawierające przecinek owijamy cudzysłowami:
```
2024-01-01,EXPENSE,50.00,Food,"Pizza, duża"  ← DOBRZE
```
Jeśli samo pole zawiera cudzysłów `"`, podwajamy go: `"` → `""` (standard CSV/RFC 4180).

---

## Bean Validation — szczegółowo

### Pełna lista adnotacji użytych w projekcie

| Adnotacja | Gdzie | Co sprawdza |
|---|---|---|
| `@NotBlank` | `name`, `category` | Nie null, nie `""`, nie same spacje |
| `@NotNull` | `type`, `amount`, `accountId` | Nie null (ale może być puste) |
| `@Positive` | `amount` | Liczba > 0 (wyklucza 0 i ujemne) |
| `@Size(max = 100)` | `name`, `category` | Długość stringa ≤ 100 |

### Jak działa walidacja krok po kroku
1. Klient wysyła `POST /accounts` z body `{"name": ""}`
2. Spring deserializuje JSON → obiekt `AccountCreateRequest`
3. `@Valid` na parametrze kontrolera → Spring uruchamia Bean Validation na tym obiekcie
4. `@NotBlank` na `name` nie przechodzi → Spring rzuca `MethodArgumentNotValidException`
5. `GlobalExceptionHandler` łapie ten wyjątek → buduje `ErrorResponse` z kodem `400`
6. Klient dostaje: `{"status": 400, "message": "name: Name is required", ...}`

Serwis i baza **nigdy nie są dotykane** gdy walidacja nie przejdzie.

---

## GlobalExceptionHandler — jak działa obsługa błędów

### `@RestControllerAdvice`
Specjalna klasa która "słucha" na wyjątki rzucane z dowolnego kontrolera w aplikacji. Jest jedna globalna — bez niej każdy wyjątek dałby `500 Internal Server Error` z brzydkim stack trace.

### `@ExceptionHandler(ResourceNotFoundException.class)`
Mówi: "gdy gdziekolwiek w aplikacji zostanie rzucony `ResourceNotFoundException`, wywołaj tę metodę". Spring przechwytuje wyjątek zanim dotrze do klienta.

### Metoda `build()` — jeden punkt odpowiedzi błędu
```java
private ResponseEntity<ErrorResponse> build(HttpStatus status, String message, HttpServletRequest request) {
    ErrorResponse body = new ErrorResponse(
        LocalDateTime.now(),   // timestamp — kiedy wystąpił błąd
        status.value(),        // 404, 400, 409...
        status.getReasonPhrase(), // "Not Found", "Bad Request"...
        message,               // np. "Account not found: abc-123"
        request.getRequestURI()   // np. "/accounts/abc-123"
    );
    return ResponseEntity.status(status).body(body);
}
```

### Co łapie handler i jakie kody zwraca

| Wyjątek | Kod | Skąd rzucany |
|---|---|---|
| `ResourceNotFoundException` | 404 | Serwis gdy nie ma encji w bazie |
| `ConflictException` | 409 | `AccountService.delete()` gdy konto ma transakcje |
| `MethodArgumentNotValidException` | 400 | Spring automatycznie po nieudanej walidacji `@Valid` |
| `HttpMessageNotReadableException` | 400 | Spring gdy JSON w body jest uszkodzony / brakuje body |

### `ErrorResponse` jako Java record
```java
public record ErrorResponse(LocalDateTime timestamp, int status, String error, String message, String path) {}
```
`record` to skrócony sposób zapisu klasy w Java 16+. Automatycznie generuje konstruktor, gettery, `equals`, `hashCode`, `toString`. Idealny dla niezmiennych DTO — raz stworzony obiekt nie może być zmieniony.

---

## Testy — jak są zbudowane

### Dwa rodzaje testów w projekcie

#### 1. Testy jednostkowe (`AccountServiceTest`, `TransactionServiceTest`)
- **Nie startują Spring** — szybkie (milisekundy), bez bazy danych
- Używają **Mockito** — biblioteki do tworzenia "atrap" (mocków) zależności
- `@ExtendWith(MockitoExtension.class)` — uruchamia Mockito zamiast Springa
- `@Mock` — tworzy atrapę klasy (np. `AccountRepository`), która nic nie robi dopóki nie powiesz jej co ma zwracać
- `@InjectMocks` — tworzy testowany obiekt (np. `AccountService`) i wstrzykuje do niego mocki zamiast prawdziwych zależności

**Jak działają mocki:**
```java
// "gdy ktoś wywoła findById z tym ID, zwróć ten obiekt"
when(accountRepository.findById(id)).thenReturn(Optional.of(account));

// "sprawdź że metoda save() była wywołana dokładnie raz"
verify(accountRepository).save(any());

// "sprawdź że delete() NIE była wywołana"
verify(accountRepository, never()).delete(any());
```

**ArgumentCaptor** — przechwytuje argument przekazany do mocka, żeby sprawdzić jego wartość:
```java
ArgumentCaptor<Account> captor = ArgumentCaptor.forClass(Account.class);
verify(accountRepository).save(captor.capture());
// teraz mogę sprawdzić co dokładnie było zapisane:
assertThat(captor.getValue().getBalance()).isEqualByComparingTo("1500.00");
```

**Dlaczego `isEqualByComparingTo` a nie `isEqualTo` dla BigDecimal:**
`BigDecimal("1500.00").equals(BigDecimal("1500.0"))` → `false` (różna skala!)
`BigDecimal("1500.00").compareTo(BigDecimal("1500.0"))` → `0` (wartości równe)
`isEqualByComparingTo` używa `compareTo` — porównuje wartość, nie reprezentację.

#### 2. Testy integracyjne (`BudgetIntegrationTest`)
- **Startują cały Spring** — wolniejsze (~2 min), ale testują prawdziwe zachowanie
- Używają **Testcontainers** — biblioteka która startuje prawdziwy PostgreSQL w Dockerze tylko na czas testów
- `@SpringBootTest` — ładuje pełny kontekst Springa (wszystkie beany, baza, HTTP)
- `@AutoConfigureMockMvc` — daje `MockMvc` do wysyłania requestów HTTP bez otwierania portu
- `@Import(TestcontainersConfiguration.class)` — podłącza PostgreSQL z Dockera zamiast produkcyjnej bazy

**`@BeforeEach void setUp()`** — przed każdym testem czyści bazę:
```java
transactionRepository.deleteAll();  // najpierw transakcje (FK!)
accountRepository.deleteAll();       // potem konta
```
Kolejność ważna — transakcje mają FK do kont, więc trzeba je usunąć pierwsze.

**Dlaczego nie `@Transactional` na teście integracyjnym:**
`@Transactional` na teście rollbackuje po każdej metodzie. Ale `MockMvc` wywołuje kontroler przez warstwę HTTP, gdzie serwis otwiera i zamyka **własną** transakcję przed powrotem. Do czasu rollbacku testu dane są już zacommitowane. Ręczne `deleteAll()` jest jedynym pewnym rozwiązaniem.

**Asercje MockMvc:**
```java
mockMvc.perform(get("/accounts/" + id))     // wyślij GET request
    .andExpect(status().isOk())              // oczekuj HTTP 200
    .andExpect(jsonPath("$.balance").value(3300.00))  // oczekuj pole w JSON
    .andExpect(jsonPath("$.error").value("Not Found")); // oczekuj wartość
```
`jsonPath("$.balance")` — wyrażenie JSONPath: `$` = root, `.balance` = pole o tej nazwie.

---

## `AccountRepository extends JpaRepository<Account, UUID>` — pusty interfejs

`JpaRepository` to interfejs ze Spring Data JPA. Dziedzicząc po nim dostajesz gotowe metody CRUD bez pisania ani linijki SQL:

```java
accountRepository.save(account)       // INSERT / UPDATE
accountRepository.findById(id)        // SELECT WHERE id = ?
accountRepository.findAll()           // SELECT * FROM accounts
accountRepository.deleteById(id)      // DELETE WHERE id = ?
accountRepository.existsById(id)      // SELECT COUNT(*) WHERE id = ?
```

Spring przy starcie aplikacji **sam generuje implementację** tego interfejsu w pamięci. Ty nigdy nie piszesz klasy `AccountRepositoryImpl`.

`<Account, UUID>` — dwa parametry generyczne:
- `Account` — jakiej encji dotyczy repo
- `UUID` — typ klucza głównego (`id`)

Jeśli potrzebujesz czegoś niestandardowego — dopisujesz metodę i Spring rozumie nazwę:
`findByBalanceGreaterThan(BigDecimal amount)` → `SELECT * WHERE balance > ?`

**Analogia:** jak `list` w Pythonie — dostajesz `.append()`, `.remove()`, `len()` bez pisania tych metod. Tu dostajesz CRUD na bazie.

---

## `@GeneratedValue(strategy = GenerationType.UUID)`

Mówi JPA: "przy zapisie do bazy wygeneruj automatycznie wartość dla tego pola". Strategia `UUID` oznacza że Hibernate sam losuje UUID (v4) — nie musisz nic podawać przy tworzeniu obiektu.

Przed Spring Boot 3.x tego nie było i trzeba było używać `@GenericGenerator` z Hibernate — teraz to jedna linijka.

---

## `@Column(nullable = false, precision = 19, scale = 2)` — co to precision i scale?

Dotyczy `BigDecimal` / `NUMERIC` w SQL:

- **`precision = 19`** — łączna liczba cyfr (przed i po przecinku). 19 daje max ~9 biliardów.
- **`scale = 2`** — ile miejsc po przecinku. `2` = grosze/centy.

Bez tych parametrów Hibernate wygenerowałby `NUMERIC(38, 2)` albo coś bez precyzji — zależnie od bazy. Lepiej być jawnym w kontekście finansowym.

**Jak liczyć:** `precision - scale = cyfry przed przecinkiem`. Czyli `19 - 2 = 17` cyfr przed przecinkiem → max ~100 biliardów. Miejsca po przecinku są zawsze dokładnie 2.

```
NUMERIC(19, 2):
[ 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 . 8 9 ]
|←————————— 17 cyfr ————————————→| |← 2 →|
```

`nullable = false` → kolumna `NOT NULL` w bazie.

---
