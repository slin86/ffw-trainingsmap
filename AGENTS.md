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

- [x] Projektgerust: Boot 4.1.0 + Gradle Kotlin DSL + Java 21 Toolchain
- [x] Dependencies: web, thymeleaf, data-jpa, security, validation, flyway, postgresql, redis-session, actuator
- [x] Flyway Migration `V1`: Tabellen `vehicle` und `app_user`
- [x] JPA-Entitaten + Spring Data Repositories fur beide Tabellen
- [x] `application.yml` mit Env-Variablen: DB_URL, DB_USER, DB_PASSWORD, REDIS_HOST
- [x] `docker-compose.yml`: postgres:16 + redis:7
- [x] SecurityConfig: BCrypt + UserDetailsService aus DB (Spring Security 7)
- [x] DataSeeder (`dev` Profil): admin/admin + 4 Hamburger FF-Fahrzeuge
- [x] Build erfolgreich: `./gradlew build` ohne Fehler

## Noch offen

- REST/API Controller fur Fahrzeug-Daten
- Thymeleaf-Seiten mit OpenStreetMap Karte (z.B. Leaflet)
- CI/CD Pipeline, unit/integration tests
- Dockerfile fur Produktivdeployment
