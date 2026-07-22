# ff-trainingskarte

Training-Tool fur die Freiwillige Feuerwehr zur Darstellung von Fahrzeugen auf einer Hamburg-Karte.

## Stack

- Java 21, Gradle (Kotlin DSL)
- **Spring Boot 4.1**, Jackson 3 mit `tools.jackson.*` Packages
- PostgreSQL 16 + Flyway Migrationen
- Redis 7 fur HTTP-Sessions
- Spring Security 7 und Thymeleaf fuer Web UI

## Konventionen

- Constructor injection, kein Lombok, keine `@Setter`/`@Autowired` auf Feldern
- UI-Texte auf Deutsch, Code und Variablen auf Englisch
- Keine globalen Imports; explizite Pakete immer voll qualifiziert wenn notig
- Kein `javax.*`; nur `jakarta.*` namespaces

## Current State (Erreichnisse)

### M1–M3: Grundgerüst & Datenlage
- [x] Projektgerust: Boot 4.1.0 + Gradle Kotlin DSL + Java 21 Toolchain
- [x] Dependencies: web, thymeleaf, data-jpa, security, validation, flyway, postgresql, redis-session, actuator
- [x] Flyway Migration `V1`: Tabellen `vehicle` und `app_user`
- [x] JPA-Entitaten + Spring Data Repositories fur beide Tabellen
- [x] `application.yml` mit Env-Variablen: DB_URL, DB_USER, DB_PASSWORD, REDIS_HOST
- [x] `docker-compose.yml`: postgres:16 + redis:7
- [x] SecurityConfig: BCrypt + UserDetailsService aus DB (Spring Security 7)
- [x] DataSeeder (`dev` Profil): admin/admin + 4 Hamburger FF-Fahrzeuge

### M4: Admin UI & Tests (complete)
- [x] REST API (`VehicleController`) fuers Frontend
- [x] Admin-Kontroller: `AdminVehicleController`, `AdminUserController` mit Flash-Messages (Deutsch)
- [x] Thymeleaf-Templates: Fahrzeugliste, Fahrzeug-Bearbeitung (Leaflet-Karte), Nutzerverwaltung
- [x] Eigene CSS (`/css/admin.css`), kein Framework
- [x] ADMIN-only Zugriff auf `/admin/**`; VIEWER erholt 403
- [x] Selbstschutz bei Nutzerverwaltung: keine eigene Loeschung/Deaktivierung
- [x] `@WebMvcTest`: `AdminVehicleControllerTest`, `AdminUserControllerTest` (Sichtbarkeit, CRUD, Selbstschutz)
- [x] Navigation zwischen Admin-Seiten gegenseitig verlinkt; `/map` --> `/` korrigiert
- [x] `./gradlew build` erfolgreich, 31 Tests green
- [x] `Vehicle.getStatusLabel()` fuers Template

## Noch offen

- CI/CD Pipeline
- Dockerfile fur Produktivdeployment
