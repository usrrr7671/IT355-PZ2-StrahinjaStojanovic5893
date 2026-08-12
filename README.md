# Help Desk / Ticketing sistem

Drugi projektni zadatak iz predmeta **IT355 – Veb sistemi 2**, Univerzitet Metropolitan Beograd.

- **Student:** Strahinja Stojanović
- **Indeks:** 5893
- **Tema:** Help desk / ticketing sistem sa RBAC kontrolom pristupa i internom prepiskom (chat)

## O aplikaciji

Full-stack aplikacija za prijavu i rešavanje tiketa tehničke podrške. Korisnik prijavljuje
problem (tiket), agent ga preuzima i menja status kroz životni ciklus, administrator upravlja
šifarnicima i nalozima. Pored javnih komentara na tiketu, sistem ima i **direktnu prepisku
(chat) između korisnika i agenta**, kao i **interne beleške** vidljive samo osoblju podrške —
što čini kontrolu pristupa (RBAC) centralnim delom teme.

## Akteri (uloge)

| Uloga | Opis |
|-------|------|
| `ROLE_USER` | Prijavljuje tikete, komentariše svoje tikete, dopisuje se sa agentom |
| `ROLE_AGENT` | Preuzima tikete, menja status i prioritet, piše interne beleške |
| `ROLE_ADMIN` | Sve što i agent + upravljanje korisnicima, kategorijama i prioritetima |

## Tehnologije

**Backend:** Java 21, Spring Boot 3.5.x, Spring Web, Spring Data JPA, Spring Security (JWT),
Bean Validation, H2 baza, springdoc-openapi (Swagger UI)
**Testiranje:** JUnit 5, Mockito, Spring Boot Test, MockMvc
**Frontend:** React (Vite)

## Pokretanje

### Backend

```bash
cd backend
./mvnw spring-boot:run
```

Aplikacija se podiže na `http://localhost:8080`.

- Swagger UI: `http://localhost:8080/swagger-ui.html`
- H2 konzola: `http://localhost:8080/h2-console`

### Frontend

```bash
cd frontend
npm install
npm run dev
```

## Struktura repozitorijuma

```
backend/    Spring Boot REST API
frontend/   React klijentska aplikacija
docs/       Dokumentacija projekta
```
