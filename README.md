# Pult — Help desk / ticketing sistem

Drugi projektni zadatak iz predmeta **IT355 – Veb sistemi 2**, Univerzitet Metropolitan Beograd.

- **Student:** Strahinja Stojanović
- **Indeks:** 5893
- **Tema:** Help desk / ticketing sistem sa RBAC kontrolom pristupa i internom prepiskom
- **Dokumentacija:** [`docs/DOKUMENTACIJA.md`](docs/DOKUMENTACIJA.md) — isti sadržaj za predaju: [`docs/IT355-PZ2-dokumentacija.docx`](docs/IT355-PZ2-dokumentacija.docx)

## O aplikaciji

Full-stack aplikacija za prijavu i rešavanje tiketa tehničke podrške. Korisnik prijavljuje
problem, agent ga preuzima i vodi kroz životni ciklus, administrator održava naloge i
šifarnike.

Kontrola pristupa je nosiva tema, a ne dodatak: isti tiket i isti URL daju različit odgovor
u zavisnosti od toga ko pita. Korisnik ne vidi tuđe tikete, ne menja status, ne vidi interne
beleške podrške — a privatnu prepisku ne može da otvori ni administrator.

![Detalji tiketa](docs/slike/05-detalji-tiketa-agent.png)

## Uloge

| Uloga | Šta sme |
|---|---|
| `ROLE_USER` | Prijavljuje tikete, vidi i komentariše **samo svoje**, ponovo otvara rešen tiket, dopisuje se sa podrškom |
| `ROLE_AGENT` | Vidi sve tikete, menja status kroz životni ciklus, dodeljuje tikete, piše interne beleške |
| `ROLE_ADMIN` | Sve što i agent + nalozi, kategorije, prioriteti, brisanje tiketa |

## Šta je implementirano

- **8 JPA entiteta** — korisnik, tiket, komentar, istorija statusa, kategorija, prioritet, prepiska, poruka
- **39 REST endpointa** u 9 kontrolera, dokumentovanih kroz Swagger UI
- **JWT autentifikacija** bez stanja na serveru, sa BCrypt hešovanjem lozinki
- **RBAC na tri nivoa** — ruta, metoda (`@PreAuthorize`) i pojedinačan zapis (servisni sloj)
- **Životni ciklus tiketa** kao konačni automat sa propisanim prelazima i punom istorijom
- **SLA rok odziva** izveden iz prioriteta, sa označavanjem probijenih rokova
- **Prepiska** između korisnika i podrške, sa brojem nepročitanih poruka
- **48 testova** — jedinični (Mockito) i integracioni (MockMvc sa stvarnim JWT tokenom)
- **React frontend** sa devet ekrana i prilagodljivim rasporedom

## Tehnologije

**Backend:** Java 21 · Spring Boot 3.5 · Spring Web · Spring Data JPA · Spring Security 6.5
· Bean Validation · jjwt · H2 · springdoc-openapi
**Testiranje:** JUnit 5 · Mockito · Spring Boot Test · MockMvc
**Frontend:** React 19 · Vite · React Router · Axios · ručno pisani CSS

## Pokretanje

Potrebni su **JDK 21+** i **Node.js 18+**.

### Backend — port 8080

```bash
cd backend
./mvnw spring-boot:run
```

### Frontend — port 5173

```bash
cd frontend
npm install
npm run dev
```

Aplikacija se otvara na **http://localhost:5173**.

### Testovi

```bash
cd backend
./mvnw test
```

## Nalozi za probu

Kreiraju se automatski pri prvom pokretanju, zajedno sa pet demonstracionih tiketa.
Lozinka za sve naloge: **`lozinka123`**

| Korisničko ime | Uloga |
|---|---|
| `admin` | Administrator |
| `agent1`, `agent2` | Agent |
| `pera`, `mika` | Korisnik |

## Korisni linkovi

| Adresa | Sadržaj |
|---|---|
| http://localhost:5173 | Korisnički interfejs |
| http://localhost:8080/swagger-ui.html | Swagger UI |
| http://localhost:8080/h2-console | H2 konzola (`jdbc:h2:file:./data/helpdesk`) |

## Struktura repozitorijuma

```
backend/    Spring Boot REST API
frontend/   React (Vite) klijentska aplikacija
docs/       Dokumentacija projekta i slike ekrana
```
