# AI_NOTES.md

Krótka notatka o roli AI (Claude Code) w tym projekcie.

Projekt był budowany etap po etapie z pomocą Claude Code jako narzędzia do generowania kodu. Poniżej wybrane momenty, w których podjąłem świadome decyzje projektowe — nie akceptowałem propozycji automatycznie.

---

## Decyzje podjęte po przedstawieniu opcji przez AI

- **UUID zamiast Long jako typ ID** — Claude przedstawił trade-offy (bezpieczeństwo URL vs prostota). Wybrałem UUID, bo ID są wystawiane w URL-ach i `Long` pozwala na enumerację zasobów.

- **Filtry `?from=&to=` w `/summary`** — Claude zapytał czy dodać filtry dat do endpointu `/summary`. Wybrałem opcję z filtrami dla spójności z `/transactions`, gdzie te same filtry już istnieją.

- **Encja `CategoryLimit` w bazie zamiast konfiguracji w `application.properties`** — Claude przedstawił dwa podejścia. Wybrałem encję w bazie, bo limity można wtedy zmieniać przez API bez restartu aplikacji.

- **Java 21 zamiast 17** — specyfikacja zadania mówiła Java 17, ale `stock-market-api` i Spring Initializr używały 21. Claude wskazał niespójność. Zdecydowałem zostać przy 21 (nowszy LTS, w pełni kompatybilny).

## Miejsca gdzie aktywnie weryfikowałem wygenerowany kod

- Pytałem o znaczenie poszczególnych adnotacji JPA i Lombok (`@ManyToOne`, `@FetchType.LAZY`, `@Builder`, `@NoArgsConstructor`) — rozumiem każdą z nich i potrafię uzasadnić jej użycie.

- Pytałem o wzorzec `:param IS NULL OR warunek` w JPQL — zrozumiałem jak działa obsługa opcjonalnych filtrów i dlaczego jedna metoda `findWithFilters` obsługuje wszystkie kombinacje parametrów.

- Pytałem o `NUMERIC(19, 2)` — zrozumiałem czym jest `precision` vs `scale` i dlaczego `BigDecimal` jest jedynym sensownym wyborem w aplikacji finansowej.
