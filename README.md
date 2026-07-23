# Feuerwehr-Trainingskarte – Projektplan & Agenten-Prompts

Webanwendung für die Freiwillige Feuerwehr: Fahrzeuge auf einer OSM-Karte
(Hamburg), Positionen flexibel setzbar, Admin-Oberfläche für Fahrzeuge,
Benutzer und Positionen. Gebaut vom lokalen Coding-Agenten (Qwen3.6-27B Q4).

---

## 1. Architektur-Entscheidung

**Stack (bewusst schlank – lokale Modelle liefern bei einfachen Stacks
deutlich bessere Ergebnisse):**

| Komponente | Wahl | Begründung |
|---|---|---|
| Backend | Spring Boot 4.1, Java 21, Gradle | Dein Heimspiel; Boot 3.5 ist seit 30.06.2026 OSS-EOL – Neustart auf einer EOL-Linie wäre unsauber. Achtung: Qwen kennt v.a. Boot 2/3, daher Leitplanken im M1-Prompt |
| Datenbank | **PostgreSQL** (vorhanden) | Fahrzeuge, User, Positionen – klassisch relational |
| Cache/Live | **Redis** (vorhanden) | Sessions; ab M5 Pub/Sub für Live-Positionsupdates |
| Karte | **Leaflet + OpenStreetMap-Tiles** | Kostenlos, kein API-Key. Tile-Server: tile.openstreetmap.org (für Trainingsbetrieb mit wenigen Nutzern von der Usage Policy gedeckt) |
| Frontend | Server-side (Thymeleaf) + Vanilla JS für die Karte | Kein Build-Toolchain-Overhead, weniger Fehlerquellen für den Agenten |
| Auth | Spring Security, Session-basiert, Rollen ADMIN / VIEWER | Redis als Session-Store |
| Deployment | Docker-Image → ghcr.io → ArgoCD (`apps/feuerwehr/`) | Dein Standard-Muster |

**Bewusst NICHT in v1:** MongoDB (kein zweites Datenmodell ohne Grund) und
Qdrant. Qdrant-Ausbaustufe für später: Übungs-/Einsatzberichte als
durchsuchbare Wissensbasis (RAG über bge-m3 – Pipeline existiert ja schon).

**Keine Service-Dopplung:** Die App nutzt im Cluster die vorhandenen
Postgres/Redis-Instanzen (eigene DB `feuerwehr` + eigener User, Redis mit
Key-Prefix bzw. eigener DB-Nummer). Das docker-compose in M1 ist NUR die
Wegwerf-Dev-Umgebung auf dem Mac, damit der Agent Tests lokal ausführen
kann – es landet nie im Cluster.

**Datenmodell v1:**
- `vehicle`: id, callsign (Funkrufname, z.B. "HLF 20-1"), type (HLF/DLK/TLF/MTW/RW/ELW),
  status (Anlehnung FMS: 1=frei Funk, 2=frei Wache, 3=Einsatz übernommen, 4=am Einsatzort, 6=außer Dienst),
  lat, lng, updated_at
- `app_user`: id, username, password_hash (BCrypt), role, enabled

---

## 2. Vorgehensweise mit dem Agenten

**Regeln (wichtiger als der Prompt selbst):**

1. **Ein Meilenstein = eine frische OpenCode-Session.** Das Q4 hat 16K
   Kontext – eine Endlos-Session kippt irgendwann.
2. **Nach jedem grünen Meilenstein: `git commit`.** Sauberer Rollback-Punkt.
3. Agent Tests ausführen lassen (`./gradlew test`), Fehler zurückspielen,
   erst weiter, wenn grün.
4. Vor jeder Session den PC wecken: `curl -X POST http://llm.home.lan/wake`
5. Modellwahl: Q4 wie gewünscht (`/models` in OpenCode). Wird eine Session
   zäh, ist Q3 für mechanische Aufgaben (CRUD, Templates) völlig okay –
   Q4 lohnt vor allem bei M2 (Security) und M5 (Nebenläufigkeit).
6. Start jeder Session: `AGENTS.md` existiert ab M1 im Repo – der Agent
   liest sie automatisch und kennt damit Konventionen + Stand.

**Setup:**

```bash
mkdir -p ~/dev/ff-trainingskarte && cd ~/dev/ff-trainingskarte
git init
opencode   # Modell: qwen3.6-coder:q4
```

---

## 3. Die Meilenstein-Prompts

Prompts auf Englisch (Code-Qualität lokaler Modelle ist damit messbar
besser), UI-Texte auf Deutsch. Copy-paste pro Session.

### M1 – Skeleton & Datenmodell

```
Create a Spring Boot 4 project "ff-trainingskarte" with Gradle (Kotlin DSL),
Java 21. Purpose: a training tool for a volunteer fire department showing
fire trucks on a map of Hamburg.

IMPORTANT - Spring Boot 4 guardrails (do NOT fall back to Boot 3 idioms):
- Use Spring Boot 4.1.x in the Gradle plugin block
- JSON is Jackson 3: packages are tools.jackson.*, NOT com.fasterxml.*
  (only jackson-annotations keeps the old package)
- Starters are modular; if a class is missing, check whether it moved to
  its own starter instead of downgrading
- jakarta.* namespaces everywhere, never javax.*

Requirements:
- Dependencies: web, thymeleaf, data-jpa, security, validation, flyway,
  postgresql driver, spring-session-data-redis, actuator
- Flyway migration V1: table "vehicle" (id bigserial PK, callsign varchar
  unique not null, type varchar not null, status int not null default 2,
  lat double precision not null, lng double precision not null,
  updated_at timestamptz not null default now()) and table "app_user"
  (id bigserial PK, username varchar unique not null, password_hash varchar
  not null, role varchar not null, enabled boolean not null default true)
- JPA entities + Spring Data repositories for both tables
- application.yml: config via environment variables (DB_URL, DB_USER,
  DB_PASSWORD, REDIS_HOST) with sensible localhost defaults
- docker-compose.yml for local dev: postgres:16 and redis:7
- A DataSeeder (CommandLineRunner, profile "dev") creating admin/admin
  (role ADMIN, BCrypt) and 4 vehicles at real Hamburg fire station
  coordinates, e.g. FF Barmbek (53.587, 10.044), FF Eppendorf
  (53.594, 9.990), FF Altona (53.552, 9.935), FF Harburg (53.460, 9.983)
- Write an AGENTS.md documenting: project purpose, stack (explicitly:
  "Spring Boot 4.1, Jackson 3 with tools.jackson packages"), conventions
  (constructor injection, no Lombok, German UI strings, English code),
  and a "Current state" section listing completed milestones. Keep it
  under 60 lines.
- Everything must compile: ./gradlew build

Show me a short plan first, then implement. Run the build and fix errors
until it passes.
```

### M2 – Auth & REST-API

```
Read AGENTS.md first. Extend the project:

- Spring Security: session-based login (sessions stored in Redis),
  login page at /login (Thymeleaf, German labels: "Anmelden",
  "Benutzername", "Passwort"), roles ADMIN and VIEWER
- REST API under /api:
  GET /api/vehicles (authenticated) -> list all vehicles as JSON
  POST /api/vehicles (ADMIN) -> create vehicle, validate callsign unique
  PUT /api/vehicles/{id} (ADMIN) -> update type/status/callsign
  PUT /api/vehicles/{id}/position (ADMIN) -> body {lat, lng}, validate
    lat 53.3-53.8 and lng 9.6-10.4 (Hamburg area), update updated_at
  DELETE /api/vehicles/{id} (ADMIN)
- Validation errors -> HTTP 400 with a JSON problem detail
- CSRF: enabled for forms, API uses same-origin session auth
- Unit tests for the position validation, @WebMvcTest for the vehicle
  API (auth rules: VIEWER gets 403 on writes)
- Update the "Current state" section in AGENTS.md

Run ./gradlew test and fix until green.
```

### M3 – Kartenansicht

```
Read AGENTS.md first. Build the map view:

- Route / (authenticated): Thymeleaf page with a full-screen Leaflet map
  (Leaflet via CDN), centered on Hamburg (53.55, 9.99), zoom 12,
  OpenStreetMap tiles with proper attribution
- Fetch /api/vehicles on load, render one marker per vehicle
- Marker popup: callsign, type, status as German text (1 "Frei ueber Funk",
  2 "Frei auf Wache", 3 "Einsatz uebernommen", 4 "Am Einsatzort",
  6 "Ausser Dienst") and updated_at formatted as dd.MM.yyyy HH:mm
- Marker color by status: green (1,2), red (3,4), gray (6) - use simple
  colored circle markers (L.circleMarker), no custom icon assets
- Poll /api/vehicles every 10 seconds and update markers in place
  (no full page reload, keep open popups working)
- Header bar: title "FF Trainingskarte", logged-in username, logout button,
  link "Verwaltung" visible only for ADMIN
- Keep all JS in src/main/resources/static/js/map.js, vanilla JS, no
  framework, no build step

Verify with ./gradlew build. Update AGENTS.md.
```

### M4 – Admin-Oberfläche

```
Read AGENTS.md first. Build the admin UI under /admin (ADMIN only),
server-rendered Thymeleaf, German labels, minimal clean CSS (one
stylesheet, no framework):

- /admin/vehicles: table of all vehicles (Funkrufname, Typ, Status,
  Position, Aktualisiert), create form (type as dropdown: HLF, DLK, TLF,
  MTW, RW, ELW; status dropdown with German labels), edit, delete with
  confirmation
- Vehicle position editing: on the edit page, a small Leaflet map -
  clicking the map sets lat/lng into the form fields, current position
  shown as marker
- /admin/users: list, create (username, password, role), enable/disable
  toggle, delete. Never show password hashes. Prevent deleting or
  disabling your own account.
- Flash messages in German after each action ("Fahrzeug angelegt", ...)
- @WebMvcTest covering: VIEWER gets 403 on /admin/**, self-deletion is
  rejected

Run ./gradlew test until green. Update AGENTS.md.
```

### M5 – Live-Updates (Polling raus, SSE rein)

```
Read AGENTS.md first. Replace the 10s polling with near-realtime updates:

- Server-Sent Events endpoint GET /api/vehicles/stream (authenticated):
  emits a vehicle JSON event whenever a vehicle changes
- Use Redis pub/sub as the event bus: every vehicle create/update/delete
  publishes to channel "vehicle-events"; the SSE endpoint subscribes and
  forwards. This keeps it correct even with multiple app replicas.
- Frontend: EventSource with automatic reconnect; fall back to 30s
  polling if SSE is unavailable
- Heartbeat comment every 25s so proxies do not kill the connection
- Integration test with @SpringBootTest + testcontainers for Redis
  (skip if testcontainers is too heavy - then test the publisher and
  the SSE emitter registry separately with mocks)

Run ./gradlew test until green. Update AGENTS.md.
```

### M6 – Containerisierung & Deployment

```
Read AGENTS.md first. Prepare deployment:

- Multi-stage Dockerfile: gradle build stage, then eclipse-temurin:21-jre,
  non-root user, EXPOSE 8080
- Kubernetes manifests in deploy/: namespace "feuerwehr", Deployment
  (env from Secret "feuerwehr-db" and ConfigMap, readiness/liveness via
  actuator health), Service port 8080, Ingress class traefik host
  "feuerwehr.home.lan" with backend service port 8080 (the port number
  must match the Service port exactly)
- The Secret itself is NOT created here - add a deploy/README.md note
  that secrets come from Infisical (InfisicalSecret CRD, project path
  /feuerwehr) and listing the required keys
- Spring profile "prod": seeder disabled, flyway enabled
- Project README.md (German): local dev (docker-compose + bootRun),
  build & push command (docker buildx build --platform linux/amd64 -t
  ghcr.io/slin86/ff-trainingskarte:v0.1.0 --push .), deployment via
  ArgoCD repo

Verify ./gradlew build. Update AGENTS.md: mark project v1 complete.
```

**Nach M6 (deine Handgriffe, nicht der Agent):** Manifeste ins ArgoCD-Repo
unter `apps/feuerwehr/`, Application-Manifest analog zu OpenHands anlegen,
Infisical-Projektpfad `/feuerwehr` mit DB-Credentials füllen, Postgres-DB
`feuerwehr` + User anlegen, AdGuard-Rewrite prüfen (Wildcard `*.home.lan`
deckt `feuerwehr.home.lan` vermutlich schon ab).

---

## 4. Zugriff von außen – Optionen & Empfehlung

Bisher ist alles intern, und das war eine bewusste Entscheidung. Für die
Kameraden gibt es drei realistische Wege, sortiert nach Aufwand/Risiko:

### Option A: WireGuard (vorhanden!) – Empfehlung für den Start
Die FritzBox macht schon WireGuard. Pro Nutzer ein Profil erzeugen
(FritzBox-UI, QR-Code fürs Handy), fertig. AdGuard als DNS im
WireGuard-Profil eintragen, dann funktioniert `feuerwehr.home.lan` auch
unterwegs.

- ✅ Null neue Angriffsfläche, kein offener Port, keine Cloud, in 30 min live
- ✅ Passt zu deinem Internal-only-Prinzip
- ❌ Skaliert organisatorisch bis ~10–20 Nutzer, danach nervt die
  Profilverwaltung
- ❌ "Mal eben Link teilen" geht nicht

### Option B: Cloudflare Tunnel + Cloudflare Access
`cloudflared` als Deployment im Cluster baut einen Outbound-Tunnel zu
Cloudflare auf – kein Port-Forwarding, deine IP bleibt privat. Davor
Cloudflare Access (kostenlos bis 50 Nutzer): Login per E-Mail-Code,
nur freigeschaltete Adressen kommen durch. Braucht eine eigene Domain
(~5 €/Jahr).

- ✅ Echte URL für alle, kein Client nötig, Auth VOR deinem Cluster
- ✅ Kein offener Port auf der FritzBox
- ❌ Cloud-Abhängigkeit (bricht dein Local-first-Prinzip – aber nur für
  diese eine App, nicht fürs Homelab)
- ❌ Cloudflare terminiert TLS, sieht also den Traffic dieser App

### Option C: Port-Forwarding 443 → Traefik + Let's Encrypt + Auth-Middleware
Der klassische Weg: DynDNS/MyFritz, Freigabe auf Traefik, Zertifikate via
cert-manager, davor zwingend eine Auth-Schicht (Authelia/Basic-Auth) und
sauberes Host-Routing, damit NUR diese App exponiert ist.

- ✅ Komplett selbst gehostet
- ❌ Größte Angriffsfläche: dein Heimnetz hängt hinter einem offenen Port,
  du bist allein für Patching verantwortlich
- ❌ Scheitert an DS-Lite/CGNAT: **vorab prüfen, ob dein Anschluss eine
  echte öffentliche IPv4 hat** (FritzBox-Übersicht → Internet)

**Empfehlung:** Trainingsphase mit **Option A** starten – die Hürde "App
existiert und wird genutzt" ist wichtiger als die perfekte Exponierung.
Wenn die Wehr das Tool annimmt und mehr Leute drauf sollen: **Option B**
nachrüsten (ein Deployment + DNS-Eintrag, die App selbst ändert sich
nicht). Option C nur, wenn Cloud kategorisch ausscheidet – dann aber mit
Authelia und der Bereitschaft, das dauerhaft zu pflegen.

---

## 5. Ausbaustufen (Backlog für später)

- Fahrzeuge per Drag&Drop auf der Hauptkarte verschieben (ADMIN)
- Übungsszenarien: Einsatzort als Marker + Alarmierung ausgewählter Fahrzeuge
- Positions-Historie (einfaches Event-Log in Postgres) + Replay einer Übung
- Qdrant/RAG: Übungsberichte hochladen, durchsuchbar machen (bge-m3-Pipeline
  vom rag-indexer wiederverwenden)
- Mobile-Optimierung der Kartenansicht (Kameraden nutzen Handys)
