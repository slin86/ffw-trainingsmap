# ff-trainingskarte

Training-Tool fuer die Freiwillige Feuerwehr zur Darstellung von Fahrzeugen auf einer Hamburg-Karte.

## Stack

- Java 21, Gradle (Kotlin DSL)
- **Spring Boot 4.1**, Jackson 3 mit `tools.jackson.*` Packages
- PostgreSQL 16 + Flyway Migrationen
- Redis 7 fuer HTTP-Sessions (Spring Session, Cookie "SESSION", passwortgeschuetzt)
- Spring Security 7 und Thymeleaf fuer Web UI
- Leaflet + OpenStreetMap fuer die Karte (Vanilla JS, kein Framework)

## Konventionen

- Constructor Injection, kein Lombok, keine `@Setter`/`@Autowired` auf Feldern
- UI-Texte auf Deutsch, Code und Variablen auf Englisch
- Kein `javax.*`; nur `jakarta.*` Namespaces
- Fahrzeug-Status: 1 = Frei ueber Funk, 2 = Frei auf Wache, 3 = Einsatz uebernommen, 4 = Am Einsatzort, 6 = Ausser Dienst
- Status-Farben: gruen (1, 2), rot (3, 4), grau (6)
- Positions-Validierung: lat 53.3-53.8, lng 9.6-10.4 (Hamburg-Gebiet)
- Method Security: `@PreAuthorize` verwenden (nicht `@Secured` - ist ohne `securedEnabled` wirkungslos)
- Location Subtypen: `Station` hat kein `active`-Flag (immer aktiv), nur `Incident` hat Flag

## Current State

### M1: Projektgeruest (complete)
- [x] Boot 4.1.0 + Gradle Kotlin DSL + Java 21 Toolchain
- [x] Flyway Migration `V1`: Tabellen `vehicle` und `app_user`
- [x] JPA-Entitaeten + Spring Data Repositories
- [x] `application.yml` env-driven: DB_URL, DB_USER, DB_PASSWORD, REDIS_HOST, REDIS_PASSWORD
- [x] `docker-compose.yml` fuer lokale Dev-Umgebung: postgres:16 + redis:7
- [x] SecurityConfig: BCrypt + UserDetailsService aus DB
- [x] DataSeeder (`dev`-Profil): admin/admin, viewer/viewer + 4 Hamburger FF-Fahrzeuge

### M2: Auth & REST API (complete)
- [x] Spring Session Redis via Boot-4-Starter (Cookie "SESSION", nicht JSESSIONID)
- [x] Deutsche Login-Seite (`/login`, Thymeleaf)
- [x] REST API `VehicleController` unter `/api/vehicles` (GET fuer alle, Schreibzugriffe ADMIN)
- [x] Positions-Endpoint mit Hamburg-Validierung, Fehler als JSON Problem Detail
- [x] Unit- und `@WebMvcTest`-Abdeckung inkl. Rollen-Regeln

### M3: Kartenansicht (complete)
- [x] `MapController` served Karte auf `/`
- [x] `map.html`: Vollbild-Leaflet, Header mit Nutzername, Logout, Verwaltungs-Link (ADMIN)
- [x] `map.js`: Marker mit Status-Farben, deutsche Popups, 10s-Polling

### M4: Admin UI & Tests (complete)
- [x] `AdminVehicleController`, `AdminUserController` mit deutschen Flash-Messages
- [x] Templates: Fahrzeugliste, Fahrzeug-Bearbeitung (Leaflet Click-to-Set-Position), Nutzerverwaltung
- [x] Eigenes CSS (`/css/admin.css`), kein Framework
- [x] ADMIN-only auf `/admin/**`; VIEWER erhaelt 403
- [x] Selbstschutz Nutzerverwaltung: keine eigene Loeschung/Deaktivierung
- [x] Navigation zwischen allen Seiten verlinkt; toter `/map`-Link auf `/` korrigiert
- [x] 31 Tests gruen

### M5: Statuswechsel via Karte (complete)
- [x] Endpoint PUT `/api/vehicles/{id}/status` - erlaubt fuer VIEWER **und** ADMIN
- [x] SecurityConfig: Status-Route vor der allgemeinen ADMIN-Regel fuer `/api/vehicles/**`
- [x] Status-Buttons im Marker-Popup, sofortiges Marker-Update nach Wechsel
- [x] CSRF: Token und Header-Name aus den Meta-Tags in `map.html` gelesen
  (Header `X-CSRF-TOKEN`, nicht `X-XSRF-TOKEN`)
- [x] Statuslabels in map.js mit korrekten Umlauten (UTF-8)
- [x] Tests: Status-Validierung, VIEWER darf Status setzen aber sonst 403

### M7: Single-Table-Inheritance fuer Location (complete)
- [x] Flyway Migration `V3`: Tabelle `location` mit `location_type`, `name`, lat/lng (Hamburg-Constraints), `active`, `created_at`
- [x] Nullable `location_id` FK auf `vehicle` - NULL bedeutet "unterwegs"
- [x] Abstrakte Entity `Location` mit `@Entity + @Inheritance(SINGLE_TABLE) + @DiscriminatorColumn`
- [x] Subklassen `Station` (STATION, immer aktiv) und `Incident` (INCIDENT, plus Feld `active`)
- [x] Repositories: `LocationRepository` + `IncidentRepository.findByActiveTrue()`
- [x] Tests fuer Entity-Erstellung, Constraint-Validierung, nullable location_id
- [x] H2 f&uuml;r Tests hinzugef&uuml;gt, Migration tested gegen lokaler Postgres (V3 laeuft)

### M6: Deployment in k3s (complete)
- [x] Multi-Stage Dockerfile (`eclipse-temurin:21-jdk` Build, `:21-jre` Runtime, non-root User)
- [x] GitHub-Actions-Pipeline: baut und pusht `ghcr.io/slin86/ff-trainingskarte`
- [x] `application-prod.yml`: Seeder aus, Flyway an
- [x] K8s-Manifeste in `deploy/`: ns, configmap, service, ingress, deployment, infisicalsecret
- [x] Redis-Host `redis.redis.svc.cluster.local`, Redis-Passwort via Infisical
- [x] Postgres `postgres.database.svc.cluster.local:5432`, DB/User/Schema-Grant manuell angelegt
- [x] InfisicalSecret `feuerwehr-db` (Pfad `/feuerwehr`, projectSlug `homelab-ei-fj`, envSlug `prod`)
- [x] ArgoCD-Application im Repo `slin86/argocd`, repoURL per HTTPS (public Repo)
- [x] Ingress-Host `feuerwehr.home.lan`
- [x] Erster Admin manuell per SQL angelegt (BCrypt-Hash, `$2a$`, role ohne ROLE_-Praefix)

## Noch offen

- **Bewusste Nicht-Ziele:** Kein Echtzeit-Push (SSE/WebSockets) - das
  10s-Polling ist eine Architekturentscheidung. Kein MongoDB/Qdrant.
- Station hat kein `active`-Flag, nur Incident.

## Guardrails
- **Englisch** Code and Dokumentation werden in Englisch geschrieben. Kein Deutsch.
- **Scope:** Nur die im aktuellen Prompt genannten Deliverables umsetzen.
  Verbesserungsideen als Vorschlag am Ende der Antwort nennen, NICHT umsetzen.
  Keine neuen Dependencies, Packages oder Architektur-Mechanismen ohne
  expliziten Auftrag.
- **Vorhandener Code ist kein Auftrag:** Halbfertiges auf der Platte, das
  nicht unter "Noch offen" steht, wird gemeldet, nicht fortgefuehrt.
- **Boot 4 Modular Starters:** Autoconfiguration aktiviert sich NUR ueber
  `spring-boot-starter-<tech>`. Rohe Third-Party-Dependencies (z.B.
  `flyway-core`, `spring-session-data-redis`) tun nichts - und zwar lautlos.
- **Abnahme beobachtbar machen:** Jede Aenderung braucht einen pruefbaren
  Check (Logzeile, Cookie-Name, HTTP-Status, sichtbares Browser-Verhalten).
  Ein gruener Build beweist keine Funktionalitaet, Thymeleaf-Fehler sind
  Laufzeitfehler.
- **Tests reparieren, nicht entschaerfen:** Schlaegt ein Test fehl, wird der
  Produktionscode gefixt - Tests werden nie geloescht, deaktiviert oder in
  ihren Assertions aufgeweicht, ausser der Test selbst ist nachweislich falsch.
- **Nach Compaction:** Erledigungs-Aussagen aus eigenen Zusammenfassungen sind
  unzuverlaessig. Dateistand IMMER via `ls`/`git status` verifizieren.
  Ein Meilenstein ist erst complete, wenn jede Datei der Deliverable-Liste
  auf der Platte existiert.
- **Diese Datei:** Status-Haekchen und Current State aktualisieren ist Teil
  jedes Meilensteins. Die Abschnitte "Noch offen", "Bewusste Nicht-Ziele" und
  "Guardrails" werden NICHT vom Agenten veraendert.
- Keine Pfade ausserhalb des Projekt-Roots. Temporaere Dateien nur in
  ./.tmp/ (im Repo, per .gitignore ausgeschlossen), niemals /tmp oder
  absolute Systempfade.
- Nicht commiten, pushen oder deployen ohne explizite Anweisung.
