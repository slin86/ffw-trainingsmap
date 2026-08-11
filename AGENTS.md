# ff-trainingskarte

Training-Tool fuer die Freiwillige Feuerwehr zur Darstellung von Fahrzeugen auf einer Hamburg-Karte.

## Stack

- Java 21, Gradle (Kotlin DSL)
- **Spring Boot 4.1**, Jackson 3 mit `tools.jackson.*` Packages
- PostgreSQL 16 + Flyway Migrationen
- Redis 7 fuer HTTP-Sessions (Spring Session, Cookie "SESSION", passwortgeschuetzt)
- Spring Security 7 und Thymeleaf fuer Web UI
- Leaflet + OpenStreetMap fuer die Karte (Vanilla JS, kein Framework)
- **Nominatim API** fuer Adresssuche/Geocoding

## Konventionen

- Constructor Injection, kein Lombok, keine `@Setter`/`@Autowired` auf Feldern
- UI-Texte auf Deutsch, Code und Variablen auf Englisch
- Kein `javax.*`; nur `jakarta.*` Namespaces
- Fahrzeug-Status: 1 = Frei ueber Funk, 2 = Frei auf Wache, 3 = Einsatz uebernommen, 4 = Am Einsatzort, 6 = Ausser
  Dienst
- Status-Farben: gruen (1, 2), rot (3, 4), grau (6)
- Positions-Validierung: lat 53.3-53.8, lng 9.6-10.4 (Hamburg-Gebiet)
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

### M3: Karteansicht (complete)

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

- [x] Flyway Migration `V3`: Tabelle `location` mit `location_type`, `name`, lat/lng (Hamburg-Constraints), `active`,
  `created_at`
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

### M8: Karte mit Location-Markern (complete)

- [x] GET /api/locations beim Laden der Karte fuer Stationen + aktive Incidents
- [x] Location Marker (blau fuer Station, gelb fuer active Incident)
- [x] Fahrzeuge mit location_id: Marker an Location-Koordinaten
- [x] Fahrzeuge ohne location_id ("unterwegs"): Marker an eigener lat/lng
- [x] Popup pro Location zeigt Name + Typ + Listen der zugewiesenen Fahrzeuge

### M9: Admin UI Sidebar (complete)

- [x] Gemeinsame Sidebar fuer Admin-Bereich mit Nav (Fahrzeuge, Nutzer, Orte)
- [x] Aktueller Bereich optisch markiert
- [x] AdminLocationController: CRUD fuer Stations + Incidents
- [x] Toggle active Status nur fuer Incidents

### M10: Drag-and-Drop Positionierung (complete)

- [x] PATCH /api/vehicles/{id}/position (ADMIN-only, Bounds validieren)
- [x] Fahrzeug-Marker draggable nur als ADMIN
- [x] dragend: PATCH -> neue Position speichern
- [x] Fehler fallen Marker auf alte Position zurueck

### M11: Location Vehicle Zuweisung (complete)

- [x] Dropdown in Station/Incident-Popup fuer ADMIN sichtbar
- [x] Auswahl alle nicht-zugeordneten Fahrzeuge
- [x] Option "Unterwegs" (null) entfernt Zuordnung direkt
- [x] onChange: PATCH /api/vehicles/{id}/location

### M12: LocationController Refactoring (complete)

- [x] LocationController deleted, replaced by separate StationController + IncidentController
- [x] GET /api/stations - liefert alle Stationen mit Fahrzeugen
- [x] GET /api/incidents?all=true - liefert alle Incidents, ?all=false nur aktive
- [x] map.js: API-Calls angepasst auf /api/stations und /api/incidents

### M13: AdminLocationController Refactoring (complete)

- [x] AdminLocationController split into separate AdminStationController + AdminIncidentController
- [x] Separate pages: `/admin/stations` and `/admin/incidents`
- [x] Templates: stations.html, incidents.html with sidebar navigation
- [x] Fix type casting issue for LocationRepository.findById()

### M14: Station/Incident Controller Bugfixes und Tests (complete)

- [x] StationController: PUT /api/stations/{id} hinzugefuegt (ADMIN-only, 404 bei unbekannter ID)
- [x] StationController.findAll() umbenannt (keine Fetch-Joins benoetigt)
- [x] IncidentController/StationController: einheitliche Fehlermeldungen mit `ResponseStatusException`
- [x] AdminStationController/AdminIncidentController: 404 bei unknown IDs statt IllegalArgumentException
- [x] StationControllerTest: GET, POST (ADMIN-only), PUT (ADMIN-only), DELETE (ADMIN-only) + 404-Faelle
- [x] IncidentControllerTest: GET (?all=true/false), POST/PUT/DELETE (ADMIN-only), PATCH /active (ADMIN-only)
- [x] AdminStationControllerTest: GET, POST (create+redirect), POST /{id}/delete (inkl. 404-Fall)
- [x] AdminIncidentControllerTest: GET (?all=true), POST (create), POST /{id}/toggle, POST /{id}/delete
- [x] Alle 4 neue Tests gruen mit `./gradlew test`

### M15: Popup Description & Location Cleanup (complete)

- [x] Description-Feld im Karten-Popup anzeigen (leer wenn null/leer, kein Leerzeichen)
- [x] Active-Flag von Incidents bei Polling beruecksichtigt (`/api/incidents?all=false`)
- [x] Marker/Sidebar aktualisieren ohne Reload bei Deaktivierung
- [x] GeocodeController: Nominatim-API Proxy Endpoint `/api/geocode` mit Hamburg-Bounds (viewbox/bounded)
- [x] Adress-Suche UI in station-form.html und incident-form.html (Karte + Adresse)
- [x] GeocodeControllerTest: Unit-Tests fuer Parsing
- [x] 10s-Polling loescht verwaiste Marker/Sidebar-Eintraege

### M16: Check-In-Feature (complete)

- [x] Flyway Migration `V6__vehicle_checkin.sql`: Tabelle mit unique username constraint
- [x] Entity `VehicleCheckin` mit @ManyToOne Vehicle, username, checkedInAt
- [x] Repository `VehicleCheckinRepository` mit findByUsername + findByVehicleId
- [x] Service `CheckInService` mit isCheckedIn(Long vehicleId, String username)
- [x] Controller `CheckInController`:
  - POST /api/vehicles/{id}/checkin (VIEWER+ADMIN, löscht alter Check-in)
  - POST /api/vehicles/{id}/checkout (VIEWER+ADMIN, idempotent)
  - GET /api/checkin/me (liefert aktuelles vehicleId oder 204 No Content)
- [x] VehicleController.patchPosition: @PreAuthorize mit checkInService
- [x] CheckInControllerTest: alle Endpoints abgedeckt

### M17: Karte Check-In UI & Geolocation (complete)

- [x] `map.js`: Status im Fahrzeug-Popup anzeigen
- [x] Button "Einchecken"/"Auschecken" basierend auf Check-in-Status
- [x] Warnung wenn in Anderem Fahrzeug eingecheckt
- [x] `getMyCheckin()`: beim Laden der Seite Check-in laden
- [x] `startLocationWatchForVehicle()`: watchPosition starten mit 10s Throttle
- [x] `updateVehiclePositionFromGeolocation()`:Positionsaktualisierung an Server senden
- [x] `stopLocationWatch()`: beim Auschecken Watch stoppen

### M18: Versionsverwaltung (complete)

- [x] `version.txt` im Repo-Root mit Inhalt `0.4.0`
- [x] Gradle Task `processResources` und `bootJar`: version.txt in Jar-Klassenpfad kopieren
- [x] `VersionController` REST-Endpoint `/api/version` (public, keine Auth-Pflicht)
- [x] Version wird beim Serverstart einmalig gelesen via `getResourceAsStream("/version.txt")`
- [x] map.html: `<span id="app-version"></span>` im Header hinzugefügt
- [x] map.js: `fetch('/api/version')` beim Laden der Seite, Anzeige als "v0.4.0"
- [x] CSS für version anpassen (kleine Schriftgroesse, Farbe #ffcdd2 wie Header-Links)
- [x] GitHub Actions workflow build.yml erweitert:
  - `contents: write` statt `read`
  - Neuer Schritt "Read version" zur Extraktion der Version
  - Version als Tag zu Docker-Publish hinzugefügt
  - Neuer Schritt "Update deployment.yaml" zum automatischen Commit per Bot

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
- Für Code-Änderungen das Edit-Tool nutzen, nicht sed/awk via Shell.
  Shell-Textmanipulation ist auf macOS (BSD-Tools) fehleranfällig.
- Ausschliesslich Pfade relativ zum Projekt-Root. Keine absoluten
  /Users/...-Pfade konstruieren.
- Java: Spotless oder Checkstyle als Gradle-Plugin, das der Agent bei jedem Build "for free" mitgeprüft bekommt (in
  ./gradlew build eingehängt) – zwingt zumindest Formatierung/einfache Smells, ohne dass du das manuell reviewen musst.
- JS: ESLint mit ein paar strengen Regeln (z. B. no-duplicate-case, Komplexitätsgrenzen pro Funktion) – bei map.js mit
  seinen sehr langen Funktionen (updateMap, updateSidebar) würde das schon beim Schreiben auffallen.

(End of file - total 203 lines)
