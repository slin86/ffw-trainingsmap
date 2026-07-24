# FF-Trainingskarte

Trainingstool der Freiwilligen Feuerwehr zur Echtzeit-Darstellung von Fahrzeugpositionen und -status auf einer interaktiven Hamburg-Karte. Dient der Schulung des Einsatzleithelfer-Programms (ELA/EDV) mit realistischen Funksituationen.

## Tech Stack

| Ebene | Technologie |
|---|---|
| Backend | Spring Boot 4.1, Java 21, Gradle (Kotlin DSL) |
| UI | Thymeleaf, Leaflet + OpenStreetMap-Tiles |
| Datenbank | PostgreSQL 16, Flyway Migrationen |
| Sessions/Caching | Redis 7 |
| Authentifizierung | Spring Security 7, Form-Login, Rollen ADMIN / VIEWER |
| Echtzeit-Anbindung | Server-Sent Events (SSE) + Redis Pub/Sub |

## Funktionen

### Karte (`/`)
- Interaktive Leaflet-Karte mit Hamburger Stadtgebiet
- Fahrzeugmarker mit farbcodiertem Status:
  - 🟢 Frei über Funk / Frei auf Wache
  - 🔴 Einsatz übernommen / Am Einsatzort
  - ⚫ Außer Dienst
- Marker-Popup zeigt Rufname, Fahrzeugtyp, Status und letzte Aktualisierung
- Statusänderung per Knopfdruck im Popup (ALLE authenticated Nutzer)
- Automatischer Refresh alle 10 Sekunden + SSE-Echtzeit-Updates

### Verwaltung (`/admin/vehicles`, `/admin/users`)
- **Fahrzeugverwaltung**: CRUD für Fahrzeuge mit Koordinateneingabe über Karte, Pflichtfülle und Duplikatsprüfung (Rufname)
- **Benutzerverwaltung**: Nutzer anlegen, Rollen ändern, aktivieren/deaktivieren
- Selbstschutz: ADMIN kann sich selbst nicht löschen oder deaktivieren
- Nur `ROLE_ADMIN` hat Zugriff auf `/admin/**`; VIEWER erhält 403

### Echtzeit-Architektur
- Event-basiert über Redis Pub/Sub und SSE (Server-Sent Events)
- Fahrzeugänderung → Redis-Publish → SSE-Broadcast an alle verbundenen Karten
- Heartbeat-Mechanismus zur Verbindungserhaltung (60s Timeout)

## Statuscodes des ELA-Systems

| Code | Bedeutung | Farbe auf Karte |
|---|---|---|
| 1 | Frei über Funk | Grün |
| 2 | Frei auf Wache | Grün |
| 3 | Einsatz übernommen | Rot |
| 4 | Am Einsatzort | Rot |
| 6 | Außer Dienst | Grau |

## Konfiguration

Alle Parameter per Umgebungsvariablen konfigurierbar (Defaults für lokale Entwicklung):

| Variable | Default | Beschreibung |
|---|---|---|
| `DB_URL` | `jdbc:postgresql://localhost:5432/ff_trainingskarte` | JDBC-Verbindung zur Datenbank |
| `DB_USER` | `postgres` | Datenbank-Benutzer |
| `DB_PASSWORD` | `password` | Datenbank-Passwort |
| `REDIS_HOST` | `localhost` | Redis-Host (Sessions + Pub/Sub) |
| `REDIS_PORT` | `6379` | Redis-Port |
| `SERVER_PORT` | `8080` | HTTP-Servlet-Port |

## Lokale Entwicklung

```bash
# 1. Infrastrukturebene starten (Postgres + Redis)
docker compose up -d

# 2. App im Dev-Modus mit Seed-Daten
./gradlew bootRun --args='--spring.profiles.active=dev'
```

App: http://localhost:8080
Admin-Zugang: `admin` / `admin`

Seedet automatisch einen ADMIN-Nutzer und vier Hamburger FF-Fahrzeuge:
- HLF 20/1 (Wandsbek)
- DLK 12/1 (Altona)
- TLF 3/1 (Hamburg-Mitte)
- MTW 1/1 (Harburg)

### Tests

```bash
./gradlew test allTests
```

31 Unit- und Integrationstests (`@WebMvcTest` für Controller, selbstschutz-Logik, CRUD-Abdeckung).

## API-Endpunkte

| Methode | Pfad | Zugriff | Beschreibung |
|---|---|---|---|
| GET | `/api/vehicles` | Authenticated | Alle Fahrzeuge zurückgeben (JSON) |
| POST | `/api/vehicles` | ADMIN | Neues Fahrzeug anlegen |
| PUT | `/api/vehicles/{id}` | ADMIN | Fahrzeug-Basisdaten aktualisieren |
| PUT | `/api/vehicles/{id}/position` | ADMIN | Position ändern (Koordinatenvalidierung: Hamburg-Radius) |
| PUT | `/api/vehicles/{id}/status` | Authenticated | Status wechseln (Werte 1,2,3,4,6 → triggert SSE-Broadcast) |
| DELETE | `/api/vehicles/{id}` | ADMIN | Fahrzeug löschen → triggert SSE-Löschbenachrichtigung |

HTTP-Request-Beispiele in `http/*.http`.

## Docker & Deployment

### Image bauen

```bash
docker buildx build --platform linux/amd64 -t ghcr.io/slin86/ff-trainingskarte:v0.1.0 --push .
```

Zweistufiges Image: Build mit Eclipse Temurin JDK 21, Laufzeit nur JRE (non-root User `appuser`).

### k3s-Deployment (Mit ArgoCD)

Kubernetes-Manifeste in `deploy/`:

| Datei | Zweck |
|---|---|
| `ns.yaml` | Namespace `feuerwehr` |
| `configmap.yaml` | Spring-Profil `prod`, Redis-Host, SSE-Konfiguration |
| `deployment.yaml` | 2 Replikas, liveness/readiness über Actuator `/actuator/health`, Ressourcenlimits (256m/512m Memory) |
| `service.yaml` | ClusterIP auf Port 8080 |
| `ingress.yaml` | Traefik-Ingress für Host `feuerwehr.home.lan` |

Secrets (`InfisicalSecret`CRD), DB-Passwort, Spring Redis-Konfiguration und CORS-Allow-Origin. **Nicht im Repository**.

Deploy via ArgoCD:
1. Manifeste ins ArgoCD-Repo unter `apps/feuerwehr/` kopieren
2. ArgoCD Application-Ressource erstellen (`repoURL`, `targetRevision`, `path: apps/feuerwehr`)
3. Sync → Pods provisionieren, Ingress aktiv

Details: [deploy/README.md](deploy/README.md)

## Repository-Struktur

```
ff-trainingskarte/
├── deploy/               # Kubernetes Manifeste
├── docker-compose.yml    # Locale Entwicklungsumgebung (Postgres + Redis)
├── Dockerfile            # Production-docker-image
├── http/                 # HTTP-Request-Vorlagen (.http)
├── src/main/java/de/ffw/trainingskarte/
│   ├── config/           # SecurityConfig
│   ├── controller/       # Web + Admin + REST API-Controller
│   │   └── dto/          # Request-Records (Vehicle, Position, StatusChange)
│   ├── entity/           # JPA @Entity-Klassen (Vehicle, AppUser)
│   ├── repository/       # Spring Data Repositories
│   ├── seeder/           # DevSeed (DataSeeder.java)
│   └── sse/              # SSE-Registry + RedisPub/Sub Event-Verwaltung
├── src/main/resources/
│   ├── db/migration/     # Flyway-Skripte
│   ├── static/js/        # Frontend JavaScript (Leaflet-Karte)
│   ├── templates/        # Thymeleaf-Vorlagen (map.html, login.html)
│   └── application.yml   # Defaults (alle über Env-Overrides konfigurierbar)
├── src/test/java/        # Unit + @WebMvcTest-Tests
```

## Architektur-Entscheidungen

- **10s-Polling als Fallback**: Die Karte friert zuverlässig bei SSE-Verbindungsabbrüchen. Polling sichert die Darstellung auch ohne persistente Verbindungen.
- **Redis Session-Store**: Ermöglicht horizontales Scaling (multi-replica) mit geteilter Session-Speicherung.
- **Kein externes CSS-Framework**: Eigenes Minimal-CSS für reduzierte Abhängigkeiten und schnelles Rendering auf Tablet/Kommunikationsgeräten in der Wache.
- **Constructor Injection durchgängig**: kein Lombok, keine `@Autowired`-Feldinjektion → explizite Abhängigkeiten, einfache Testbarkeit.

## Konventionen

- Constructor-Injektion, kein Lombok, keine Feld-Injektion
- UI-Texte auf Deutsch, Code in englisch
- Keine globalen Imports; volle Paketqualifikation wo nötig
- Nur Jakarta-Namensräume (`jakarta.*`), kein `javax.*`
- Jede Änderung braucht einen beobachtbaren Abnahme-Check (Logzeile, Cookie-Name, HTTP-Status) – "kompiliert" beweist nichts
